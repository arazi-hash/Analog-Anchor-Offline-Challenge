package com.analoganchor.offlinechallenge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.analoganchor.offlinechallenge.MainActivity
import com.analoganchor.offlinechallenge.R
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.ByteBuffer

class MyVpnService : VpnService() {

    companion object {
        private const val TAG = "OfflineChallenge"
        private const val CHANNEL_ID = "offline_challenge_channel"
        private const val COMPLETION_CHANNEL_ID = "challenge_completion_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.analoganchor.offlinechallenge.START_VPN"
        const val ACTION_STOP = "com.analoganchor.offlinechallenge.STOP_VPN"

        var isRunning = false
            private set
    }

    private var localTunnel: ParcelFileDescriptor? = null
    private var packetLoop: Job? = null
    private var notificationUpdater: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
        }

        val prefs = ChallengePreferences(this)

        // CRITICAL GUARD: If no challenge is active,
        // do NOT start VPN tunnel, do NOT blackhole traffic, and exit cleanly immediately.
        if (!prefs.isActive) {
            Log.d(TAG, "onStartCommand called with inactive challenge. Aborting VPN.")
            VpnGuardWorker.cancel(this)
            NetworkGuard.unregister(this)

            // Temporary notification to fulfill startForegroundService contract if invoked by system, then immediately remove
            val tempNotification = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getLocalizedContext().getString(R.string.app_name))
                .build()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, tempNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, tempNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
                } else {
                    startForeground(NOTIFICATION_ID, tempNotification)
                }
            } catch (e: Exception) {
                // Ignore foreground start exception during quick abort
            }
            stopVpn()
            return START_NOT_STICKY
        }

        // Start as foreground service immediately with actual active progress
        val initialProgress = prefs.getProgress()
        val initialRemaining = prefs.getRemainingMillis()
        val notification = buildNotification(initialProgress, initialRemaining)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "startForeground with type failed: ${e.message}")
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback startForeground failed: ${ex.message}")
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Build VPN tunnel that blackholes ALL traffic (IPv4 & IPv6)
        val builder = Builder()
            .addAddress("10.1.1.1", 24)
            .addRoute("0.0.0.0", 0)       // Capture all IPv4 traffic
            .addAddress("fd00::1", 128)
            .addRoute("::", 0)            // Capture all IPv6 traffic
            .addDnsServer("10.1.1.1")     // Sinkhole IPv4 DNS
            .addDnsServer("fd00::1")      // Sinkhole IPv6 DNS
            .setSession(getString(R.string.app_name))
            .setBlocking(false)

        try {
            localTunnel = builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN tunnel: ${e.message}")
            stopVpn()
            return START_NOT_STICKY
        }

        if (localTunnel == null) {
            Log.e(TAG, "Failed to establish VPN tunnel (null descriptor)")
            stopVpn()
            return START_NOT_STICKY
        }

        // Consume packets in a loop (blackhole — read and discard)
        packetLoop = serviceScope.launch {
            val input = FileInputStream(localTunnel!!.fileDescriptor)
            val buffer = ByteBuffer.allocate(1024)
            try {
                while (true) {
                    withContext(Dispatchers.IO) {
                        buffer.clear()
                        input.channel.read(buffer)
                    }
                    delay(100) // Throttle to save battery
                }
            } catch (e: Exception) {
                Log.d(TAG, "Packet loop ended: ${e.message}")
            }
        }

        // Update notification every 1 second with progress
        notificationUpdater = serviceScope.launch {
            val updaterPrefs = ChallengePreferences(this@MyVpnService)
            while (true) {
                delay(1_000)
                if (!updaterPrefs.isActive) {
                    Log.d(TAG, "Challenge ended. Stopping VPN and tearing down notifications.")
                    try {
                        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                        val adminComponent = DeviceAdminReceiver.getComponentName(this@MyVpnService)
                        if (dpm?.isAdminActive(adminComponent) == true) {
                            dpm.removeActiveAdmin(adminComponent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to remove admin on completion: ${e.message}")
                    }
                    // Disarm Layer 2 & 3 guards — challenge is over
                    VpnGuardWorker.cancel(this@MyVpnService)
                    NetworkGuard.unregister(this@MyVpnService)
                    stopVpn()
                    com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this@MyVpnService)
                    break
                }

                if (updaterPrefs.isExpired()) {
                    if (!updaterPrefs.isHoldingOffline) {
                        updaterPrefs.isHoldingOffline = true
                        showCompletionNotification()
                    }
                    // Keep VPN active and blackholing packets. Update ongoing notification to indicate holding offline.
                    updateHoldingNotification()
                    com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this@MyVpnService)
                    continue
                }
                val progress = updaterPrefs.getProgress()
                val remaining = updaterPrefs.getRemainingMillis()
                val elapsed = System.currentTimeMillis() - updaterPrefs.startTimeMillis

                // Half-minute (30s) check: remind user to turn off Wi-Fi/Data only if they haven't turned them off
                if (elapsed >= 30 * 1000L && !updaterPrefs.isBatteryReminderSent) {
                    if (com.analoganchor.offlinechallenge.util.NetworkHelper.isWifiOrDataActive(this@MyVpnService)) {
                        updaterPrefs.isBatteryReminderSent = true
                        showBatteryReminderNotification()
                    }
                }

                if (progress >= 0.5f && !updaterPrefs.isHalfwayNotified) {
                    updaterPrefs.isHalfwayNotified = true
                    showHalfwayNotification()
                }
                updateNotification(progress, remaining)
                com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(this@MyVpnService)
            }
        }

        isRunning = true
        Log.d(TAG, "VPN started — all internet traffic blackholed")
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN revoked by system or another VPN")
        val prefs = ChallengePreferences(this)
        if (!prefs.isActive || prefs.isExpired()) {
            stopVpn()
        }
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopVpn() {
        packetLoop?.cancel()
        packetLoop = null
        notificationUpdater?.cancel()
        notificationUpdater = null
        try {
            localTunnel?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close localTunnel: ${e.message}")
        }
        localTunnel = null
        isRunning = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "stopForeground error: ${e.message}")
        }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "NotificationManager cancel error: ${e.message}")
        }
        stopSelf()
        Log.d(TAG, "VPN stopped and notification dismissed")
    }

    // ── Notification ──────────────────────────────────────────────────

    private fun getLocalizedContext(): android.content.Context {
        val prefs = ChallengePreferences(this)
        val locale = java.util.Locale(prefs.language)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        return createConfigurationContext(config)
    }

    private fun createNotificationChannel() {
        val locContext = getLocalizedContext()
        val manager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            locContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW // Low = no sound, but persistent
        ).apply {
            description = locContext.getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val compChannelName = if (ChallengePreferences(this).language == "ar") "إكمال التحدي" else "Challenge Completion"
        val completionChannel = NotificationChannel(
            COMPLETION_CHANNEL_ID,
            compChannelName,
            NotificationManager.IMPORTANCE_HIGH // High = makes sound, vibrates, pops up on screen
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 400, 1000, 400, 1500, 500, 1500, 500, 2000)
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            val audioAttr = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()
            setSound(soundUri, audioAttr)
            setShowBadge(true)
        }
        manager.createNotificationChannel(completionChannel)
    }

    private fun triggerCompletionAlert() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 1000, 400, 1000, 400, 1500, 500, 1500, 500, 2000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(
                        pattern,
                        intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                        -1
                    ))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }

            // Audible chime / alert feedback for pocket and social gatherings
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, soundUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger completion alert: ${e.message}")
        }
    }

    private fun showCompletionNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val isAr = ChallengePreferences(this).language == "ar"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_ROUTE", "challenge")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isAr) "🎯 اكتملت ساعة مشوار السكينة!" else "🎯 1-Hour Milestone Completed!"
        val body = if (isAr) {
            "أحسنت! الدرع لا يزال نشطاً لحماية صفاء ذهنك. اضغط هنا لتمديد الجلسة أو إنهائها."
        } else {
            "Great job! The offline shield remains active. Tap here to extend or conclude and reconnect."
        }

        val builder = Notification.Builder(this, COMPLETION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        manager.notify(2, builder.build())
        triggerCompletionAlert()
    }

    private fun updateHoldingNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val isAr = ChallengePreferences(this).language == "ar"
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_TARGET_ROUTE", "challenge")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isAr) "🎯 درع الأوفلاين نشط — انتهت الساعة" else "🎯 Offline Shield Active — 1 Hour Complete"
        val body = if (isAr) "اضغط هنا لتمديد الجلسة أو إنهائها والاتصال بالإنترنت" else "Tap here to extend or conclude and reconnect"

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(100, 100, false)
            .setCategory(Notification.CATEGORY_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notification = builder.build()
        @Suppress("DEPRECATION")
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showHalfwayNotification() {
        val locContext = getLocalizedContext()
        val manager = getSystemService(NotificationManager::class.java)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = locContext.getString(R.string.halfway_notification_title)
        val body = locContext.getString(R.string.halfway_notification_body)

        val notification = Notification.Builder(this, COMPLETION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(3, notification)
        triggerHalfwayVibration()
    }

    private fun triggerHalfwayVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return

            val pattern = longArrayOf(0, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    pattern,
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                ))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate for halfway milestone: ${e.message}")
        }
    }

    private fun showBatteryReminderNotification() {
        val locContext = getLocalizedContext()
        val manager = getSystemService(NotificationManager::class.java)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = locContext.getString(R.string.battery_reminder_notification_title)
        val body = locContext.getString(R.string.battery_reminder_notification_body)

        val notification = Notification.Builder(this, COMPLETION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(4, notification)
        triggerBatteryReminderVibration()
    }

    private fun triggerBatteryReminderVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return

            val pattern = longArrayOf(0, 300, 150, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                    pattern,
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                ))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate for battery reminder: ${e.message}")
        }
    }

    private fun buildNotification(progress: Float, remainingMillis: Long): Notification {
        val locContext = getLocalizedContext()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = (progress * 100).toInt()
        val title = "${locContext.getString(R.string.notification_title)} — $percent%"
        val body = formatRemaining(remainingMillis, locContext)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(100, percent, false)
            .setCategory(Notification.CATEGORY_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notification = builder.build()
        @Suppress("DEPRECATION")
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        return notification
    }

    private fun updateNotification(progress: Float, remainingMillis: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(progress, remainingMillis))
    }

    private fun formatRemaining(millis: Long, locContext: android.content.Context): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val timeString = if (hours > 0) "${hours}h ${minutes}m ${seconds}s" else "${minutes}m ${seconds}s"
        return "${locContext.getString(R.string.time_remaining)} $timeString"
    }
}

package com.analoganchor.offlinechallenge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.analoganchor.offlinechallenge.data.ChallengePreferences

/**
 * Restarts the VPN service after device reboot if a challenge is still active.
 * Also arms Layer 2 (WorkManager) and Layer 3 (NetworkCallback) guards as
 * backup safety nets in case the VPN fails to start immediately.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.REBOOT"
        )

        if (intentAction !in validActions) return

        val prefs = ChallengePreferences(context)

        // Only restart if challenge is active and not expired
        if (!prefs.isActive) {
            Log.d("OfflineChallenge", "Boot: No active challenge, skipping VPN restart")
            return
        }

        if (prefs.isExpired()) {
            Log.d("OfflineChallenge", "Boot: Challenge expired during shutdown, cleaning up")
            prefs.endChallenge()
            return
        }

        // Arm Layer 2 & 3 guards immediately — these work even if VPN can't start yet
        try {
            VpnGuardWorker.schedule(context)
        } catch (e: Exception) {
            Log.e("OfflineChallenge", "Boot: Could not schedule VpnGuardWorker: ${e.message}")
        }
        try {
            NetworkGuard.register(context)
        } catch (e: Exception) {
            Log.e("OfflineChallenge", "Boot: Could not register NetworkGuard: ${e.message}")
        }

        // Check if VPN permission is still granted
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            // VPN permission was revoked — can't auto-start without user interaction
            // But Layer 2 & 3 guards are armed and will catch it when app opens
            Log.w("OfflineChallenge", "Boot: VPN permission not granted, guards armed as backup")
            return
        }

        // Restart the VPN service safely (catch background execution restrictions on Android 12+)
        Log.d("OfflineChallenge", "Boot: Restarting VPN for active challenge")
        var startedSuccessfully = false
        try {
            val serviceIntent = Intent(context, MyVpnService::class.java).apply {
                action = MyVpnService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
            startedSuccessfully = true
        } catch (e: Exception) {
            Log.e("OfflineChallenge", "Boot: Could not start foreground service from background: ${e.message}")
        }

        // If VPN didn't start automatically (e.g. background execution block or OEM autostart delay),
        // execute merged Option 1 & 2:
        // Option 1: Direct activity auto-launch
        // Option 2: High-priority heads-up notification (fullScreenIntent)
        if (!startedSuccessfully || !MyVpnService.isRunning) {
            triggerRebootAutoLaunchAndNotification(context)
        }
    }

    private fun triggerRebootAutoLaunchAndNotification(context: Context) {
        val launchIntent = Intent(context, com.analoganchor.offlinechallenge.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_REBOOT_CALIBRATION", true)
        }

        // Option 1: Direct Activity Launch
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.d("OfflineChallenge", "Direct startActivity on boot blocked: ${e.message}")
        }

        // Option 2: High-Priority Heads-Up / FullScreen Notification (Incoming call / Alarm style)
        try {
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, launchIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "reboot_alert_channel"
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Reboot Shield Protection",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts user when reboot protection calibration is required"
                    setBypassDnd(true)
                }
                manager.createNotificationChannel(channel)
            }

            val prefs = ChallengePreferences(context)
            val isAr = prefs.language == "ar"
            val title = if (isAr) "🚨 مطلوب معايرة حماية النظام" else "🚨 System Protection Calibration Required"
            val body = if (isAr) "اضغط فوراً لإتمام معايرة التغطية الدائمة للنظام بعد إعادة التشغيل." else "Tap immediately to calibrate System Always-On Protection after reboot."

            val notification = android.app.Notification.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(com.analoganchor.offlinechallenge.R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setCategory(android.app.Notification.CATEGORY_ALARM)
                .build()

            manager.notify(99, notification)
        } catch (e: Exception) {
            Log.e("OfflineChallenge", "Failed to post high priority notification: ${e.message}")
        }
    }
}

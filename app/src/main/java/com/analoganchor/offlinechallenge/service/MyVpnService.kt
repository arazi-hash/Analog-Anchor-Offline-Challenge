package com.analoganchor.offlinechallenge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
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

        // Start as foreground service immediately
        startForeground(NOTIFICATION_ID, buildNotification(0f, 0L))

        // Build VPN tunnel that blackholes ALL traffic
        val builder = Builder()
            .addAddress("10.1.1.1", 24)
            .addRoute("0.0.0.0", 0)       // Capture all IPv4 traffic
            .setSession(getString(R.string.app_name))
            .setBlocking(false)

        localTunnel = builder.establish()

        if (localTunnel == null) {
            Log.e(TAG, "Failed to establish VPN tunnel")
            stopSelf()
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

        // Update notification every 30 seconds with progress
        notificationUpdater = serviceScope.launch {
            val prefs = ChallengePreferences(this@MyVpnService)
            while (true) {
                delay(30_000)
                if (prefs.isExpired()) {
                    // Challenge timer expired — auto-stop
                    prefs.endChallenge()
                    stopVpn()
                    break
                }
                val progress = prefs.getProgress()
                val remaining = prefs.getRemainingMillis()
                updateNotification(progress, remaining)
            }
        }

        isRunning = true
        Log.d(TAG, "VPN started — all internet traffic blackholed")
        return START_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopVpn() {
        packetLoop?.cancel()
        notificationUpdater?.cancel()
        localTunnel?.close()
        localTunnel = null
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "VPN stopped")
    }

    // ── Notification ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW // Low = no sound, but persistent
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(progress: Float, remainingMillis: Long): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percent = (progress * 100).toInt()
        val title = "${getString(R.string.notification_title)} — $percent%"
        val body = formatRemaining(remainingMillis)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, percent, false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(progress: Float, remainingMillis: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(progress, remainingMillis))
    }

    private fun formatRemaining(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        // Use Arabic string resource pattern
        return "${getString(R.string.time_remaining)} ${hours}h ${minutes}m"
    }
}

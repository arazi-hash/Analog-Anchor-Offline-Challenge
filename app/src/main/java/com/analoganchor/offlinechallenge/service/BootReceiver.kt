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
        try {
            val serviceIntent = Intent(context, MyVpnService::class.java).apply {
                action = MyVpnService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("OfflineChallenge", "Boot: Could not start foreground service from background: ${e.message}")
        }
    }
}

package com.analoganchor.offlinechallenge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.analoganchor.offlinechallenge.data.ChallengePreferences

/**
 * Restarts the VPN service after device reboot if a challenge is still active.
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

        // Check if VPN permission is still granted
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent != null) {
            // VPN permission was revoked — can't auto-start without user interaction
            Log.w("OfflineChallenge", "Boot: VPN permission not granted, cannot auto-restart")
            return
        }

        // Restart the VPN service
        Log.d("OfflineChallenge", "Boot: Restarting VPN for active challenge")
        val serviceIntent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }
}

package com.analoganchor.offlinechallenge.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.analoganchor.offlinechallenge.data.ChallengePreferences
import java.util.concurrent.TimeUnit

/**
 * Layer 2: Periodic safety-net that checks every 15 minutes if the VPN
 * should be running for an active challenge but isn't. Survives reboots,
 * force-stops, and OEM battery optimizations better than BroadcastReceivers.
 */
class VpnGuardWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "OfflineChallenge"
        const val WORK_NAME = "vpn_guard_periodic"

        /**
         * Schedule the periodic guard. Safe to call multiple times —
         * KEEP policy means it won't replace an existing schedule.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<VpnGuardWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "VpnGuardWorker scheduled")
        }

        /** Cancel the periodic guard. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "VpnGuardWorker cancelled")
        }
    }

    override fun doWork(): Result {
        val prefs = ChallengePreferences(applicationContext)

        // No active challenge → stop guarding
        if (!prefs.isActive) {
            Log.d(TAG, "Guard: No active challenge, cancelling periodic work")
            cancel(applicationContext)
            return Result.success()
        }

        // Challenge expired while phone was off or app was killed
        if (prefs.isExpired()) {
            Log.d(TAG, "Guard: Challenge expired, cleaning up")
            prefs.endChallenge()
            prefs.isCompletedPendingShow = true
            cancel(applicationContext)
            com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(applicationContext)
            return Result.success()
        }

        // Challenge active but VPN not running → restart it
        if (!MyVpnService.isRunning) {
            val vpnIntent = VpnService.prepare(applicationContext)
            if (vpnIntent == null) {
                // Permission still granted — restart VPN
                Log.d(TAG, "Guard: VPN not running, restarting")
                try {
                    val serviceIntent = Intent(applicationContext, MyVpnService::class.java).apply {
                        action = MyVpnService.ACTION_START
                    }
                    applicationContext.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Guard: Could not start VPN: ${e.message}")
                }
            } else {
                Log.w(TAG, "Guard: VPN permission revoked, cannot auto-restart")
            }
        } else {
            Log.d(TAG, "Guard: VPN already running, all good")
        }

        return Result.success()
    }
}

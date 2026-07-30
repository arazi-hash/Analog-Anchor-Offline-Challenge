package com.analoganchor.offlinechallenge.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.util.Log
import com.analoganchor.offlinechallenge.data.ChallengePreferences

/**
 * Layer 3: Instant network-availability guard. Fires the moment WiFi or
 * mobile data comes online (even during boot) and immediately restarts
 * the VPN if an active challenge exists. This closes the reboot gap to
 * milliseconds instead of seconds.
 */
object NetworkGuard {

    private const val TAG = "OfflineChallenge"
    private var registered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "NetworkGuard: Network became available")
            // Use applicationContext from the callback's registered context
            appContext?.let { ctx ->
                checkAndRestartVpn(ctx)
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            // Also check when capabilities change (e.g., network gains internet)
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                appContext?.let { ctx ->
                    checkAndRestartVpn(ctx)
                }
            }
        }
    }

    private var appContext: Context? = null

    /**
     * Register the network callback. Safe to call multiple times.
     * Should be called when a challenge starts and from BootReceiver.
     */
    fun register(context: Context) {
        if (registered) return

        val ctx = context.applicationContext
        appContext = ctx

        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, networkCallback)
            registered = true
            Log.d(TAG, "NetworkGuard: Registered")
        } catch (e: Exception) {
            Log.e(TAG, "NetworkGuard: Failed to register: ${e.message}")
        }
    }

    /**
     * Unregister the network callback. Safe to call multiple times.
     * Should be called when a challenge ends.
     */
    fun unregister(context: Context) {
        if (!registered) return

        try {
            val cm = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(networkCallback)
            registered = false
            appContext = null
            Log.d(TAG, "NetworkGuard: Unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "NetworkGuard: Failed to unregister: ${e.message}")
        }
    }

    private fun checkAndRestartVpn(context: Context) {
        val prefs = ChallengePreferences(context)

        if (!prefs.isActive) {
            Log.d(TAG, "NetworkGuard: No active challenge, ignoring")
            return
        }

        if (prefs.isExpired()) {
            Log.d(TAG, "NetworkGuard: Challenge expired, cleaning up")
            prefs.endChallenge()
            prefs.isCompletedPendingShow = true
            unregister(context)
            VpnGuardWorker.cancel(context)
            com.analoganchor.offlinechallenge.widget.ChallengeWidgetReceiver.updateWidget(context)
            return
        }

        if (MyVpnService.isRunning) {
            Log.d(TAG, "NetworkGuard: VPN already running")
            return
        }

        // VPN not running — try to restart
        val vpnIntent = VpnService.prepare(context)
        if (vpnIntent == null) {
            Log.d(TAG, "NetworkGuard: Restarting VPN immediately")
            try {
                val serviceIntent = Intent(context, MyVpnService::class.java).apply {
                    action = MyVpnService.ACTION_START
                }
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "NetworkGuard: Could not start VPN: ${e.message}")
            }
        } else {
            Log.w(TAG, "NetworkGuard: VPN permission revoked, cannot auto-restart")
        }
    }
}

package com.analoganchor.offlinechallenge.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log

object NetworkHelper {

    private const val TAG = "NetworkHelper"

    /**
     * Checks if Wi-Fi or Mobile Data is currently turned on / connected.
     * Uses multiple fallback techniques so vendor restrictions (Xiaomi, Redmi, TCL, Samsung)
     * and VPN blackhole states do not cause false negatives.
     */
    fun isWifiOrDataActive(context: Context): Boolean {
        // 1. WifiManager check
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager?.isWifiEnabled == true) {
                Log.d(TAG, "Active network detected: WifiManager.isWifiEnabled = true")
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "WifiManager check skipped: ${e.message}")
        }

        // 2. Settings.Global Wi-Fi toggle
        try {
            val wifiOn = Settings.Global.getInt(context.contentResolver, Settings.Global.WIFI_ON, 0)
            if (wifiOn != 0) {
                Log.d(TAG, "Active network detected: Settings.Global.WIFI_ON = $wifiOn")
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Settings.Global.WIFI_ON check skipped: ${e.message}")
        }

        // 3. Settings.Global Mobile Data toggle
        val mobileDataKeys = listOf("mobile_data", "mobile_data1", "mobile_data0", "data_roaming")
        for (key in mobileDataKeys) {
            try {
                val dataOn = Settings.Global.getInt(context.contentResolver, key, 0)
                if (dataOn != 0) {
                    Log.d(TAG, "Active network detected: Settings.Global($key) = $dataOn")
                    return true
                }
            } catch (_: Exception) {}
        }

        // 4. TelephonyManager check
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (tm.dataState == TelephonyManager.DATA_CONNECTED ||
                    tm.dataState == TelephonyManager.DATA_CONNECTING) {
                    Log.d(TAG, "Active network detected: TelephonyManager.dataState = ${tm.dataState}")
                    return true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        if (tm.isDataEnabled) {
                            Log.d(TAG, "Active network detected: TelephonyManager.isDataEnabled = true")
                            return true
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "TelephonyManager check skipped: ${e.message}")
        }

        // 5. ConnectivityManager transport checks
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNet = cm.activeNetwork
                    if (activeNet != null) {
                        val caps = cm.getNetworkCapabilities(activeNet)
                        if (caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                             caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))) {
                            Log.d(TAG, "Active network detected: activeNetwork has WIFI or CELLULAR transport")
                            return true
                        }
                    }
                    for (network in cm.allNetworks) {
                        val caps = cm.getNetworkCapabilities(network) ?: continue
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            Log.d(TAG, "Active network detected: network has WIFI or CELLULAR transport")
                            return true
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val netInfo = cm.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    if (netInfo != null && (netInfo.type == ConnectivityManager.TYPE_WIFI || netInfo.type == ConnectivityManager.TYPE_MOBILE)) {
                        Log.d(TAG, "Active network detected: legacy activeNetworkInfo is WIFI or MOBILE")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "ConnectivityManager check skipped: ${e.message}")
        }

        Log.d(TAG, "All checks passed: Wi-Fi and Mobile Data are fully OFF")
        return false
    }

    /**
     * Opens system wireless or network settings so the user can easily toggle off Wi-Fi/Data.
     */
    fun openNetworkSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next intent
            }
        }
    }
}

package com.analoganchor.offlinechallenge.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Utility to open manufacturer-specific Autostart / Background management settings
 * (Xiaomi/Redmi MIUI, Samsung, Huawei, Oppo, Vivo, etc.)
 */
object AutostartHelper {

    private const val TAG = "OfflineChallenge"

    fun openAutostartSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        var opened = false

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                opened = tryIntent(context, "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                        || tryIntent(context, "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartActivity")
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                opened = tryIntent(context, "com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                        || tryIntent(context, "com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity")
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                opened = tryIntent(context, "com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                        || tryIntent(context, "com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            }
            manufacturer.contains("vivo") -> {
                opened = tryIntent(context, "com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                        || tryIntent(context, "com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            }
            manufacturer.contains("samsung") -> {
                opened = tryIntent(context, "com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity")
                        || tryIntent(context, "com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")
            }
        }

        if (!opened) {
            // Fallback: Open standard Battery Optimization settings
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    Log.e(TAG, "Could not open settings: ${ex.message}")
                }
            }
        }
    }

    private fun tryIntent(context: Context, packageName: String, className: String): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(packageName, className)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Failed to launch $packageName/$className: ${e.message}")
            false
        }
    }
}

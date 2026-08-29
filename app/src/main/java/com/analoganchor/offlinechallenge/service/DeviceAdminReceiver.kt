package com.analoganchor.offlinechallenge.service

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.analoganchor.offlinechallenge.data.ChallengePreferences

class DeviceAdminReceiver : android.app.admin.DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        val prefs = ChallengePreferences(context)
        return if (prefs.language == "ar") {
            "تحذير: إلغاء تفعيل حماية المسؤول سيسمح بإلغاء تثبيت التطبيق أثناء التحدي النشط. هل أنت متأكد؟"
        } else {
            "Warning: Deactivating admin protection will allow the app to be uninstalled during your active challenge. Are you sure?"
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device Admin protection deactivated")
    }

    companion object {
        private const val TAG = "DeviceAdminReceiver"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, DeviceAdminReceiver::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            return dpm?.isAdminActive(getComponentName(context)) == true
        }
    }
}

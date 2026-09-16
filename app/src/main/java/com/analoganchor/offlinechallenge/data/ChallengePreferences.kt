package com.analoganchor.offlinechallenge.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the challenge state persisted in SharedPreferences.
 */
class ChallengePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "offline_challenge_prefs"
        private const val KEY_ACTIVE = "challenge_active"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_START_TIME = "challenge_start_time"
        private const val KEY_END_TIME = "challenge_end_time"
        private const val KEY_DURATION_MS = "challenge_duration_ms"
        private const val KEY_CURRENT_REQUEST_STEP = "current_request_step"
        private const val KEY_REQUEST_1_CONSUMED = "request_1_consumed"
        private const val KEY_REQUEST_2_CONSUMED = "request_2_consumed"
        private const val KEY_REQUEST_3_CONSUMED = "request_3_consumed"
        private const val KEY_COMMITMENT_PIN_HASH = "commitment_pin_hash"
        private const val KEY_ALWAYS_ON_ACTIVATED = "always_on_vpn_activated"
        private const val KEY_DEVICE_ADMIN_ACTIVE = "device_admin_active"
        private const val KEY_HALFWAY_NOTIFIED = "challenge_halfway_notified"
        private const val KEY_BATTERY_REMINDER_SENT = "battery_reminder_sent"
        private const val KEY_IS_PARTNER_SESSION = "is_partner_session"
        private const val KEY_PARTNER_NAME = "partner_name"
        private const val KEY_PARTNER_SESSION_ID = "partner_session_id"
        private const val KEY_IS_HOLDING_OFFLINE = "is_holding_offline"
        private const val KEY_IS_GROUP_SESSION = "is_group_session"
        private const val KEY_SESSION_POINTS = "session_points"
    }

    private val directContext: Context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        try {
            context.createDeviceProtectedStorageContext() ?: context
        } catch (e: Exception) {
            context
        }
    } else {
        context
    }

    private val prefs: SharedPreferences = try {
        directContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Exception) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    init {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val normalPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (normalPrefs.contains(KEY_ACTIVE) && !prefs.contains(KEY_ACTIVE)) {
                    val editor = prefs.edit()
                    for ((key, value) in normalPrefs.all) {
                        when (value) {
                            is Boolean -> editor.putBoolean(key, value)
                            is Long -> editor.putLong(key, value)
                            is Int -> editor.putInt(key, value)
                            is String -> editor.putString(key, value)
                        }
                    }
                    editor.apply()
                }
            }
        } catch (e: Exception) {
            // Ignore migration exception
        }
    }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var isActive: Boolean
        get() = prefs.getBoolean(KEY_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_ACTIVE, value).apply()

    var startTimeMillis: Long
        get() = prefs.getLong(KEY_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_START_TIME, value).apply()

    var endTimeMillis: Long
        get() = prefs.getLong(KEY_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_END_TIME, value).apply()

    var durationMillis: Long
        get() = prefs.getLong(KEY_DURATION_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_DURATION_MS, value).apply()

    /** The current expected request step: 1, 2, or 3. */
    var currentRequestStep: Int
        get() = prefs.getInt(KEY_CURRENT_REQUEST_STEP, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_REQUEST_STEP, value).apply()

    fun isRequestConsumed(requestNumber: Int): Boolean {
        val key = when (requestNumber) {
            1 -> KEY_REQUEST_1_CONSUMED
            2 -> KEY_REQUEST_2_CONSUMED
            3 -> KEY_REQUEST_3_CONSUMED
            else -> return true
        }
        return prefs.getBoolean(key, false)
    }

    fun consumeRequest(requestNumber: Int) {
        val key = when (requestNumber) {
            1 -> KEY_REQUEST_1_CONSUMED
            2 -> KEY_REQUEST_2_CONSUMED
            3 -> KEY_REQUEST_3_CONSUMED
            else -> return
        }
        prefs.edit().putBoolean(key, true).apply()
    }

    var discountCode: String?
        get() = prefs.getString("challenge_discount_code", null)
        set(value) = prefs.edit().putString("challenge_discount_code", value).apply()

    var discountAmount: Int
        get() = prefs.getInt("challenge_discount_amount", 0)
        set(value) = prefs.edit().putInt("challenge_discount_amount", value).apply()

    var isCompletedPendingShow: Boolean
        get() = prefs.getBoolean("completed_pending_show", false)
        set(value) = prefs.edit().putBoolean("completed_pending_show", value).apply()

    private fun generateDiscountCodeForDuration(durationMs: Long): String? {
        val prefix = when (durationMs) {
            72 * 60 * 60 * 1000L -> "OFFLINE72"
            36 * 60 * 60 * 1000L -> "OFFLINE36"
            18 * 60 * 60 * 1000L -> "OFFLINE18"
            2 * 60 * 1000L -> "OFFLINETEST"
            else -> null
        } ?: return null

        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "$prefix-$suffix"
    }

    private fun getDiscountAmountForDuration(durationMs: Long): Int {
        return when (durationMs) {
            72 * 60 * 60 * 1000L -> 15
            36 * 60 * 60 * 1000L -> 10
            18 * 60 * 60 * 1000L -> 5
            2 * 60 * 1000L -> 15
            else -> 0
        }
    }

    var isAlwaysOnVpnActivated: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_ON_ACTIVATED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS_ON_ACTIVATED, value).apply()

    var isDeviceAdminActive: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_ADMIN_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEVICE_ADMIN_ACTIVE, value).apply()

    var isHalfwayNotified: Boolean
        get() = prefs.getBoolean(KEY_HALFWAY_NOTIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_HALFWAY_NOTIFIED, value).apply()

    var isBatteryReminderSent: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_REMINDER_SENT, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_REMINDER_SENT, value).apply()

    fun setCommitmentPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_COMMITMENT_PIN_HASH, hash).apply()
    }

    fun verifyCommitmentPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_COMMITMENT_PIN_HASH, null) ?: return true
        return stored == hashPin(pin)
    }

    fun hasCommitmentPin(): Boolean {
        return !prefs.getString(KEY_COMMITMENT_PIN_HASH, null).isNullOrEmpty()
    }

    fun clearCommitmentPin() {
        prefs.edit().remove(KEY_COMMITMENT_PIN_HASH).apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    var isPartnerSession: Boolean
        get() = prefs.getBoolean(KEY_IS_PARTNER_SESSION, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PARTNER_SESSION, value).apply()

    var partnerName: String
        get() = prefs.getString(KEY_PARTNER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PARTNER_NAME, value).apply()

    var partnerSessionId: String
        get() = prefs.getString(KEY_PARTNER_SESSION_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PARTNER_SESSION_ID, value).apply()

    var isHoldingOffline: Boolean
        get() = prefs.getBoolean(KEY_IS_HOLDING_OFFLINE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_HOLDING_OFFLINE, value).apply()

    var isGroupSession: Boolean
        get() = prefs.getBoolean(KEY_IS_GROUP_SESSION, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GROUP_SESSION, value).apply()

    var sessionPoints: Int
        get() = prefs.getInt(KEY_SESSION_POINTS, if (isGroupSession) 70 else 40)
        set(value) = prefs.edit().putInt(KEY_SESSION_POINTS, value).apply()

    /** Start a new challenge. Does NOT reset emergency request state as it is a global limit. */
    fun startChallenge(durationMs: Long, pin: String? = null) {
        val now = System.currentTimeMillis()
        val code = generateDiscountCodeForDuration(durationMs)
        val amount = getDiscountAmountForDuration(durationMs)
        val editor = prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_START_TIME, now)
            .putLong(KEY_END_TIME, now + durationMs)
            .putLong(KEY_DURATION_MS, durationMs)
            .putString("challenge_discount_code", code)
            .putInt("challenge_discount_amount", amount)
            .putBoolean("completed_pending_show", false)
            .putBoolean(KEY_HALFWAY_NOTIFIED, false)
            .putBoolean(KEY_BATTERY_REMINDER_SENT, false)
            .putBoolean(KEY_IS_HOLDING_OFFLINE, false)
        if (!pin.isNullOrEmpty()) {
            editor.putString(KEY_COMMITMENT_PIN_HASH, hashPin(pin))
        }
        editor.apply()
    }

    /** Start a partner challenge initiated from Analog Anchor. */
    fun startPartnerChallenge(
        durationMs: Long,
        partner: String,
        sessionId: String,
        isGroup: Boolean = false,
        points: Int = if (isGroup) 70 else 40
    ) {
        startChallenge(durationMs)
        prefs.edit()
            .putBoolean(KEY_IS_PARTNER_SESSION, true)
            .putString(KEY_PARTNER_NAME, partner)
            .putString(KEY_PARTNER_SESSION_ID, sessionId)
            .putBoolean(KEY_IS_GROUP_SESSION, isGroup)
            .putInt(KEY_SESSION_POINTS, points)
            .putBoolean(KEY_IS_HOLDING_OFFLINE, false)
            .apply()
    }

    /** Extend an active challenge by additional milliseconds (e.g. +30m, +1h, +2h) */
    fun extendChallenge(additionalMs: Long) {
        if (!isActive) return
        val currentEnd = if (isExpired()) System.currentTimeMillis() + additionalMs else endTimeMillis + additionalMs
        val currentDuration = durationMillis
        prefs.edit()
            .putLong(KEY_END_TIME, currentEnd)
            .putLong(KEY_DURATION_MS, currentDuration + additionalMs)
            .putBoolean(KEY_IS_HOLDING_OFFLINE, false)
            .apply()
    }

    /** Send completion broadcast to Analog Anchor */
    fun broadcastPartnerCompletionToAnalogAnchor(context: Context, isSuccess: Boolean = true) {
        if (!isPartnerSession) return
        val elapsedMs = System.currentTimeMillis() - startTimeMillis
        val elapsedMinutes = (elapsedMs / (60 * 1000L)).toInt().coerceAtLeast(1)
        val finalPoints = if (isSuccess) sessionPoints else -25
        val intent = android.content.Intent("com.analoganchor.app.ACTION_PARTNER_CHALLENGE_COMPLETED").apply {
            setPackage("com.analoganchor.app")
            putExtra("EXTRA_SESSION_ID", partnerSessionId)
            putExtra("EXTRA_DURATION_MINUTES", elapsedMinutes)
            putExtra("EXTRA_PARTNER_NAME", partnerName)
            putExtra("EXTRA_SUCCESS", isSuccess)
            putExtra("EXTRA_POINTS", finalPoints)
            putExtra("EXTRA_IS_GROUP", isGroupSession)
        }
        context.sendBroadcast(intent)
    }

    /** End the challenge and clear PIN/protection states. */
    fun endChallenge() {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, false)
            .putBoolean(KEY_IS_HOLDING_OFFLINE, false)
            .remove(KEY_COMMITMENT_PIN_HASH)
            .putBoolean(KEY_ALWAYS_ON_ACTIVATED, false)
            .putBoolean(KEY_HALFWAY_NOTIFIED, false)
            .putBoolean(KEY_BATTERY_REMINDER_SENT, false)
            .putBoolean(KEY_IS_PARTNER_SESSION, false)
            .putBoolean(KEY_IS_GROUP_SESSION, false)
            .remove(KEY_PARTNER_NAME)
            .remove(KEY_PARTNER_SESSION_ID)
            .apply()
    }

    /** Returns progress as 0.0 to 1.0 */
    fun getProgress(): Float {
        if (!isActive) return 0f
        val now = System.currentTimeMillis()
        val total = endTimeMillis - startTimeMillis
        if (total <= 0) return 0f
        val elapsed = (now - startTimeMillis).coerceAtLeast(0)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /** Returns remaining time in milliseconds */
    fun getRemainingMillis(): Long {
        if (!isActive) return 0L
        val remaining = endTimeMillis - System.currentTimeMillis()
        return remaining.coerceAtLeast(0L)
    }

    /** Check if the challenge timer has expired */
    fun isExpired(): Boolean {
        if (!isActive) return false
        return System.currentTimeMillis() >= endTimeMillis
    }

    /** Check if challenge is active and non-expired, but VPN service is not running (e.g. after reboot/kill) */
    fun isShieldBroken(): Boolean {
        if (!isActive) return false
        if (isExpired()) return false
        return !com.analoganchor.offlinechallenge.service.MyVpnService.isRunning
    }

    /** Reset everything */
    fun reset() {
        prefs.edit().clear().apply()
    }
}

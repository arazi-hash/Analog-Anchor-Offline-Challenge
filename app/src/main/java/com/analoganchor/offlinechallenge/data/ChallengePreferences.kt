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
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
            36 * 60 * 60 * 1000L -> "OFFLINE36"
            24 * 60 * 60 * 1000L -> "OFFLINE24"
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
            36 * 60 * 60 * 1000L -> 15
            24 * 60 * 60 * 1000L -> 10
            18 * 60 * 60 * 1000L -> 5
            2 * 60 * 1000L -> 15
            else -> 0
        }
    }

    /** Start a new challenge. Does NOT reset emergency request state as it is a global limit. */
    fun startChallenge(durationMs: Long) {
        val now = System.currentTimeMillis()
        val code = generateDiscountCodeForDuration(durationMs)
        val amount = getDiscountAmountForDuration(durationMs)
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_START_TIME, now)
            .putLong(KEY_END_TIME, now + durationMs)
            .putLong(KEY_DURATION_MS, durationMs)
            .putString("challenge_discount_code", code)
            .putInt("challenge_discount_amount", amount)
            .putBoolean("completed_pending_show", false)
            .apply()
    }

    /** End the challenge. */
    fun endChallenge() {
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
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

    /** Reset everything */
    fun reset() {
        prefs.edit().clear().apply()
    }
}

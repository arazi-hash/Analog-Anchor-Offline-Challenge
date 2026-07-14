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

    /** Start a new challenge. Resets all request consumption states. */
    fun startChallenge(durationMs: Long) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_START_TIME, now)
            .putLong(KEY_END_TIME, now + durationMs)
            .putLong(KEY_DURATION_MS, durationMs)
            .putInt(KEY_CURRENT_REQUEST_STEP, 1)
            .putBoolean(KEY_REQUEST_1_CONSUMED, false)
            .putBoolean(KEY_REQUEST_2_CONSUMED, false)
            .putBoolean(KEY_REQUEST_3_CONSUMED, false)
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

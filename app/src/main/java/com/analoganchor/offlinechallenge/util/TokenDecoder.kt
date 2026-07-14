package com.analoganchor.offlinechallenge.util

import java.text.Normalizer
import java.util.Locale

object TokenDecoder {

    // Serial alphabet used for cover groups and checksums (no 0, 1, I, O to avoid ambiguity)
    private const val SERIAL_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    // Cipher maps: digit -> letter (index = digit value)
    // Request 1: 0→Q, 1→W, 2→E, 3→R, 4→T, 5→A, 6→S, 7→D, 8→F, 9→G
    // Request 2: 0→Z, 1→Y, 2→X, 3→C, 4→V, 5→B, 6→N, 7→M, 8→H, 9→J
    // Request 3: 0→P, 1→U, 2→L, 3→K, 4→A, 5→G, 6→F, 7→D, 8→S, 9→E
    private val ENCODE_ALPHABETS = mapOf(
        1 to "QWERTASDFG",
        2 to "ZYXCVBNMHJ",
        3 to "PULKAGFDSE"
    )

    // Length code: PIN length -> letter
    private val LENGTH_CODE = mapOf(4 to 'K', 5 to 'L', 6 to 'M', 7 to 'N', 8 to 'P')
    private val LENGTH_FROM_CODE = mapOf('K' to 4, 'L' to 5, 'M' to 6, 'N' to 7, 'P' to 8)

    // Request code: request number -> letter (only for follow-ups)
    private val REQUEST_FROM_CODE = mapOf('U' to 2, 'V' to 3)

    // Result of parsing a token
    data class TokenResult(
        val decodedPin: String,
        val requestNumber: Int,
        val tokenType: String // "Legacy short", "Long serial", "Follow-up serial"
    )

    /**
     * Main entry point: takes raw token input (possibly with spaces, commas, unicode junk)
     * and returns the decoded PIN and request number, or null if invalid.
     */
    fun decode(rawToken: String): TokenResult? {
        val clean = normalizeToken(rawToken)
        return parseToken(clean)
    }

    /**
     * Aggressively normalizes raw input:
     * - NFKC Unicode normalization
     * - Uppercase
     * - Replace all dash variants (en-dash, em-dash, minus sign, etc.) with ASCII hyphen
     * - Strip ALL whitespace, commas, invisible Unicode characters
     * - Reconstruct canonical token format
     */
    private fun normalizeToken(raw: String): String {
        val compact = Normalizer.normalize(raw ?: "", Normalizer.Form.NFKC)
            .uppercase(Locale.US)
            .replace('\u2010', '-') // hyphen
            .replace('\u2011', '-') // non-breaking hyphen
            .replace('\u2012', '-') // figure dash
            .replace('\u2013', '-') // en dash
            .replace('\u2014', '-') // em dash
            .replace('\u2212', '-') // minus sign
            .replace('\uFE63', '-') // small hyphen-minus
            .replace('\uFF0D', '-') // fullwidth hyphen-minus
            // Strip ALL whitespace, commas, invisible characters
            .replace(Regex("[\\s\\u00A0\\u061C\\u200B\\u200C\\u200D\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF,،]"), "")

        // Try to match serial token format and reconstruct with proper dashes
        val serialAlphaClass = "[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]"
        val payloadPattern = "(?:[KLMNP]${serialAlphaClass}{8})|(?:[UV][KLMNP]${serialAlphaClass}{8})"
        val serialRegex = Regex(
            "AA-?ANCHOR-?(${serialAlphaClass}{5})-?(${payloadPattern})-?(${serialAlphaClass}{5})-?(${serialAlphaClass}{5})-?(${serialAlphaClass}{5})-?(${serialAlphaClass}{5})-?EXP60"
        )
        val serialMatch = serialRegex.find(compact)
        if (serialMatch != null) {
            return "AA-ANCHOR-${serialMatch.groupValues[1]}-${serialMatch.groupValues[2]}-${serialMatch.groupValues[3]}-${serialMatch.groupValues[4]}-${serialMatch.groupValues[5]}-${serialMatch.groupValues[6]}-EXP60"
        }

        // Try legacy format
        val legacyRegex = Regex("AA-?([QWERTASDFG]{4,8})-?EXP60")
        val legacyMatch = legacyRegex.find(compact)
        if (legacyMatch != null) {
            return "AA-${legacyMatch.groupValues[1]}-EXP60"
        }

        return compact
    }

    private fun parseToken(cleanToken: String): TokenResult? {
        // Try legacy format first: AA-XXXX-EXP60
        val legacyRegex = Regex("^AA-([QWERTASDFG]{4,8})-EXP60$")
        val legacyMatch = legacyRegex.matchEntire(cleanToken)
        if (legacyMatch != null) {
            val encoded = legacyMatch.groupValues[1]
            val decoded = decodePin(encoded, 1) ?: return null
            return TokenResult(decoded, 1, "Legacy short")
        }

        // Try serial format: AA-ANCHOR-{coverA}-{payload}-{coverB}-{coverC}-{coverD}-{checksum}-EXP60
        val serialAlphaClass = "[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]"
        val payloadPattern = "(?:[KLMNP]${serialAlphaClass}{8})|(?:[UV][KLMNP]${serialAlphaClass}{8})"
        val serialRegex = Regex(
            "^AA-ANCHOR-(${serialAlphaClass}{5})-(${payloadPattern})-(${serialAlphaClass}{5})-(${serialAlphaClass}{5})-(${serialAlphaClass}{5})-(${serialAlphaClass}{5})-EXP60$"
        )
        val serialMatch = serialRegex.matchEntire(cleanToken) ?: return null

        val coverA = serialMatch.groupValues[1]
        val payload = serialMatch.groupValues[2]
        val coverB = serialMatch.groupValues[3]
        val coverC = serialMatch.groupValues[4]
        val coverD = serialMatch.groupValues[5]
        val checksum = serialMatch.groupValues[6]

        // Verify checksum
        val expectedChecksum = serialChecksum(coverA + payload + coverB + coverC + coverD)
        if (checksum != expectedChecksum) return null

        // Parse payload
        return parsePayload(payload)
    }

    private fun parsePayload(payload: String): TokenResult? {
        val firstChar = payload.firstOrNull() ?: return null

        // Determine request number
        val requestNumber: Int
        val lengthIndex: Int
        if (REQUEST_FROM_CODE.containsKey(firstChar)) {
            requestNumber = REQUEST_FROM_CODE[firstChar]!!
            lengthIndex = 1
        } else {
            requestNumber = 1
            lengthIndex = 0
        }

        // Extract PIN length
        val lengthCode = payload.getOrNull(lengthIndex) ?: return null
        val pinLength = LENGTH_FROM_CODE[lengthCode] ?: return null

        // Extract encoded PIN
        val encodedPin = payload.drop(lengthIndex + 1).take(pinLength)
        if (encodedPin.length != pinLength) return null

        // Decode PIN
        val decoded = decodePin(encodedPin, requestNumber) ?: return null

        val tokenType = if (requestNumber == 1) "Long serial" else "Follow-up serial"
        return TokenResult(decoded, requestNumber, tokenType)
    }

    /**
     * Decodes encoded letters back to PIN digits using the reverse cipher map.
     */
    private fun decodePin(encoded: String, requestNumber: Int): String? {
        val alphabet = ENCODE_ALPHABETS[requestNumber] ?: return null
        return encoded.map { ch ->
            val digit = alphabet.indexOf(ch)
            if (digit == -1) return null
            digit.digitToChar()
        }.joinToString("")
    }

    /**
     * Checksum algorithm — MUST match support-tool.html serialChecksum() exactly.
     * 5 rounds, each producing one character from SERIAL_ALPHABET.
     */
    private fun serialChecksum(value: String): String {
        val sb = StringBuilder(5)
        for (round in 0 until 5) {
            var acc = (17L + round * 101L)
            for ((index, ch) in value.withIndex()) {
                acc = (acc * 33L + ch.code.toLong() + index.toLong() * (round + 7).toLong()) % 1_000_000_007L
            }
            sb.append(SERIAL_ALPHABET[(acc % SERIAL_ALPHABET.length).toInt()])
        }
        return sb.toString()
    }
}

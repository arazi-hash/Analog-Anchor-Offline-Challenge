package com.analoganchor.offlinechallenge.util

/**
 * Obfuscated storage for the master PINs.
 * PINs are stored as XOR-encoded byte arrays rather than string literals
 * to resist casual decompilation with jadx/apktool.
 *
 * This is NOT cryptographic security — it's obfuscation against casual inspection.
 */
object PinVault {

    // XOR salt — an arbitrary byte used to encode the PIN digits
    private const val SALT: Byte = 0x5A

    // Encoded PINs: each digit XOR'd with SALT
    // Request 1 PIN: 5222 → digits [5,2,2,2] → XOR with 0x5A → [0x5F, 0x58, 0x58, 0x58]
    // Request 2 PIN: 2555 → digits [2,5,5,5] → XOR with 0x5A → [0x58, 0x5F, 0x5F, 0x5F]
    // Request 3 PIN: 3555 → digits [3,5,5,5] → XOR with 0x5A → [0x59, 0x5F, 0x5F, 0x5F]
    private val ENCODED_PINS = mapOf(
        1 to byteArrayOf(0x5F.toByte(), 0x58.toByte(), 0x58.toByte(), 0x58.toByte()),
        2 to byteArrayOf(0x58.toByte(), 0x5F.toByte(), 0x5F.toByte(), 0x5F.toByte()),
        3 to byteArrayOf(0x59.toByte(), 0x5F.toByte(), 0x5F.toByte(), 0x5F.toByte())
    )

    /**
     * Returns the decoded PIN for the given request number, or null if invalid.
     */
    fun getPin(requestNumber: Int): String? {
        val encoded = ENCODED_PINS[requestNumber] ?: return null
        return encoded.map { b -> ((b.toInt() xor SALT.toInt()) and 0x0F).digitToChar() }
            .joinToString("")
    }

    /**
     * Verifies that a decoded PIN matches the expected PIN for the given request.
     */
    fun verify(decodedPin: String, requestNumber: Int): Boolean {
        val expected = getPin(requestNumber) ?: return false
        // Constant-time comparison to avoid timing attacks (overkill but good practice)
        if (decodedPin.length != expected.length) return false
        var result = 0
        for (i in decodedPin.indices) {
            result = result or (decodedPin[i].code xor expected[i].code)
        }
        return result == 0
    }
}

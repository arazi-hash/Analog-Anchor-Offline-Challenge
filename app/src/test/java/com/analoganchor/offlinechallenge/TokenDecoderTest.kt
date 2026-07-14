package com.analoganchor.offlinechallenge

import com.analoganchor.offlinechallenge.util.TokenDecoder
import com.analoganchor.offlinechallenge.util.PinVault
import org.junit.Assert.*
import org.junit.Test

class TokenDecoderTest {

    // Test helper: builds a token the same way support-tool.html does
    private val SERIAL_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val ENCODE_MAPS = mapOf(
        1 to mapOf('0' to 'Q', '1' to 'W', '2' to 'E', '3' to 'R', '4' to 'T', '5' to 'A', '6' to 'S', '7' to 'D', '8' to 'F', '9' to 'G'),
        2 to mapOf('0' to 'Z', '1' to 'Y', '2' to 'X', '3' to 'C', '4' to 'V', '5' to 'B', '6' to 'N', '7' to 'M', '8' to 'H', '9' to 'J'),
        3 to mapOf('0' to 'P', '1' to 'U', '2' to 'L', '3' to 'K', '4' to 'A', '5' to 'G', '6' to 'F', '7' to 'D', '8' to 'S', '9' to 'E')
    )
    private val LENGTH_CODE = mapOf(4 to 'K', 5 to 'L', 6 to 'M', 7 to 'N', 8 to 'P')
    private val REQUEST_CODE = mapOf(2 to 'U', 3 to 'V')

    private fun buildTestToken(pin: String, requestNumber: Int = 1): String {
        val encodeMap = ENCODE_MAPS[requestNumber]!!
        val encoded = pin.map { encodeMap[it]!! }.joinToString("")
        val lengthCode = LENGTH_CODE[encoded.length]!!
        val padding = "".padEnd(8 - encoded.length, '2')
        val payloadBody = "$lengthCode${encoded}${padding}"
        val payload = if (requestNumber == 1) payloadBody else "${REQUEST_CODE[requestNumber]}$payloadBody"
        val coverA = "A2B3C"
        val coverB = "D4E5F"
        val coverC = "G6H7J"
        val coverD = "K8L9M"
        val checksum = serialChecksum(coverA + payload + coverB + coverC + coverD)
        return "AA-ANCHOR-$coverA-$payload-$coverB-$coverC-$coverD-$checksum-EXP60"
    }

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

    @Test
    fun `decode request 1 token with PIN 5222`() {
        val token = buildTestToken("5222", 1)
        val result = TokenDecoder.decode(token)
        assertNotNull(result)
        assertEquals("5222", result!!.decodedPin)
        assertEquals(1, result.requestNumber)
    }

    @Test
    fun `decode request 2 token with PIN 2555`() {
        val token = buildTestToken("2555", 2)
        val result = TokenDecoder.decode(token)
        assertNotNull(result)
        assertEquals("2555", result!!.decodedPin)
        assertEquals(2, result.requestNumber)
    }

    @Test
    fun `decode request 3 token with PIN 3555`() {
        val token = buildTestToken("3555", 3)
        val result = TokenDecoder.decode(token)
        assertNotNull(result)
        assertEquals("3555", result!!.decodedPin)
        assertEquals(3, result.requestNumber)
    }

    @Test
    fun `PinVault returns correct PINs`() {
        assertEquals("5222", PinVault.getPin(1))
        assertEquals("2555", PinVault.getPin(2))
        assertEquals("3555", PinVault.getPin(3))
        assertNull(PinVault.getPin(4))
    }

    @Test
    fun `PinVault verify works`() {
        assertTrue(PinVault.verify("5222", 1))
        assertTrue(PinVault.verify("2555", 2))
        assertTrue(PinVault.verify("3555", 3))
        assertFalse(PinVault.verify("1234", 1))
        assertFalse(PinVault.verify("5222", 2)) // right pin, wrong request
    }

    @Test
    fun `token with injected spaces still decodes`() {
        val token = buildTestToken("5222", 1)
        val dirty = token.replace("-", " - ") // spaces around dashes
        val result = TokenDecoder.decode(dirty)
        assertNotNull(result)
        assertEquals("5222", result!!.decodedPin)
    }

    @Test
    fun `token with injected commas still decodes`() {
        val token = buildTestToken("5222", 1)
        // Insert commas after every 4 chars
        val dirty = token.chunked(4).joinToString(",")
        val result = TokenDecoder.decode(dirty)
        assertNotNull(result)
        assertEquals("5222", result!!.decodedPin)
    }

    @Test
    fun `token with unicode dashes still decodes`() {
        val token = buildTestToken("5222", 1)
        val dirty = token.replace('-', '\u2013') // en-dash instead of hyphen
        val result = TokenDecoder.decode(dirty)
        assertNotNull(result)
        assertEquals("5222", result!!.decodedPin)
    }

    @Test
    fun `invalid checksum is rejected`() {
        val token = buildTestToken("5222", 1)
        // Corrupt the last group before -EXP60
        val corrupted = token.dropLast(9) + "ZZZZZ-EXP60"
        val result = TokenDecoder.decode(corrupted)
        assertNull(result)
    }

    @Test
    fun `legacy token format decodes correctly`() {
        // Legacy format: AA-AEEE-EXP60 (PIN 5222 with cipher 1)
        val result = TokenDecoder.decode("AA-AEEE-EXP60")
        assertNotNull(result)
        assertEquals("5222", result!!.decodedPin)
        assertEquals(1, result.requestNumber)
        assertEquals("Legacy short", result.tokenType)
    }
}

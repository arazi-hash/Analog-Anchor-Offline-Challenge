import Foundation

/**
 * Obfuscated storage for the master PINs.
 * PINs are stored as XOR-encoded byte arrays rather than string literals
 * to resist casual inspection and static analysis.
 *
 * 1:1 Swift port of Android com.analoganchor.offlinechallenge.util.PinVault
 */
public enum PinVault {

    // XOR salt — arbitrary byte used to encode the PIN digits
    private static let salt: UInt8 = 0x5A

    // Encoded PINs: each digit XOR'd with salt
    // Request 1 PIN: 5222 → digits [5,2,2,2] → XOR with 0x5A → [0x5F, 0x58, 0x58, 0x58]
    // Request 2 PIN: 2555 → digits [2,5,5,5] → XOR with 0x5A → [0x58, 0x5F, 0x5F, 0x5F]
    // Request 3 PIN: 3555 → digits [3,5,5,5] → XOR with 0x5A → [0x59, 0x5F, 0x5F, 0x5F]
    private static let encodedPins: [Int: [UInt8]] = [
        1: [0x5F, 0x58, 0x58, 0x58],
        2: [0x58, 0x5F, 0x5F, 0x5F],
        3: [0x59, 0x5F, 0x5F, 0x5F]
    ]

    /**
     * Returns the decoded PIN for the given request number, or nil if invalid.
     */
    public static func getPin(for requestNumber: Int) -> String? {
        guard let encoded = encodedPins[requestNumber] else { return nil }
        let digits = encoded.map { byte -> Character in
            let digitVal = (byte ^ salt) & 0x0F
            return Character(String(digitVal))
        }
        return String(digits)
    }

    /**
     * Verifies that a decoded PIN matches the expected PIN for the given request number.
     * Uses constant-time byte comparison.
     */
    public static func verify(decodedPin: String, requestNumber: Int) -> Bool {
        guard let expected = getPin(for: requestNumber) else { return false }
        guard decodedPin.count == expected.count else { return false }

        var result: UInt8 = 0
        let decodedBytes = Array(decodedPin.utf8)
        let expectedBytes = Array(expected.utf8)
        for i in 0..<decodedBytes.count {
            result |= (decodedBytes[i] ^ expectedBytes[i])
        }
        return result == 0
    }
}

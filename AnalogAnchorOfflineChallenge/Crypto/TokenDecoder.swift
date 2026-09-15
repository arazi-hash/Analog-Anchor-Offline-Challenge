import Foundation

/**
 * Token decoder for emergency offline challenge bypass.
 *
 * 1:1 Swift port of Android com.analoganchor.offlinechallenge.util.TokenDecoder
 *
 * Handles NFKC Unicode normalization, character stripping, dash homogenization,
 * 5-round polynomial checksum calculation, and inverse cipher mapping.
 */
public enum TokenDecoder {

    // Serial alphabet used for cover groups and checksums (excludes 0, 1, I, O to avoid ambiguity)
    private static let serialAlphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    // Cipher maps: digit -> letter (index = digit value)
    // Request 1: 0→Q, 1→W, 2→E, 3→R, 4→T, 5→A, 6→S, 7→D, 8→F, 9→G
    // Request 2: 0→Z, 1→Y, 2→X, 3→C, 4→V, 5→B, 6→N, 7→M, 8→H, 9→J
    // Request 3: 0→P, 1→U, 2→L, 3→K, 4→A, 5→G, 6→F, 7→D, 8→S, 9→E
    private static let encodeAlphabets: [Int: String] = [
        1: "QWERTASDFG",
        2: "ZYXCVBNMHJ",
        3: "PULKAGFDSE"
    ]

    // Length code: letter -> PIN length
    private static let lengthFromCode: [Character: Int] = [
        "K": 4, "L": 5, "M": 6, "N": 7, "P": 8
    ]

    // Request code: letter -> request number (only for follow-up requests)
    private static let requestFromCode: [Character: Int] = [
        "U": 2, "V": 3
    ]

    // Result of parsing a token
    public struct TokenResult: Equatable {
        public let decodedPin: String
        public let requestNumber: Int
        public let tokenType: String // "Legacy short", "Long serial", "Follow-up serial"

        public init(decodedPin: String, requestNumber: Int, tokenType: String) {
            self.decodedPin = decodedPin
            self.requestNumber = requestNumber
            self.tokenType = tokenType
        }
    }

    /**
     * Main entry point: takes raw token input (possibly with spaces, commas, unicode junk)
     * and returns the decoded PIN and request number, or nil if invalid.
     */
    public static func decode(_ rawToken: String) -> TokenResult? {
        let clean = normalizeToken(rawToken)
        return parseToken(clean)
    }

    /**
     * Aggressively normalizes raw input:
     * - NFKC Unicode normalization
     * - Uppercase
     * - Replace all dash variants with ASCII hyphen
     * - Strip ALL whitespace, commas, and invisible Unicode characters
     * - Reconstruct canonical token format
     */
    public static func normalizeToken(_ raw: String) -> String {
        var compact = raw.precomposedStringWithCanonicalMapping.uppercased()

        // Replace all dash variants
        let dashVariants: [Character] = [
            "\u{2010}", "\u{2011}", "\u{2012}", "\u{2013}",
            "\u{2014}", "\u{2212}", "\u{FE63}", "\u{FF0D}"
        ]
        for dash in dashVariants {
            compact = compact.replacingOccurrences(of: String(dash), with: "-")
        }

        // Strip ALL whitespace, commas, invisible characters
        let stripPattern = "[\\s\\u00A0\\u061C\\u200B\\u200C\\u200D\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF,،]"
        compact = compact.replacingOccurrences(of: stripPattern, with: "", options: .regularExpression)

        // Try to match serial token format and reconstruct with proper dashes
        let serialAlphaClass = "[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]"
        let payloadPattern = "(?:[KLMNP]\(serialAlphaClass){8})|(?:[UV][KLMNP]\(serialAlphaClass){8})"
        let serialRegexPattern = "AA-?ANCHOR-?(\(serialAlphaClass){5})-?(\(payloadPattern))-?(\(serialAlphaClass){5})-?(\(serialAlphaClass){5})-?(\(serialAlphaClass){5})-?(\(serialAlphaClass){5})-?EXP60"

        if let regex = try? NSRegularExpression(pattern: serialRegexPattern) {
            let range = NSRange(compact.startIndex..<compact.endIndex, in: compact)
            if let match = regex.firstMatch(in: compact, options: [], range: range) {
                if let r1 = Range(match.range(at: 1), in: compact),
                   let r2 = Range(match.range(at: 2), in: compact),
                   let r3 = Range(match.range(at: 3), in: compact),
                   let r4 = Range(match.range(at: 4), in: compact),
                   let r5 = Range(match.range(at: 5), in: compact),
                   let r6 = Range(match.range(at: 6), in: compact) {
                    return "AA-ANCHOR-\(compact[r1])-\(compact[r2])-\(compact[r3])-\(compact[r4])-\(compact[r5])-\(compact[r6])-EXP60"
                }
            }
        }

        // Try legacy format
        let legacyRegexPattern = "AA-?([QWERTASDFG]{4,8})-?EXP60"
        if let regex = try? NSRegularExpression(pattern: legacyRegexPattern) {
            let range = NSRange(compact.startIndex..<compact.endIndex, in: compact)
            if let match = regex.firstMatch(in: compact, options: [], range: range) {
                if let r1 = Range(match.range(at: 1), in: compact) {
                    return "AA-\(compact[r1])-EXP60"
                }
            }
        }

        return compact
    }

    private static func parseToken(_ cleanToken: String) -> TokenResult? {
        // Try legacy format first: AA-XXXX-EXP60
        let legacyPattern = "^AA-([QWERTASDFG]{4,8})-EXP60$"
        if let regex = try? NSRegularExpression(pattern: legacyPattern) {
            let range = NSRange(cleanToken.startIndex..<cleanToken.endIndex, in: cleanToken)
            if let match = regex.firstMatch(in: cleanToken, options: [], range: range),
               let r1 = Range(match.range(at: 1), in: cleanToken) {
                let encoded = String(cleanToken[r1])
                guard let decoded = decodePin(encoded: encoded, requestNumber: 1) else { return nil }
                return TokenResult(decodedPin: decoded, requestNumber: 1, tokenType: "Legacy short")
            }
        }

        // Try serial format: AA-ANCHOR-{coverA}-{payload}-{coverB}-{coverC}-{coverD}-{checksum}-EXP60
        let serialAlphaClass = "[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]"
        let payloadPattern = "(?:[KLMNP]\(serialAlphaClass){8})|(?:[UV][KLMNP]\(serialAlphaClass){8})"
        let serialPattern = "^AA-ANCHOR-(\(serialAlphaClass){5})-(\(payloadPattern))-(\(serialAlphaClass){5})-(\(serialAlphaClass){5})-(\(serialAlphaClass){5})-(\(serialAlphaClass){5})-EXP60$"

        guard let regex = try? NSRegularExpression(pattern: serialPattern) else { return nil }
        let range = NSRange(cleanToken.startIndex..<cleanToken.endIndex, in: cleanToken)
        guard let match = regex.firstMatch(in: cleanToken, options: [], range: range) else { return nil }

        guard let r1 = Range(match.range(at: 1), in: cleanToken),
              let r2 = Range(match.range(at: 2), in: cleanToken),
              let r3 = Range(match.range(at: 3), in: cleanToken),
              let r4 = Range(match.range(at: 4), in: cleanToken),
              let r5 = Range(match.range(at: 5), in: cleanToken),
              let r6 = Range(match.range(at: 6), in: cleanToken) else {
            return nil
        }

        let coverA = String(cleanToken[r1])
        let payload = String(cleanToken[r2])
        let coverB = String(cleanToken[r3])
        let coverC = String(cleanToken[r4])
        let coverD = String(cleanToken[r5])
        let checksum = String(cleanToken[r6])

        // Verify checksum
        let expectedChecksum = serialChecksum(value: coverA + payload + coverB + coverC + coverD)
        guard checksum == expectedChecksum else { return nil }

        // Parse payload
        return parsePayload(payload: payload)
    }

    private static func parsePayload(payload: String) -> TokenResult? {
        guard let firstChar = payload.first else { return nil }

        let requestNumber: Int
        let lengthIndex: Int
        if let req = requestFromCode[firstChar] {
            requestNumber = req
            lengthIndex = 1
        } else {
            requestNumber = 1
            lengthIndex = 0
        }

        let chars = Array(payload)
        guard lengthIndex < chars.count else { return nil }
        let lengthChar = chars[lengthIndex]
        guard let pinLength = lengthFromCode[lengthChar] else { return nil }

        let startIndex = lengthIndex + 1
        guard startIndex + pinLength <= chars.count else { return nil }
        let encodedPin = String(chars[startIndex..<startIndex + pinLength])

        guard let decoded = decodePin(encoded: encodedPin, requestNumber: requestNumber) else { return nil }
        let tokenType = (requestNumber == 1) ? "Long serial" : "Follow-up serial"
        return TokenResult(decodedPin: decoded, requestNumber: requestNumber, tokenType: tokenType)
    }

    private static func decodePin(encoded: String, requestNumber: Int) -> String? {
        guard let alphabet = encodeAlphabets[requestNumber] else { return nil }
        let alphaChars = Array(alphabet)

        var result = ""
        for ch in encoded {
            guard let idx = alphaChars.firstIndex(of: ch) else { return nil }
            result.append(String(idx))
        }
        return result
    }

    /**
     * Checksum algorithm — MUST match support-tool.html and TokenDecoder.kt serialChecksum() exactly.
     * 5 rounds, each producing one character from SERIAL_ALPHABET.
     */
    public static func serialChecksum(value: String) -> String {
        let serialChars = Array(serialAlphabet)
        var result = ""

        for round in 0..<5 {
            var acc: Int64 = 17 + Int64(round) * 101
            for (index, ch) in value.enumerated() {
                guard let scalar = ch.unicodeScalars.first else { continue }
                let code = Int64(scalar.value)
                acc = (acc * 33 + code + Int64(index) * Int64(round + 7)) % 1_000_000_007
            }
            let charIndex = Int(acc % Int64(serialChars.count))
            result.append(serialChars[charIndex])
        }

        return result
    }
}

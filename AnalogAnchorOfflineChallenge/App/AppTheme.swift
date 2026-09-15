import SwiftUI

// MARK: - Brand Design System
// Derived from the Analog Anchor brand identity: Obsidian, Cyan Glow, and Amber

public enum AppTheme {
    // Primary Background: Deep Obsidian
    public static let obsidian = Color(hex: 0x0A0E14)
    
    // Surface Card & Container
    public static let cardSurface = Color(hex: 0x121820)
    public static let cardBorder = Color(hex: 0x1E293B)
    
    // Brand Accents
    public static let cyanGlow = Color(hex: 0x00E5FF)
    public static let cyanDark = Color(hex: 0x0097A7)
    public static let amber = Color(hex: 0xFFB300)      // 2-Minute Test Mode
    
    // Text Hierarchy
    public static let textPrimary = Color(hex: 0xF8FAFC)
    public static let textSecondary = Color(hex: 0x94A3B8)
    public static let textMuted = Color(hex: 0x64748B)
    
    // Success / Completion
    public static let successGreen = Color(hex: 0x10B981)
}

// MARK: - Color Hex Initializer
extension Color {
    public init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}

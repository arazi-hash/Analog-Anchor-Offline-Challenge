import SwiftUI

public struct EmergencyUnlockModal: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var screenTimeManager: ScreenTimeManager
    
    @State private var tokenInput: String = ""
    @State private var errorMessage: String?
    @State private var shakeOffset: CGFloat = 0
    @State private var isVerifying = false
    
    public init(screenTimeManager: ScreenTimeManager) {
        self.screenTimeManager = screenTimeManager
    }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.obsidian
                    .ignoresSafeArea()
                
                VStack(spacing: 24) {
                    // Header Warning Icon
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 40))
                            .foregroundColor(AppTheme.amber)
                        
                        Text("Emergency Bypass Gate")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(AppTheme.textPrimary)
                        
                        Text("To prevent impulsive relapse, this challenge cannot be casually canceled. Enter an emergency token issued by your accountability partner or founder.")
                            .font(.system(size: 13))
                            .foregroundColor(AppTheme.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 8)
                    }
                    .padding(.top, 16)
                    
                    // Token Input Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("AUTHORIZATION TOKEN")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(AppTheme.textMuted)
                            .tracking(1)
                        
                        TextField("AA-ANCHOR-...", text: $tokenInput)
                            .font(.system(size: 14, weight: .medium, design: .monospaced))
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.characters)
                            .padding(14)
                            .background(AppTheme.cardSurface)
                            .foregroundColor(AppTheme.cyanGlow)
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(errorMessage != nil ? Color.red : AppTheme.cardBorder, lineWidth: 1)
                            )
                            .offset(x: shakeOffset)
                        
                        if let error = errorMessage {
                            HStack(spacing: 4) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 11))
                                Text(error)
                                    .font(.system(size: 12))
                            }
                            .foregroundColor(.red)
                            .padding(.leading, 2)
                        }
                    }
                    
                    // Unlock Button
                    Button {
                        verifyAndUnlock()
                    } label: {
                        HStack {
                            if isVerifying {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .black))
                            } else {
                                Text("Verify & Clear Shields")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.black)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(tokenInput.trimmingCharacters(in: .whitespaces).isEmpty ? AppTheme.cardBorder : AppTheme.cyanGlow)
                        .cornerRadius(10)
                    }
                    .disabled(tokenInput.trimmingCharacters(in: .whitespaces).isEmpty || isVerifying)
                    
                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Emergency Unlock")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(AppTheme.cardSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Dismiss") {
                        dismiss()
                    }
                    .foregroundColor(AppTheme.textSecondary)
                }
            }
        }
    }
    
    private func verifyAndUnlock() {
        errorMessage = nil
        isVerifying = true
        
        let trimmed = tokenInput.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // 1. Decode token using native Swift TokenDecoder
        guard let result = TokenDecoder.decode(trimmed) else {
            triggerShake(error: "Invalid token format or invalid checksum.")
            isVerifying = false
            return
        }
        
        // 2. Verify decoded PIN against PinVault for the request number
        // Request 1: 5222, Request 2: 2555, Request 3: 3555
        let isValid = PinVault.verify(decodedPin: result.decodedPin, requestNumber: result.requestNumber)
        
        guard isValid else {
            triggerShake(error: "Token decoded successfully, but cryptographic PIN mismatch.")
            isVerifying = false
            return
        }
        
        // 3. Valid! Clear all Screen Time shields immediately
        screenTimeManager.emergencyUnlock()
        isVerifying = false
        dismiss()
    }
    
    private func triggerShake(error: String) {
        self.errorMessage = error
        withAnimation(Animation.default.repeatCount(4, autoreverses: true).speed(6)) {
            shakeOffset = 10
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            shakeOffset = 0
        }
    }
}

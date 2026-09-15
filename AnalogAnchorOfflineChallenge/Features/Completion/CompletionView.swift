import SwiftUI

public struct CompletionView: View {
    @ObservedObject var screenTimeManager: ScreenTimeManager
    @State private var appearAnimation = false
    
    public init(screenTimeManager: ScreenTimeManager) {
        self.screenTimeManager = screenTimeManager
    }
    
    public var body: some View {
        ZStack {
            AppTheme.obsidian
                .ignoresSafeArea()
            
            VStack(spacing: 32) {
                Spacer()
                
                // Celebratory Glow Icon
                ZStack {
                    Circle()
                        .fill(AppTheme.cyanGlow.opacity(0.15))
                        .frame(width: 140, height: 140)
                        .scaleEffect(appearAnimation ? 1.1 : 0.9)
                    
                    Circle()
                        .fill(AppTheme.cyanGlow.opacity(0.3))
                        .frame(width: 100, height: 100)
                    
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 52))
                        .foregroundColor(AppTheme.cyanGlow)
                }
                
                // Title & Affirmation
                VStack(spacing: 12) {
                    Text("Challenge Completed!")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(AppTheme.textPrimary)
                    
                    Text("You have reclaimed your presence.\nYour commitment protected your mind.")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(AppTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                }
                
                // Unshield Status Pill
                HStack(spacing: 8) {
                    Image(systemName: "lock.open.fill")
                        .foregroundColor(AppTheme.successGreen)
                    Text("All apps automatically unshielded")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppTheme.successGreen)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(AppTheme.successGreen.opacity(0.12))
                .cornerRadius(20)
                
                Spacer()
                
                // Outbound Ecosystem CTA Button
                VStack(spacing: 12) {
                    Link(destination: URL(string: "https://get-analog-anchor.com/")!) {
                        HStack(spacing: 8) {
                            Text("Discover the Main Analog Anchor Ecosystem")
                                .font(.system(size: 15, weight: .bold))
                            Image(systemName: "arrow.up.right")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(AppTheme.cyanGlow)
                        .cornerRadius(14)
                        .shadow(color: AppTheme.cyanGlow.opacity(0.4), radius: 8, x: 0, y: 4)
                    }
                    
                    // Reset / Return to Setup Button
                    Button {
                        withAnimation {
                            screenTimeManager.currentSession = nil
                        }
                    } label: {
                        Text("Start Another Challenge")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(AppTheme.textSecondary)
                            .padding(.vertical, 8)
                    }
                }
                .padding(.bottom, 24)
            }
            .padding(.horizontal, 24)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) {
                appearAnimation = true
            }
            // Ensure all shields are cleared
            screenTimeManager.completeChallenge()
        }
    }
}

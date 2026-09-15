import SwiftUI
import Combine

public struct ChallengeView: View {
    @ObservedObject var screenTimeManager: ScreenTimeManager
    @State private var showingEmergencyModal = false
    @State private var currentDate = Date()
    
    // 1-second interval timer
    private let timer = Timer.publish(every: 1.0, on: .main, in: .common).autoconnect()
    
    public init(screenTimeManager: ScreenTimeManager) {
        self.screenTimeManager = screenTimeManager
    }
    
    public var body: some View {
        ZStack {
            AppTheme.obsidian
                .ignoresSafeArea()
            
            VStack(spacing: 32) {
                // Header Status
                HStack(spacing: 8) {
                    Circle()
                        .fill(AppTheme.cyanGlow)
                        .frame(width: 8, height: 8)
                        .shadow(color: AppTheme.cyanGlow, radius: 4)
                    
                    Text("CHALLENGE ACTIVE • SHIELD ENGAGED")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(AppTheme.cyanGlow)
                        .tracking(1)
                }
                .padding(.top, 16)
                
                Spacer()
                
                // Circular Progress Ring & Live Countdown
                if let session = screenTimeManager.currentSession {
                    circularProgressView(session: session)
                }
                
                Spacer()
                
                // Information Card
                shieldStatusCard
                
                // Emergency Bypass Button (No Casual Give Up)
                VStack(spacing: 8) {
                    Button {
                        showingEmergencyModal = true
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "key.fill")
                                .font(.system(size: 12))
                            Text("Emergency Unlock")
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .foregroundColor(AppTheme.amber)
                        .padding(.vertical, 12)
                        .padding(.horizontal, 20)
                        .background(AppTheme.amber.opacity(0.12))
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(AppTheme.amber.opacity(0.3), lineWidth: 1)
                        )
                    }
                    
                    Text("Casual cancellation is disabled to protect your pre-commitment.")
                        .font(.system(size: 11))
                        .foregroundColor(AppTheme.textMuted)
                        .multilineTextAlignment(.center)
                }
                .padding(.bottom, 24)
            }
            .padding(.horizontal, 24)
        }
        .onReceive(timer) { newDate in
            currentDate = newDate
            
            // Check if timer expired
            if let session = screenTimeManager.currentSession, session.isExpired {
                screenTimeManager.completeChallenge()
            }
        }
        .sheet(isPresented: $showingEmergencyModal) {
            EmergencyUnlockModal(screenTimeManager: screenTimeManager)
        }
    }
    
    // MARK: - Circular Progress View
    private func circularProgressView(session: ChallengeSession) -> some View {
        let progress = session.progressFraction
        let remainingText = session.formattedRemainingTime
        let percent = Int(progress * 100)
        
        return ZStack {
            // Background Track Ring
            Circle()
                .stroke(AppTheme.cardBorder, lineWidth: 14)
                .frame(width: 250, height: 250)
            
            // Animated Glowing Progress Ring
            Circle()
                .trim(from: 0.0, to: CGFloat(progress))
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: [AppTheme.cyanDark, AppTheme.cyanGlow]),
                        center: .center,
                        startAngle: .degrees(0),
                        endAngle: .degrees(360)
                    ),
                    style: StrokeStyle(lineWidth: 14, lineCap: .round)
                )
                .frame(width: 250, height: 250)
                .rotationEffect(.degrees(-90))
                .animation(.linear(duration: 0.5), value: progress)
            
            // Center Metrics
            VStack(spacing: 8) {
                Text(remainingText)
                    .font(.system(size: 28, weight: .bold, design: .monospaced))
                    .foregroundColor(AppTheme.textPrimary)
                
                Text("\(percent)% COMPLETED")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundColor(AppTheme.cyanGlow)
                    .tracking(1)
                
                if session.isTestMode {
                    Text("2-MIN TEST MODE")
                        .font(.system(size: 9, weight: .heavy))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(AppTheme.amber.opacity(0.2))
                        .foregroundColor(AppTheme.amber)
                        .cornerRadius(4)
                        .padding(.top, 4)
                }
            }
        }
    }
    
    // MARK: - Shield Status Card
    private var shieldStatusCard: some View {
        HStack(spacing: 14) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 24))
                .foregroundColor(AppTheme.cyanGlow)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Distractions Actively Shielded")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                
                Text("Opening blocked apps displays Apple's native shield.")
                    .font(.system(size: 12))
                    .foregroundColor(AppTheme.textSecondary)
            }
            
            Spacer()
        }
        .padding(16)
        .background(AppTheme.cardSurface)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppTheme.cardBorder, lineWidth: 1)
        )
    }
}

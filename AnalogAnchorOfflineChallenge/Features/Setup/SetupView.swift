import SwiftUI
import FamilyControls

public struct SetupView: View {
    @ObservedObject var screenTimeManager: ScreenTimeManager
    @State private var showingPickerSheet = false
    @State private var showingPermissionAlert = false
    
    public init(screenTimeManager: ScreenTimeManager) {
        self.screenTimeManager = screenTimeManager
    }
    
    public var body: some View {
        ZStack {
            AppTheme.obsidian
                .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 24) {
                    // 1. Header Hero
                    headerHero
                    
                    // 2. Attention Philosophy Card (Pre-Commitment)
                    philosophyCard
                    
                    // 3. App Selection Trigger
                    appSelectionSection
                    
                    // 4. Duration Selection (Pre-Commitment Matrix)
                    durationMatrixSection
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
            }
        }
        .sheet(isPresented: $showingPickerSheet) {
            AppPickerSheet(screenTimeManager: screenTimeManager)
        }
        .alert("Screen Time Access Required", isPresented: $showingPermissionAlert) {
            Button("Grant Access") {
                Task {
                    await screenTimeManager.requestAuthorization()
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("To shield distracting applications and maintain your pre-commitment, Analog Anchor requires Apple Screen Time permission.")
        }
    }
    
    // MARK: - 1. Header Hero
    private var headerHero: some View {
        VStack(spacing: 8) {
            Image(systemName: "shield.checkered")
                .font(.system(size: 44, weight: .semibold))
                .foregroundColor(AppTheme.cyanGlow)
                .padding(.bottom, 4)
            
            Text("Analog Anchor Offline Challenge")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(AppTheme.textPrimary)
                .multilineTextAlignment(.center)
            
            Text("Choose intentional presence over subconscious screen consumption.")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
        .padding(.top, 12)
    }
    
    // MARK: - 2. Philosophy Card
    private var philosophyCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "sparkles")
                    .foregroundColor(AppTheme.cyanGlow)
                Text("Pre-Commitment Philosophy")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppTheme.cyanGlow)
                Spacer()
            }
            
            Text("Constant connectivity subtly fractures our attention and robs our daily presence. By locking your distractions before temptations strike, you protect what matters most.")
                .font(.system(size: 13, weight: .regular))
                .foregroundColor(AppTheme.textSecondary)
                .lineSpacing(3)
            
            Link(destination: URL(string: "https://get-analog-anchor.com/")!) {
                HStack(spacing: 6) {
                    Text("After completing this challenge, the main Analog Anchor app helps you build conscious control")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(AppTheme.cyanGlow)
                        .multilineTextAlignment(.leading)
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(AppTheme.cyanGlow)
                }
                .padding(.top, 4)
            }
        }
        .padding(16)
        .background(AppTheme.cardSurface)
        .cornerRadius(14)
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(AppTheme.cardBorder, lineWidth: 1)
        )
    }
    
    // MARK: - 3. App Selection Section
    private var appSelectionSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("STEP 1: CHOOSE DISTRACTIONS")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(AppTheme.textMuted)
                .tracking(1)
            
            Button {
                if screenTimeManager.authorizationStatus == .approved {
                    showingPickerSheet = true
                } else {
                    showingPermissionAlert = true
                }
            } label: {
                HStack {
                    Image(systemName: "apps.iphone")
                        .font(.system(size: 18))
                        .foregroundColor(AppTheme.cyanGlow)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Select Shielded Apps")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AppTheme.textPrimary)
                        
                        let count = screenTimeManager.activitySelection.applicationTokens.count
                        let catCount = screenTimeManager.activitySelection.categoryTokens.count
                        Text(count == 0 && catCount == 0 ? "No apps selected yet" : "\(count) apps, \(catCount) categories shielded")
                            .font(.system(size: 12))
                            .foregroundColor(AppTheme.textSecondary)
                    }
                    
                    Spacer()
                    
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppTheme.textMuted)
                }
                .padding(16)
                .background(AppTheme.cardSurface)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(AppTheme.cyanGlow.opacity(0.3), lineWidth: 1)
                )
            }
        }
    }
    
    // MARK: - 4. Duration Matrix Section
    private var durationMatrixSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("STEP 2: COMMIT OFFLINE DURATION")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(AppTheme.textMuted)
                .tracking(1)
            
            VStack(spacing: 10) {
                ForEach(ChallengeDurationOption.options) { option in
                    Button {
                        startChallenge(with: option)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 8) {
                                    Text(option.title)
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(option.isTestMode ? AppTheme.amber : AppTheme.textPrimary)
                                    
                                    if option.isTestMode {
                                        Text("TEST MODE")
                                            .font(.system(size: 9, weight: .heavy))
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(AppTheme.amber.opacity(0.2))
                                            .foregroundColor(AppTheme.amber)
                                            .cornerRadius(4)
                                    }
                                }
                                
                                Text(option.subtitle)
                                    .font(.system(size: 12))
                                    .foregroundColor(AppTheme.textSecondary)
                            }
                            
                            Spacer()
                            
                            HStack(spacing: 4) {
                                Text("Engage")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(option.isTestMode ? AppTheme.amber : AppTheme.cyanGlow)
                                Image(systemName: "lock.shield.fill")
                                    .font(.system(size: 13))
                                    .foregroundColor(option.isTestMode ? AppTheme.amber : AppTheme.cyanGlow)
                            }
                        }
                        .padding(16)
                        .background(AppTheme.cardSurface)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(option.isTestMode ? AppTheme.amber.opacity(0.5) : AppTheme.cardBorder, lineWidth: 1)
                        )
                    }
                }
            }
        }
    }
    
    private func startChallenge(with option: ChallengeDurationOption) {
        guard screenTimeManager.authorizationStatus == .approved else {
            showingPermissionAlert = true
            return
        }
        
        withAnimation {
            screenTimeManager.startChallenge(durationSeconds: option.durationSeconds)
        }
    }
}

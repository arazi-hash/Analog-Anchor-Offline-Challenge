import Foundation
import Combine
import SwiftUI
import FamilyControls
import ManagedSettings
import DeviceActivity

/**
 * ScreenTimeManager coordinates Apple Screen Time frameworks:
 * - FamilyControls: Individual device authorization and FamilyActivitySelection
 * - ManagedSettings: System-level kernel shield enforcement
 * - State persistence in App Group UserDefaults
 */
@MainActor
public final class ScreenTimeManager: ObservableObject {
    public static let shared = ScreenTimeManager()
    
    // App Group Suite Name
    public static let appGroupSuite = "group.com.analoganchor.offlinechallenge"
    private static let sessionKey = "active_challenge_session"
    private static let selectionKey = "shielded_activity_selection"
    
    // Apple Screen Time Subsystems
    private let managedStore = ManagedSettingsStore()
    private let userDefaults = UserDefaults(suiteName: appGroupSuite) ?? UserDefaults.standard
    
    // Published State
    @Published public var authorizationStatus: AuthorizationStatus = .notDetermined
    @Published public var activitySelection = FamilyActivitySelection()
    @Published public var currentSession: ChallengeSession?
    @Published public var isShieldActive: Bool = false
    
    private init() {
        loadPersistedState()
        checkAuthorizationStatus()
    }
    
    // MARK: - Authorization
    public func requestAuthorization() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            authorizationStatus = AuthorizationCenter.shared.authorizationStatus
        } catch {
            print("ScreenTimeManager: Authorization failed: \(error.localizedDescription)")
            authorizationStatus = AuthorizationCenter.shared.authorizationStatus
        }
    }
    
    public func checkAuthorizationStatus() {
        authorizationStatus = AuthorizationCenter.shared.authorizationStatus
    }
    
    // MARK: - Shield Enforcement
    public func startChallenge(durationSeconds: TimeInterval) {
        let session = ChallengeSession(targetDurationSeconds: durationSeconds)
        self.currentSession = session
        persistSession(session)
        
        // Enforce kernel-level shield
        applyShields()
    }
    
    public func applyShields() {
        // Apply shielded applications from user selection
        managedStore.shield.applications = activitySelection.applicationTokens
        
        // Optional: shield web domains associated with distracting categories
        if !activitySelection.categoryTokens.isEmpty {
            managedStore.shield.applicationCategories = .specific(activitySelection.categoryTokens)
        }
        
        isShieldActive = true
    }
    
    public func emergencyUnlock() {
        // Clear all shields immediately
        managedStore.clearAllSettings()
        
        if var session = currentSession {
            session.isManuallyUnlocked = true
            self.currentSession = session
        }
        
        clearPersistedSession()
        isShieldActive = false
    }
    
    public func completeChallenge() {
        // Clear all shields on natural expiry
        managedStore.clearAllSettings()
        clearPersistedSession()
        isShieldActive = false
    }
    
    // MARK: - App Selection Persistence
    public func saveActivitySelection() {
        do {
            let data = try JSONEncoder().encode(activitySelection)
            userDefaults.set(data, forKey: Self.selectionKey)
        } catch {
            print("Failed to persist activity selection: \(error)")
        }
    }
    
    private func loadPersistedState() {
        // Load Selection
        if let data = userDefaults.data(forKey: Self.selectionKey),
           let selection = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) {
            self.activitySelection = selection
        }
        
        // Load Active Session
        if let data = userDefaults.data(forKey: Self.sessionKey),
           let session = try? JSONDecoder().decode(ChallengeSession.self, from: data) {
            if !session.isExpired && !session.isManuallyUnlocked {
                self.currentSession = session
                self.isShieldActive = true
            } else {
                // Expired while app was closed
                clearPersistedSession()
            }
        }
    }
    
    private func persistSession(_ session: ChallengeSession) {
        if let data = try? JSONEncoder().encode(session) {
            userDefaults.set(data, forKey: Self.sessionKey)
        }
    }
    
    private func clearPersistedSession() {
        userDefaults.removeObject(forKey: Self.sessionKey)
    }
}

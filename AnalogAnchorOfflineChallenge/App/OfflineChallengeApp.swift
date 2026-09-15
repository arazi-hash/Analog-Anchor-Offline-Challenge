import SwiftUI
import FamilyControls

@main
struct OfflineChallengeApp: App {
    @StateObject private var screenTimeManager = ScreenTimeManager.shared
    
    var body: some Scene {
        WindowGroup {
            Group {
                if let session = screenTimeManager.currentSession {
                    if session.isExpired {
                        CompletionView(screenTimeManager: screenTimeManager)
                    } else {
                        ChallengeView(screenTimeManager: screenTimeManager)
                    }
                } else {
                    SetupView(screenTimeManager: screenTimeManager)
                }
            }
            .preferredColorScheme(.dark)
            .task {
                // Request Screen Time authorization on initial launch
                await screenTimeManager.requestAuthorization()
            }
        }
    }
}

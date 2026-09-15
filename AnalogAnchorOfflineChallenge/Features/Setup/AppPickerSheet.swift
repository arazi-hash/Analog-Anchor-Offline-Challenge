import SwiftUI
import FamilyControls

public struct AppPickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var screenTimeManager: ScreenTimeManager
    
    public init(screenTimeManager: ScreenTimeManager) {
        self.screenTimeManager = screenTimeManager
    }
    
    public var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.obsidian
                    .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // Apple's Native Privacy-Safe Activity Picker
                    FamilyActivityPicker(selection: $screenTimeManager.activitySelection)
                        .background(AppTheme.obsidian)
                }
            }
            .navigationTitle("Select Shielded Apps")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(AppTheme.cardSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        screenTimeManager.saveActivitySelection()
                        dismiss()
                    }
                    .foregroundColor(AppTheme.cyanGlow)
                    .fontWeight(.semibold)
                }
            }
        }
    }
}

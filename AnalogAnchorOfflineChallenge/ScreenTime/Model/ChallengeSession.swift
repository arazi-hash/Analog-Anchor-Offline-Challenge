import Foundation

// MARK: - Challenge Duration Option
public struct ChallengeDurationOption: Identifiable, Hashable {
    public let id = UUID()
    public let title: String
    public let subtitle: String
    public let durationSeconds: TimeInterval
    public let isTestMode: Bool
    
    public static let options: [ChallengeDurationOption] = [
        ChallengeDurationOption(
            title: "2 Minutes",
            subtitle: "Quick Test Mode",
            durationSeconds: 120,
            isTestMode: true
        ),
        ChallengeDurationOption(
            title: "12 Hours",
            subtitle: "Half-Day Focus",
            durationSeconds: 12 * 3600,
            isTestMode: false
        ),
        ChallengeDurationOption(
            title: "18 Hours",
            subtitle: "Deep Evening Reset",
            durationSeconds: 18 * 3600,
            isTestMode: false
        ),
        ChallengeDurationOption(
            title: "36 Hours",
            subtitle: "Full Weekend Detox",
            durationSeconds: 36 * 3600,
            isTestMode: false
        ),
        ChallengeDurationOption(
            title: "72 Hours",
            subtitle: "3-Day Digital Pilgrimage",
            durationSeconds: 72 * 3600,
            isTestMode: false
        )
    ]
}

// MARK: - Challenge Session Model
public struct ChallengeSession: Codable, Equatable {
    public let id: UUID
    public let startTime: Date
    public let targetDurationSeconds: TimeInterval
    public var isManuallyUnlocked: Bool
    
    public init(
        id: UUID = UUID(),
        startTime: Date = Date(),
        targetDurationSeconds: TimeInterval,
        isManuallyUnlocked: Bool = false
    ) {
        self.id = id
        self.startTime = startTime
        self.targetDurationSeconds = targetDurationSeconds
        self.isManuallyUnlocked = isManuallyUnlocked
    }
    
    public var targetEndTime: Date {
        startTime.addingTimeInterval(targetDurationSeconds)
    }
    
    public var remainingSeconds: TimeInterval {
        max(0, targetEndTime.timeIntervalSince(Date()))
    }
    
    public var elapsedSeconds: TimeInterval {
        max(0, Date().timeIntervalSince(startTime))
    }
    
    public var progressFraction: Double {
        guard targetDurationSeconds > 0 else { return 1.0 }
        let progress = elapsedSeconds / targetDurationSeconds
        return min(1.0, max(0.0, progress))
    }
    
    public var isExpired: Bool {
        remainingSeconds <= 0
    }
    
    public var isTestMode: Bool {
        targetDurationSeconds <= 120
    }
    
    public var formattedRemainingTime: String {
        let totalSec = Int(remainingSeconds)
        let hours = totalSec / 3600
        let minutes = (totalSec % 3600) / 60
        let seconds = totalSec % 60
        
        if hours > 0 {
            return String(format: "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            return String(format: "%02dm %02ds", minutes, seconds)
        }
    }
}

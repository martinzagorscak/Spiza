import Foundation
import Shared
import FirebaseCrashlytics

final class SwiftCrashLoggerBridge: IosCrashLoggerBridge {
    func recordException(message: String, stackTrace: [String]) {
        let error = NSError(
            domain: "ComposeMultiplatform",
            code: 0,
            userInfo: [
                NSLocalizedDescriptionKey: message,
                "kotlin_stack_trace": stackTrace.joined(separator: "\n")
            ]
        )
        Crashlytics.crashlytics().record(error: error)
    }
}

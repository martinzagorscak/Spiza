import FirebaseRemoteConfig
import Foundation
import Shared

final class SwiftRemoteConfigBridge: NSObject, IosRemoteConfigBridge {
    private let lock = NSLock()
    private var configuredRemoteConfig: RemoteConfig?

    // Created on first use because this bridge is created (in iOSApp.init) before FirebaseApp.configure() runs, and
    // RemoteConfig.remoteConfig() requires a configured FirebaseApp. Not a `lazy var`: Kotlin may call the bridge from several
    // threads at once, and `lazy` is not thread-safe, so the first access is guarded by a lock.
    private var remoteConfig: RemoteConfig {
        lock.lock()
        defer { lock.unlock() }
        if let configuredRemoteConfig {
            return configuredRemoteConfig
        }
        let remoteConfig = Self.makeRemoteConfig()
        configuredRemoteConfig = remoteConfig
        return remoteConfig
    }

    // Same as FirebaseRemoteConfigDataSource: settings and defaults (RemoteConfigKeys.defaults) are applied once, before first use.
    private static func makeRemoteConfig() -> RemoteConfig {
        let remoteConfig = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        #if DEBUG
        // Debug builds fetch on every check, so a value changed in the console shows up on the next start.
        settings.minimumFetchInterval = 0
        #else
        settings.minimumFetchInterval = 3600
        #endif
        remoteConfig.configSettings = settings
        // Kotlin Long/Boolean arrive as NSNumber and String as NSString, so every default is an NSObject.
        remoteConfig.setDefaults(RemoteConfigKeys.shared.defaults.compactMapValues { $0 as? NSObject })
        return remoteConfig
    }

    func fetchAndActivate(onComplete: @escaping (BridgeError?) -> Void) {
        // Strong self: onComplete must always be called, and the bridge lives as long as the app anyway.
        remoteConfig.fetchAndActivate { _, error in
            onComplete(error.map { self.toFetchBridgeError($0) })
        }
    }

    func getLong(key: String) -> Int64 {
        remoteConfig.configValue(forKey: key).numberValue.int64Value
    }

    func getBoolean(key: String) -> Bool {
        remoteConfig.configValue(forKey: key).boolValue
    }

    func getString(key: String) -> String {
        remoteConfig.configValue(forKey: key).stringValue
    }

    /// Remote Config wraps network failures in its own error domain, so the generic `toBridgeError()` (Firestore codes)
    /// would classify them as Unknown. Throttling and network failures are temporary, so they are reported as Offline.
    private func toFetchBridgeError(_ error: Error) -> BridgeError {
        let nsError = error as NSError
        let underlyingError = nsError.userInfo[NSUnderlyingErrorKey] as? NSError
        let isThrottled = nsError.domain == RemoteConfigErrorDomain && nsError.code == RemoteConfigError.throttled.rawValue
        if isThrottled || underlyingError?.domain == NSURLErrorDomain {
            return BridgeError(reason: "Offline", message: error.localizedDescription)
        }
        return error.toBridgeError()
    }
}

import Shared
import UIKit

/// Receives Home Screen Quick Actions. SwiftUI keeps owning the window: this delegate never sets one.
final class SceneDelegate: NSObject, UIWindowSceneDelegate {
    // Cold start: the tapped action arrives with the connection options and performActionFor is not called.
    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        if let shortcutItem = connectionOptions.shortcutItem {
            _ = handle(shortcutItem)
        }
    }

    // Warm start: the app was already running.
    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        completionHandler(handle(shortcutItem))
    }

    private func handle(_ shortcutItem: UIApplicationShortcutItem) -> Bool {
        IosAppShortcutsKt.handleAppShortcut(type: shortcutItem.type)
    }
}

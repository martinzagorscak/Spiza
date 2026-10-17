import FirebaseCore
import GoogleSignIn
import Shared
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()

        return true
    }
}

@main
struct iOSApp: App {
    // register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        InitKoinIosKt.doInitKoinIos(
            crashLoggerBridge: SwiftCrashLoggerBridge(),
            authBridge: SwiftAuthBridge(),
            userBridge: SwiftUserBridge(),
            householdBridge: SwiftHouseholdBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

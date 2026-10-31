import AuthenticationServices
import CryptoKit
import FirebaseAuth
import FirebaseCore
import Foundation
import GoogleSignIn
import Shared
import UIKit

private final class AuthListenerHandle: NSObject, IosListenerHandle {
    private let handle: AuthStateDidChangeListenerHandle

    init(_ handle: AuthStateDidChangeListenerHandle) {
        self.handle = handle
    }

    func remove() {
        Auth.auth().removeStateDidChangeListener(handle)
    }
}

final class SwiftAuthBridge: NSObject, IosAuthBridge {

    func currentUser() -> AuthUser? {
        Auth.auth().currentUser?.toAuthUser()
    }

    func signIn(provider: AuthProvider, onResult: @escaping (AuthUser?, KotlinBoolean, String?) -> Void) {
        // Kotlin may call this from any thread; UIKit and the sign-in SDKs need the main actor.
        Task { @MainActor in
            switch provider {
            case .google: self.signInWithGoogle(onResult)
            case .apple: onResult(nil, KotlinBoolean(bool: false), "Unsupported provider for now")  // TODO: #ImplementAppleAuthProvider
            default: onResult(nil, KotlinBoolean(bool: false), "Unsupported provider")
            }
        }
    }

    func signOut(onComplete: @escaping (String?) -> Void) {
        do {
            GIDSignIn.sharedInstance.signOut()
            try Auth.auth().signOut()
            onComplete(nil)
        } catch {
            onComplete(error.localizedDescription)
        }
    }

    func observeAuthState(onChange: @escaping (AuthUser?) -> Void) -> IosListenerHandle {
        let handle = Auth.auth().addStateDidChangeListener { _, user in
            onChange(user?.toAuthUser())
        }
        return AuthListenerHandle(handle)
    }

    @MainActor
    private func signInWithGoogle(_ onResult: @escaping (AuthUser?, KotlinBoolean, String?) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID,
            let presenter = Self.topViewController()
        else {
            onResult(nil, KotlinBoolean(bool: false), "Missing Firebase clientID or presenting view controller")
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                let cancelled = error.code == GIDSignInError.canceled.rawValue
                onResult(nil, KotlinBoolean(bool: cancelled), error.localizedDescription)
                return
            }
            guard let idToken = result?.user.idToken?.tokenString, let user = result?.user else {
                onResult(nil, KotlinBoolean(bool: false), "No Google ID token")
                return
            }
            let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: user.accessToken.tokenString)
            Self.firebaseSignIn(credential, onResult)
        }
    }

    fileprivate static func firebaseSignIn(_ credential: AuthCredential, _ onResult: @escaping (AuthUser?, KotlinBoolean, String?) -> Void) {
        Auth.auth().signIn(with: credential) { result, error in
            if let error = error {
                onResult(nil, KotlinBoolean(bool: false), error.localizedDescription)
            } else {
                onResult(result?.user.toAuthUser(), KotlinBoolean(bool: false), result == nil ? "No user" : nil)
            }
        }
    }

    @MainActor
    fileprivate static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

extension User {
    fileprivate func toAuthUser() -> AuthUser {
        AuthUser(uid: uid, displayName: displayName, email: email, photoUrl: photoURL?.absoluteString)
    }
}

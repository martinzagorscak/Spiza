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

/// Outcome of the Google account picker, shared by sign-in and reauthentication.
private enum GoogleCredentialResult {
    case credential(AuthCredential)
    case cancelled(Error)
    case failure(Error)
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

    func reauthenticate(onResult: @escaping (AuthUser?, KotlinBoolean, BridgeError?) -> Void) {
        // Kotlin may call this from any thread; UIKit and the sign-in SDKs need the main actor.
        Task { @MainActor in
            guard let user = Auth.auth().currentUser else {
                onResult(nil, KotlinBoolean(bool: false), Self.bridgeError("No signed-in user to reauthenticate"))
                return
            }
            // TODO: #ImplementAppleAuthProvider - reauthenticate Apple users once Apple sign-in exists.
            guard user.providerData.contains(where: { $0.providerID == GoogleAuthProvider.id }) else {
                onResult(nil, KotlinBoolean(bool: false), Self.bridgeError("Reauthentication is only supported for Google users"))
                return
            }
            self.googleCredential { result in
                switch result {
                case .cancelled(let error):
                    onResult(nil, KotlinBoolean(bool: true), error.toBridgeError())
                case .failure(let error):
                    onResult(nil, KotlinBoolean(bool: false), error.toBridgeError())
                case .credential(let credential):
                    // Firebase rejects a credential of another account with a user-mismatch error (17024); Kotlin tells it apart.
                    user.reauthenticate(with: credential) { _, error in
                        if let error = error as NSError?, error.domain == "FIRAuthErrorDomain", error.code == 17024 {
                            let mismatch = BridgeError(reason: AuthBridgeErrors.shared.USER_MISMATCH, message: error.localizedDescription)
                            onResult(nil, KotlinBoolean(bool: false), mismatch)
                        } else if let error = error {
                            onResult(nil, KotlinBoolean(bool: false), error.toBridgeError())
                        } else {
                            onResult(user.toAuthUser(), KotlinBoolean(bool: false), nil)
                        }
                    }
                }
            }
        }
    }

    func deleteCurrentUser(onComplete: @escaping (BridgeError?, BridgeError?) -> Void) {
        guard let user = Auth.auth().currentUser else {
            onComplete(Self.bridgeError("No signed-in user to delete"), nil)
            return
        }
        user.delete { error in
            if let error = error {
                onComplete(error.toBridgeError(), nil)
                return
            }
            // The Firebase user is already gone; a failed disconnect is only reported so Kotlin can log it.
            Task { @MainActor in
                GIDSignIn.sharedInstance.disconnect { error in
                    onComplete(nil, error?.toBridgeError())
                }
            }
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
        googleCredential { result in
            switch result {
            case .cancelled(let error):
                onResult(nil, KotlinBoolean(bool: true), error.localizedDescription)
            case .failure(let error):
                onResult(nil, KotlinBoolean(bool: false), error.localizedDescription)
            case .credential(let credential):
                Self.firebaseSignIn(credential, onResult)
            }
        }
    }

    /// Shows the Google account picker and returns the Firebase credential of the picked account.
    @MainActor
    private func googleCredential(_ completion: @escaping (GoogleCredentialResult) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID,
            let presenter = Self.topViewController()
        else {
            completion(.failure(Self.authError("Missing Firebase clientID or presenting view controller")))
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                let cancelled = error.code == GIDSignInError.canceled.rawValue
                completion(cancelled ? .cancelled(error) : .failure(error))
                return
            }
            guard let idToken = result?.user.idToken?.tokenString, let user = result?.user else {
                completion(.failure(Self.authError("No Google ID token")))
                return
            }
            completion(.credential(GoogleAuthProvider.credential(withIDToken: idToken, accessToken: user.accessToken.tokenString)))
        }
    }

    private static func authError(_ message: String) -> Error {
        NSError(domain: "SpizaAuth", code: 0, userInfo: [NSLocalizedDescriptionKey: message])
    }

    private static func bridgeError(_ message: String) -> BridgeError {
        BridgeError(reason: "Unknown", message: message)
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

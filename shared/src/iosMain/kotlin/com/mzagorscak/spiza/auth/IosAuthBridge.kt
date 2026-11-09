package com.mzagorscak.spiza.auth

import com.mzagorscak.spiza.data.BridgeError
import com.mzagorscak.spiza.data.household.IosListenerHandle

/** Implemented in Swift (iosApp), which owns FirebaseAuth, GoogleSignIn and Sign in with Apple. */
interface IosAuthBridge {
    fun currentUser(): AuthUser?

    fun signIn(
        provider: AuthProvider,
        onResult: (user: AuthUser?, cancelled: Boolean, errorMessage: String?) -> Unit,
    )

    fun signOut(onComplete: (errorMessage: String?) -> Unit)

    /**
     * Re-runs the sign-in flow of the current user's provider and reauthenticates them in Firebase.
     * Exactly one outcome: [onResult] gets the user, `cancelled = true`, or an error. A different account is an error with
     * reason [AuthBridgeErrors.USER_MISMATCH].
     */
    fun reauthenticate(onResult: (user: AuthUser?, cancelled: Boolean, error: BridgeError?) -> Unit)

    /**
     * Deletes the Firebase user, then disconnects the Google session. [error] is the failed delete (the session is
     * then left as it is); [sessionRevokeError] only reports a failed disconnect after a successful delete.
     */
    fun deleteCurrentUser(onComplete: (error: BridgeError?, sessionRevokeError: BridgeError?) -> Unit)

    /** Registers a new listener per call (auth state has several collectors); the handle removes only that listener. */
    fun observeAuthState(onChange: (AuthUser?) -> Unit): IosListenerHandle
}

/** Auth-specific [BridgeError.reason]s, shared with Swift as `AuthBridgeErrors.shared`. */
object AuthBridgeErrors {
    const val USER_MISMATCH = "UserMismatch"
}

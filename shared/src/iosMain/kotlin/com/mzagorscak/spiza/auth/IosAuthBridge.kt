package com.mzagorscak.spiza.auth

/** Implemented in Swift (iosApp), which owns FirebaseAuth, GoogleSignIn and Sign in with Apple. */
interface IosAuthBridge {
    fun currentUser(): AuthUser?

    fun signIn(
        provider: AuthProvider,
        onResult: (user: AuthUser?, cancelled: Boolean, errorMessage: String?) -> Unit,
    )

    fun signOut(onComplete: (errorMessage: String?) -> Unit)

    fun startObservingAuthState(onChange: (AuthUser?) -> Unit)

    fun stopObservingAuthState()
}

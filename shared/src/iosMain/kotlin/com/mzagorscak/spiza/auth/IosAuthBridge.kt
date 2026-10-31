package com.mzagorscak.spiza.auth

import com.mzagorscak.spiza.data.household.IosListenerHandle

/** Implemented in Swift (iosApp), which owns FirebaseAuth, GoogleSignIn and Sign in with Apple. */
interface IosAuthBridge {
    fun currentUser(): AuthUser?

    fun signIn(
        provider: AuthProvider,
        onResult: (user: AuthUser?, cancelled: Boolean, errorMessage: String?) -> Unit,
    )

    fun signOut(onComplete: (errorMessage: String?) -> Unit)

    /** Registers a new listener per call (auth state has several collectors); the handle removes only that listener. */
    fun observeAuthState(onChange: (AuthUser?) -> Unit): IosListenerHandle
}

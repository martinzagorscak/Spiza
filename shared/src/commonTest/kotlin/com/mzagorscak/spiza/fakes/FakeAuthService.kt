package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Signed in as [TEST_USER] by default; a successful [signIn] and [signOut] are reflected in [user] like Firebase Auth. */
class FakeAuthService(
    signedInUser: AuthUser? = TEST_USER,
) : AuthService {
    /** The signed-in user; set it to null to sign out. */
    val user = MutableStateFlow(signedInUser)

    /** What [signIn] returns. */
    var signInResult: AuthResult = AuthResult.Success(TEST_USER)

    override val authState: Flow<AuthUser?> = user

    override val currentUser: AuthUser? get() = user.value

    override suspend fun signIn(provider: AuthProvider): AuthResult =
        signInResult.also { result ->
            if (result is AuthResult.Success) user.value = result.user
        }

    override suspend fun signOut() {
        user.value = null
    }
}

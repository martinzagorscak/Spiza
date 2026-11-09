package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Signed in as [TEST_USER] by default; a successful [signIn], [signOut] and [deleteCurrentUser] are reflected in [user]
 * like Firebase Auth.
 */
class FakeAuthService(
    signedInUser: AuthUser? = TEST_USER,
) : AuthService {
    /** The signed-in user; set it to null to sign out. */
    val user = MutableStateFlow(signedInUser)

    /** What [signIn] returns. */
    var signInResult: AuthResult = AuthResult.Success(TEST_USER)

    /** What [reauthenticate] returns. */
    var reauthenticateResult: AuthResult = AuthResult.Success(TEST_USER)

    /** When set, [deleteCurrentUser] throws it and the user stays signed in. */
    var deleteUserFailure: Throwable? = null

    var reauthenticateCalls = 0

    /** Every [deleteCurrentUser] attempt, recorded **before** [deleteUserFailure] applies. */
    var deleteUserCalls = 0

    override val authState: Flow<AuthUser?> = user

    override val currentUser: AuthUser? get() = user.value

    override suspend fun signIn(provider: AuthProvider): AuthResult =
        signInResult.also { result ->
            if (result is AuthResult.Success) user.value = result.user
        }

    override suspend fun signOut() {
        user.value = null
    }

    override suspend fun reauthenticate(): AuthResult {
        reauthenticateCalls++
        return reauthenticateResult
    }

    override suspend fun deleteCurrentUser() {
        deleteUserCalls++
        deleteUserFailure?.let { throw it }
        user.value = null
    }
}

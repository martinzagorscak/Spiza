package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthService(
    signedInUser: AuthUser? = TEST_USER,
) : AuthService {
    val user = MutableStateFlow(signedInUser)
    var signInResult: AuthResult = AuthResult.Success(TEST_USER)

    override val authState: Flow<AuthUser?> = user

    override val currentUser: AuthUser? get() = user.value

    override suspend fun signIn(provider: AuthProvider): AuthResult = signInResult

    override suspend fun signOut() {
        user.value = null
    }
}

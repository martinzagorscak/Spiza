package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserRepository : UserRepository {
    val currentUserProfile = MutableStateFlow<UserProfile?>(TEST_PROFILE)
    var users: List<UserProfile> = listOf(TEST_PROFILE)
    var getUsersFailure: Throwable? = null
    var signInResult: AuthResult = AuthResult.Success(TEST_USER)

    override val profile: Flow<UserProfile?> = currentUserProfile

    override val currentProfile: UserProfile? get() = currentUserProfile.value

    override suspend fun getUsers(userIds: List<String>): List<UserProfile> {
        getUsersFailure?.let { throw it }
        return users.filter { it.id in userIds }
    }

    override suspend fun signIn(provider: AuthProvider): AuthResult = signInResult

    override suspend fun signOut() {
        currentUserProfile.value = null
    }
}

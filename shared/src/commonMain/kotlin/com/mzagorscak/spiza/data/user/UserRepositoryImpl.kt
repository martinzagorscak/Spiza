package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.AuthUser
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UserRepositoryImpl(
    private val authService: AuthService,
) : UserRepository {
    override val profile: Flow<UserProfile?> = authService.authState.map { it?.toProfile() }

    override val currentProfile: UserProfile? get() = authService.currentUser?.toProfile()

    override suspend fun signIn(provider: AuthProvider): AuthResult = authService.signIn(provider)

    override suspend fun signOut() = authService.signOut()
}

private fun AuthUser.toProfile() = UserProfile(id = uid, name = displayName, email = email, photoUrl = photoUrl)

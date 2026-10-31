package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.AuthUser
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.data.user.model.UserProfileDto
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class UserRepositoryImpl(
    private val authService: AuthService,
    private val userDataSource: UserDataSource,
    private val crashLogger: CrashLogger,
) : UserRepository {
    override val profile: Flow<UserProfile?> =
        authService.authState
            .map { it?.toProfile() }
            .onEach { userProfile ->
                if (userProfile != null) {
                    userDataSource.saveUserProfile(userProfile.toDto())
                }
            }

    override val currentProfile: UserProfile? get() = authService.currentUser?.toProfile()

    override suspend fun getUsers(userIds: List<String>): List<UserProfile> = userDataSource.getUsers(userIds).map { it.toDomain() }

    override suspend fun signIn(provider: AuthProvider): AuthResult =
        authService.signIn(provider).also { result ->
            if (result is AuthResult.Failure) {
                crashLogger.recordException(
                    Throwable(
                        message = "Sign-in failed; ${result.error.message}",
                        cause = result.error,
                    ),
                )
            }
        }

    override suspend fun signOut() = authService.signOut()
}

private fun AuthUser.toProfile() = UserProfile(id = uid, name = displayName, email = email, photoUrl = photoUrl)

private fun UserProfile.toDto() = UserProfileDto(id = id, name = name, email = email, photoUrl = photoUrl)

private fun UserProfileDto.toDomain() = UserProfile(id = id, name = name, email = email, photoUrl = photoUrl)

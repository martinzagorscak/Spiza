package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Emits the signed-in user's profile, or null when signed out. */
    val profile: Flow<UserProfile?>

    /** Synchronous snapshot of the signed-in user, used to seed state before [profile] emits. */
    val currentProfile: UserProfile?

    suspend fun getUsers(userIds: List<String>): List<UserProfile>

    suspend fun signIn(provider: AuthProvider): AuthResult

    suspend fun signOut()
}

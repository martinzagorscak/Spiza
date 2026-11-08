package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Signed in as [TEST_PROFILE] by default. Calls are recorded **before** their failure applies; a successful [signIn] and
 * [signOut] are reflected in [currentUserProfile] like the auth-backed profile.
 */
class FakeUserRepository : UserRepository {
    /** The signed-in user's profile, null when signed out. */
    val currentUserProfile = MutableStateFlow<UserProfile?>(TEST_PROFILE)

    /** Every known profile; [getUsers] returns those that were asked for. */
    var users: List<UserProfile> = listOf(TEST_PROFILE)

    /** When set, [getUsers] throws it. */
    var getUsersFailure: Throwable? = null

    /** What [signIn] returns. */
    var signInResult: AuthResult = AuthResult.Success(TEST_USER)

    /** When set, [signIn] throws it instead of returning [signInResult]. */
    var signInFailure: Throwable? = null
    val signInProviders = mutableListOf<AuthProvider>()

    /** The ids of every [getUsers] call, so tests can check what was fetched. */
    val requestedUserIds = mutableListOf<List<String>>()

    /** When set, [signOut] throws it. */
    var signOutFailure: Throwable? = null

    /** [signOut] suspends forever, like a sign-out that never finishes. */
    var neverCompleteSignOut = false

    /** Number of [signOut] calls, counted before they can fail. */
    var signOutCalls = 0

    override val profile: Flow<UserProfile?> = currentUserProfile

    override val currentProfile: UserProfile? get() = currentUserProfile.value

    override suspend fun getUsers(userIds: List<String>): List<UserProfile> {
        requestedUserIds += userIds
        getUsersFailure?.let { throw it }
        return users.filter { it.id in userIds }
    }

    override suspend fun signIn(provider: AuthProvider): AuthResult {
        signInProviders += provider
        signInFailure?.let { throw it }
        return signInResult.also { result ->
            if (result is AuthResult.Success) {
                currentUserProfile.value =
                    UserProfile(
                        id = result.user.uid,
                        name = result.user.displayName,
                        email = result.user.email,
                        photoUrl = result.user.photoUrl,
                    )
            }
        }
    }

    override suspend fun signOut() {
        signOutCalls++
        signOutFailure?.let { throw it }
        if (neverCompleteSignOut) awaitCancellation()
        currentUserProfile.value = null
    }
}

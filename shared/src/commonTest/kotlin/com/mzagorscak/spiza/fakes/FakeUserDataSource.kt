package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.user.UserDataSource
import com.mzagorscak.spiza.data.user.model.UserProfileDto

/** Every save is recorded as an attempt **before** [saveFailure] applies; saved profiles are reflected in [users]. */
class FakeUserDataSource : UserDataSource {
    /** The `users/{uid}` documents. */
    var users: List<UserProfileDto> = emptyList()

    /** When set, [saveUserProfile] throws it. */
    var saveFailure: Throwable? = null

    /** When set, [getUsers] throws it. */
    var getUsersFailure: Throwable? = null

    val savedProfiles = mutableListOf<UserProfileDto>()

    override suspend fun saveUserProfile(profile: UserProfileDto) {
        savedProfiles += profile
        saveFailure?.let { throw it }
        users = users.filterNot { it.id == profile.id } + profile
    }

    override suspend fun getUsers(userIds: List<String>): List<UserProfileDto> {
        getUsersFailure?.let { throw it }
        // Like the real data sources: one fetch per id, in the requested order, missing profiles skipped.
        return userIds.distinct().mapNotNull { id -> users.firstOrNull { it.id == id } }
    }
}

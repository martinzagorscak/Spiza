package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.data.user.model.UserProfileDto

interface UserDataSource {
    suspend fun saveUserProfile(profile: UserProfileDto)

    /** Profiles of [userIds] in the same order, without duplicates; ids without a profile document are skipped. */
    suspend fun getUsers(userIds: List<String>): List<UserProfileDto>

    /** Deletes the `users/{userId}` document (succeeds when it does not exist). */
    suspend fun deleteUserProfile(userId: String)
}

object UserFields {
    const val COLLECTION_USERS = "users"
    const val NAME = "name"
    const val EMAIL = "email"
    const val PHOTO_URL = "photoUrl"
}

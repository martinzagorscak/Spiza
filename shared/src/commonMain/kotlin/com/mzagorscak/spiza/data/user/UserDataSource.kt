package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.data.user.model.UserProfileDto

interface UserDataSource {
    suspend fun saveUserProfile(profile: UserProfileDto)

    suspend fun getUsers(userIds: List<String>): List<UserProfileDto>
}

object UserFields {
    const val COLLECTION_USERS = "users"
    const val NAME = "name"
    const val EMAIL = "email"
    const val PHOTO_URL = "photoUrl"
}

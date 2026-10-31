package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.user.UserDataSource
import com.mzagorscak.spiza.data.user.model.UserProfileDto

class FakeUserDataSource : UserDataSource {
    var saveFailure: Throwable? = null
    val savedProfiles = mutableListOf<UserProfileDto>()

    override suspend fun saveUserProfile(profile: UserProfileDto) {
        saveFailure?.let { throw it }
        savedProfiles += profile
    }

    override suspend fun getUsers(userIds: List<String>): List<UserProfileDto> = emptyList()
}

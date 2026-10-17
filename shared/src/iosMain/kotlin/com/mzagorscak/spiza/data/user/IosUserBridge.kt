package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.data.user.model.UserProfileDto

interface IosUserBridge {
    fun saveUserProfile(
        profile: UserProfileDto,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun getUsers(
        userIds: List<String>,
        onComplete: (users: List<UserProfileDto>, errorMessage: String?) -> Unit,
    )
}

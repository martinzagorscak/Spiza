package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.data.BridgeError
import com.mzagorscak.spiza.data.user.model.UserProfileDto

interface IosUserBridge {
    fun saveUserProfile(
        profile: UserProfileDto,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun getUsers(
        userIds: List<String>,
        onComplete: (users: List<UserProfileDto>, error: BridgeError?) -> Unit,
    )
}

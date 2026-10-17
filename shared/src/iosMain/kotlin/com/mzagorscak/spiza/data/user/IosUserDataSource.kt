package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.data.user.model.UserProfileDto
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosUserDataSource(
    private val bridge: IosUserBridge,
) : UserDataSource {
    override suspend fun saveUserProfile(profile: UserProfileDto) {
        suspendCancellableCoroutine { continuation ->
            bridge.saveUserProfile(profile) { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }
    }

    override suspend fun getUsers(userIds: List<String>): List<UserProfileDto> =
        suspendCancellableCoroutine { continuation ->
            bridge.getUsers(userIds = userIds) { users, error ->
                if (error == null) {
                    continuation.resume(users)
                } else {
                    continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }
}

package com.mzagorscak.spiza.data.user

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mzagorscak.spiza.data.user.model.UserProfileDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class FirestoreUserDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserDataSource {
    private val users: CollectionReference get() = firestore.collection(UserFields.COLLECTION_USERS)

    override suspend fun saveUserProfile(profile: UserProfileDto) {
        val docRef = users.document(profile.id)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val data = mutableMapOf<String, Any?>()
            data[UserFields.NAME] = profile.name
            data[UserFields.EMAIL] = profile.email
            data[UserFields.PHOTO_URL] = profile.photoUrl
            docRef.set(data).await()
        }
    }

    override suspend fun getUsers(userIds: List<String>): List<UserProfileDto> {
        if (userIds.isEmpty()) return emptyList()
        // One get per id: the rules deny listing users, so a whereIn(documentId()) query would be rejected.
        return coroutineScope {
            userIds
                .distinct()
                .map { userId -> async { users.document(userId).get().await() } }
                .awaitAll()
                .filter { it.exists() }
                .map { it.toUserProfileDto() }
        }
    }

    override suspend fun deleteUserProfile(userId: String) {
        users.document(userId).delete().await()
    }
}

private fun DocumentSnapshot.toUserProfileDto(): UserProfileDto =
    UserProfileDto(
        id = id,
        name = getString(UserFields.NAME),
        email = getString(UserFields.EMAIL),
        photoUrl = getString(UserFields.PHOTO_URL),
    )

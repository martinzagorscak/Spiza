package com.mzagorscak.spiza.data.user

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mzagorscak.spiza.data.user.model.UserProfileDto
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
        return users
            .whereIn(FieldPath.documentId(), userIds)
            .get()
            .await()
            .documents
            .mapNotNull {
                it.toUserProfileDto()
            }
    }
}

private fun DocumentSnapshot.toUserProfileDto(): UserProfileDto =
    UserProfileDto(
        id = id,
        name = getString(UserFields.NAME),
        email = getString(UserFields.EMAIL),
        photoUrl = getString(UserFields.PHOTO_URL),
    )

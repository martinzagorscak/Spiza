package com.mzagorscak.spiza.data.household

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreHouseholdDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : HouseholdDataSource {
    private val households: CollectionReference get() = firestore.collection(HouseholdFields.COLLECTION)

    private fun items(householdId: String) = households.document(householdId).collection(HouseholdFields.ITEMS)

    override fun observeHouseholds(userId: String): Flow<List<HouseholdDto>> =
        callbackFlow {
            val registration =
                households
                    .whereArrayContains(HouseholdFields.JOINED_MEMBERS, userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                        } else if (snapshot != null) {
                            trySend(snapshot.documents.mapNotNull { it.toHouseholdDto() })
                        }
                    }
            awaitClose { registration.remove() }
        }

    override fun observeItems(householdId: String): Flow<List<ArticleItemDto>> =
        callbackFlow {
            val registration =
                items(householdId).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                    } else if (snapshot != null) {
                        trySend(snapshot.documents.mapNotNull { it.toArticleItemDto() })
                    }
                }
            awaitClose { registration.remove() }
        }

    override suspend fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
    ): String {
        val document = households.document()
        document
            .set(
                mapOf(
                    HouseholdFields.NAME to name,
                    HouseholdFields.DESCRIPTION to description,
                    HouseholdFields.OWNER_ID to ownerId,
                    HouseholdFields.JOINED_MEMBERS to listOf(ownerId),
                    HouseholdFields.PENDING_MEMBERS to emptyList<String>(),
                ),
            ).await()
        return document.id
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
    ) {
        items(householdId)
            .document()
            .set(
                mapOf(
                    HouseholdFields.TEXT to text,
                    HouseholdFields.IS_CHECKED to false,
                    HouseholdFields.POSITION to position,
                ),
            ).await()
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) {
        items(householdId).document(itemId).update(HouseholdFields.TEXT, text).await()
    }

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) {
        items(householdId).document(itemId).update(HouseholdFields.IS_CHECKED, isChecked).await()
    }

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) {
        items(householdId).document(itemId).update(HouseholdFields.POSITION, position).await()
    }

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) {
        items(householdId).document(itemId).delete().await()
    }

    override suspend fun clearCheckedItems(householdId: String) {
        items(householdId)
            .whereEqualTo(HouseholdFields.IS_CHECKED, true)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete() }
    }
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.stringList(field: String): List<String> = (get(field) as? List<String>).orEmpty()

private fun DocumentSnapshot.toHouseholdDto(): HouseholdDto? {
    val name = getString(HouseholdFields.NAME) ?: return null
    return HouseholdDto(
        id = id,
        name = name,
        description = getString(HouseholdFields.DESCRIPTION).orEmpty(),
        ownerId = getString(HouseholdFields.OWNER_ID).orEmpty(),
        joinedMembers = stringList(HouseholdFields.JOINED_MEMBERS),
        pendingMembers = stringList(HouseholdFields.PENDING_MEMBERS),
    )
}

private fun DocumentSnapshot.toArticleItemDto(): ArticleItemDto? {
    val position = getDouble(HouseholdFields.POSITION) ?: return null
    return ArticleItemDto(
        id = id,
        text = getString(HouseholdFields.TEXT).orEmpty(),
        isChecked = getBoolean(HouseholdFields.IS_CHECKED) ?: false,
        position = position,
    )
}

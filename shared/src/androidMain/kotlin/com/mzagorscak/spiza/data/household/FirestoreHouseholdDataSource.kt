package com.mzagorscak.spiza.data.household

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldValue.arrayUnion
import com.google.firebase.firestore.FirebaseFirestore
import com.mzagorscak.spiza.data.household.HouseholdFields.JOIN_KEY
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreHouseholdDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : HouseholdDataSource {
    private val households: CollectionReference get() = firestore.collection(HouseholdFields.COLLECTION_HOUSEHOLDS)
    private val householdJoinKeys: CollectionReference get() = firestore.collection(HouseholdFields.COLLECTION_JOIN_KEYS)

    private fun items(householdId: String) = households.document(householdId).collection(HouseholdFields.ITEMS)

    override suspend fun createHouseholdJoinKey(userId: String): String {
        val generatedKey = generateJoinKey()
        householdJoinKeys
            .document(userId)
            .set(
                mapOf(JOIN_KEY to generatedKey),
            ).await()
        return generatedKey
    }

    private fun generateJoinKey(): String = UUID.randomUUID().toString()

    override suspend fun deleteHouseholdJoinKey(userId: String) {
        householdJoinKeys.document(userId).delete().await()
    }

    override fun getHouseholdJoinKey(userId: String): Flow<String?> =
        flow {
            val joinKey = householdJoinKeys
                .document(userId)
                .get()
                .await()
                .getString(JOIN_KEY)
            emit(joinKey)
        }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): Boolean {
        val joinKeySnapshot = householdJoinKeys.whereEqualTo(JOIN_KEY, userJoinKey).get().await()
        if (joinKeySnapshot.isEmpty) {
            return false
        }

        val userId = joinKeySnapshot.documents.first().id

        // Adding the member and consuming their join key is one atomic write, so it never half-succeeds.
        firestore
            .batch()
            .update(households.document(householdId), HouseholdFields.JOINED_MEMBERS, arrayUnion(userId))
            .delete(householdJoinKeys.document(userId))
            .commit()
            .await()
        return true
    }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) {
        households
            .document(householdId)
            .update(
                HouseholdFields.JOINED_MEMBERS,
                FieldValue.arrayRemove(memberId),
            ).await()
    }

    // Firestore does not delete subcollections, so the items go in the same batch as the household
    // (a batch allows 500 writes, far above ITEMS_PER_HOUSEHOLD_CONSTRAINT).
    override suspend fun deleteHousehold(householdId: String) {
        val batch = firestore.batch()
        items(householdId)
            .get()
            .await()
            .documents
            .forEach { batch.delete(it.reference) }
        batch.delete(households.document(householdId)).commit().await()
    }

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
                ),
            ).await()
        return document.id
    }

    override suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    ) {
        households
            .document(householdId)
            .update(
                mapOf(
                    HouseholdFields.NAME to name,
                    HouseholdFields.DESCRIPTION to description,
                ),
            ).await()
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
        val checkedItems =
            items(householdId)
                .whereEqualTo(HouseholdFields.IS_CHECKED, true)
                .get()
                .await()
                .documents
        if (checkedItems.isEmpty()) return
        val batch = firestore.batch()
        checkedItems.forEach { batch.delete(it.reference) }
        batch.commit().await()
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

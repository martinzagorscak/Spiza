package com.mzagorscak.spiza.data.account

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.household.HouseholdFields
import com.mzagorscak.spiza.data.user.UserFields
import kotlinx.coroutines.tasks.await

class FirestoreAccountDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AccountDataSource {
    // One batch, so a rejected write (e.g. by the rules) leaves the account untouched instead of half-deleted. It stays far below
    // the 500 writes limit: at most 3 households per user (ConstraintsManager), each owned one with at most 100 items, plus the
    // join key and the profile (~310 writes). Above the limit Firestore rejects the whole batch, which still deletes nothing.
    // Server reads only: the batch must act on the current memberships and items, never on a stale cache.
    override suspend fun deleteAccountData(userId: String) {
        val joinKeys =
            firestore
                .collection(HouseholdFields.COLLECTION_JOIN_KEYS)
                .whereEqualTo(HouseholdFields.USER_ID, userId)
                .get(Source.SERVER)
                .await()
                .documents
        val households =
            firestore
                .collection(HouseholdFields.COLLECTION_HOUSEHOLDS)
                .whereArrayContains(HouseholdFields.JOINED_MEMBERS, userId)
                .get(Source.SERVER)
                .await()
                .documents

        val ownedHouseholds = mutableListOf<DocumentReference>()
        val joinedHouseholds = mutableListOf<DocumentReference>()
        households.forEach { household ->
            // Unlike the listeners, a malformed household is not skipped: it would stay behind with the user in it.
            val ownerId =
                household.get(HouseholdFields.OWNER_ID) as? String
                    ?: throw DataException(
                        reason = DataException.Reason.Unknown,
                        message = "Household ${household.id} has no readable owner",
                    )
            if (ownerId == userId) ownedHouseholds += household.reference else joinedHouseholds += household.reference
        }
        // Firestore does not delete subcollections, so the items of owned households go in the same batch.
        val items =
            ownedHouseholds.flatMap { household ->
                household
                    .collection(HouseholdFields.ITEMS)
                    .get(Source.SERVER)
                    .await()
                    .documents
                    .map { it.reference }
            }

        val batch = firestore.batch()
        joinKeys.forEach { batch.delete(it.reference) }
        items.forEach { batch.delete(it) }
        // Also for the other members: a household can not exist without its owner.
        ownedHouseholds.forEach { batch.delete(it) }
        joinedHouseholds.forEach { batch.update(it, HouseholdFields.JOINED_MEMBERS, FieldValue.arrayRemove(userId)) }
        batch.delete(firestore.collection(UserFields.COLLECTION_USERS).document(userId))
        batch.commit().await()
    }
}

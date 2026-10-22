package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosHouseholdDataSource(
    private val bridge: IosHouseholdBridge,
) : HouseholdDataSource {
    override suspend fun createHouseholdJoinKey(userId: String): String =
        suspendCancellableCoroutine { continuation ->
            bridge.createHouseholdJoinKey(userId) { key, error ->
                if (key != null) {
                    continuation.resume(key)
                } else {
                    continuation.resumeWithException(IllegalStateException(error ?: "Creating join key failed"))
                }
            }
        }

    override fun getHouseholdJoinKey(userId: String): Flow<String?> =
        flow {
            val key =
                suspendCancellableCoroutine<String?> { continuation ->
                    bridge.getHouseholdJoinKey(userId) { key, error ->
                        if (error == null) {
                            continuation.resume(key)
                        } else {
                            continuation.resumeWithException(IllegalStateException(error))
                        }
                    }
                }
            emit(key)
        }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): Boolean =
        suspendCancellableCoroutine { continuation ->
            bridge.addMemberToHousehold(householdId, userJoinKey) { isUserFound, error ->
                if (error == null) {
                    continuation.resume(isUserFound)
                } else {
                    continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) = write { bridge.removeMemberFromHousehold(householdId, memberId, it) }

    override suspend fun deleteHousehold(householdId: String) = write { bridge.deleteHousehold(householdId, it) }

    override fun observeHouseholds(userId: String): Flow<List<HouseholdDto>> =
        callbackFlow {
            val handle =
                bridge.observeHouseholds(
                    userId = userId,
                    onChange = { trySend(it) },
                    onError = { close(IllegalStateException(it)) },
                )
            awaitClose { handle.remove() }
        }

    override fun observeItems(householdId: String): Flow<List<ArticleItemDto>> =
        callbackFlow {
            val handle =
                bridge.observeItems(
                    householdId = householdId,
                    onChange = { trySend(it) },
                    onError = { close(IllegalStateException(it)) },
                )
            awaitClose { handle.remove() }
        }

    override suspend fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
    ): String =
        suspendCancellableCoroutine { continuation ->
            bridge.createHousehold(ownerId, name, description) { id, error ->
                if (id != null) {
                    continuation.resume(id)
                } else {
                    continuation.resumeWithException(IllegalStateException(error ?: "Creating household failed"))
                }
            }
        }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
    ) = write { bridge.addItem(householdId, text, position, it) }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) = write { bridge.updateItemText(householdId, itemId, text, it) }

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) = write { bridge.setItemChecked(householdId, itemId, isChecked, it) }

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) = write { bridge.moveItem(householdId, itemId, position, it) }

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) = write { bridge.deleteItem(householdId, itemId, it) }

    override suspend fun clearCheckedItems(householdId: String) = write { bridge.clearCheckedItems(householdId, it) }

    private suspend fun write(block: (onComplete: (String?) -> Unit) -> Unit) {
        suspendCancellableCoroutine { continuation ->
            block { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }
    }
}

package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.BridgeError
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import com.mzagorscak.spiza.data.toDataException
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
                    continuation.resumeWithException(error.orUnknown("Creating join key failed"))
                }
            }
        }

    override suspend fun deleteHouseholdJoinKey(userId: String) =
        suspendCancellableCoroutine { continuation ->
            bridge.deleteHouseholdJoinKey(userId) { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(error.toDataException())
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
                            continuation.resumeWithException(error.toDataException())
                        }
                    }
                }
            emit(key)
        }

    override suspend fun getJoinKeyUserId(joinKey: String): String? =
        suspendCancellableCoroutine { continuation ->
            bridge.getJoinKeyUserId(joinKey) { userId, error ->
                if (error == null) {
                    continuation.resume(userId)
                } else {
                    continuation.resumeWithException(error.toDataException())
                }
            }
        }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userId: String,
        joinKey: String,
    ) = write { bridge.addMemberToHousehold(householdId, userId, joinKey, it) }

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
                    onError = { close(it.toDataException()) },
                )
            awaitClose { handle.remove() }
        }

    override fun observeItems(householdId: String): Flow<List<ArticleItemDto>> =
        callbackFlow {
            val handle =
                bridge.observeItems(
                    householdId = householdId,
                    onChange = { trySend(it) },
                    onError = { close(it.toDataException()) },
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
                    continuation.resumeWithException(error.orUnknown("Creating household failed"))
                }
            }
        }

    override suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    ) = write { bridge.updateHousehold(householdId, name, description, it) }

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

    private suspend fun write(block: (onComplete: (BridgeError?) -> Unit) -> Unit) {
        suspendCancellableCoroutine { continuation ->
            block { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(error.toDataException())
                }
            }
        }
    }
}

private fun BridgeError?.orUnknown(message: String): DataException =
    this?.toDataException() ?: DataException(reason = DataException.Reason.Unknown, message = message)

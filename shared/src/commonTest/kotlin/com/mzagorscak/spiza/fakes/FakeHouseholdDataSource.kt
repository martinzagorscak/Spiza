package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * In-memory Firestore. Every write is recorded as an attempt **before** [writeFailure] / [neverAcknowledgeWrites] apply;
 * successful writes are reflected in [households], [items] and [joinKeys] like the real collections.
 */
class FakeHouseholdDataSource : HouseholdDataSource {
    /** Every household document; [observeHouseholds] emits those the user has joined, like the `array-contains` query. */
    val households = MutableStateFlow<List<HouseholdDto>>(emptyList())

    /** Items of every household (the fake ignores the household id), in stored order: Firestore does not sort them. */
    val items = MutableStateFlow<List<ArticleItemDto>>(emptyList())

    /** `joinKeys/{joinKey}` -> userId. A user has at most one key. */
    val joinKeys = mutableMapOf<String, String>()

    /** When set, [observeHouseholds] fails with it on every subscription (like a rejected Firestore listener). */
    var householdsFailure: Throwable? = null

    /** When set, [getHouseholds] throws it (like a server read while offline). */
    var getHouseholdsFailure: Throwable? = null

    /** When set, [observeItems] fails with it on every subscription. */
    var itemsFailure: Throwable? = null

    /** When set, [getHouseholdJoinKey] fails with it on every subscription. */
    var joinKeyFailure: Throwable? = null

    /** When set, every write throws it (like a write rejected by the rules). */
    var writeFailure: Throwable? = null

    /** Writes never complete, like Firestore writes while offline (they wait for a server acknowledgement). */
    var neverAcknowledgeWrites = false

    /** How many times each listener was subscribed to (every retry subscribes again). */
    var householdsSubscriptions = 0
    var itemsSubscriptions = 0

    /** The user id of every [getHouseholdJoinKey] subscription (every refresh reads the key again). */
    val joinKeyRequestsOfUsers = mutableListOf<String>()
    val createdJoinKeysOfUsers = mutableListOf<String>()
    val deletedJoinKeysOfUsers = mutableListOf<String>()

    /** Every join key passed to [getJoinKeyUserId]. */
    val joinKeyLookups = mutableListOf<String>()

    data class AddedMember(
        val householdId: String,
        val userId: String,
        val joinKey: String,
    )

    val addedMembers = mutableListOf<AddedMember>()

    data class CreatedHousehold(
        val ownerId: String,
        val name: String,
        val description: String,
    )

    val createdHouseholds = mutableListOf<CreatedHousehold>()

    data class UpdatedHousehold(
        val householdId: String,
        val name: String,
        val description: String,
    )

    val updatedHouseholds = mutableListOf<UpdatedHousehold>()

    data class AddedItem(
        val householdId: String,
        val text: String,
        val position: Double,
    )

    val addedItems = mutableListOf<AddedItem>()

    val clearedHouseholds = mutableListOf<String>()

    private var nextJoinKey = 0
    private var nextItemId = 0
    private var nextHouseholdId = 0

    private suspend fun write() {
        writeFailure?.let { throw it }
        if (neverAcknowledgeWrites) awaitCancellation()
    }

    private fun updateHouseholdDto(
        householdId: String,
        change: (HouseholdDto) -> HouseholdDto,
    ) {
        households.value = households.value.map { if (it.id == householdId) change(it) else it }
    }

    private fun updateItem(
        itemId: String,
        change: (ArticleItemDto) -> ArticleItemDto,
    ) {
        items.value = items.value.map { if (it.id == itemId) change(it) else it }
    }

    override suspend fun createHouseholdJoinKey(userId: String): String {
        createdJoinKeysOfUsers += userId
        write()
        val joinKey = "join-key-${++nextJoinKey}"
        joinKeys.values.removeAll { it == userId }
        joinKeys[joinKey] = userId
        return joinKey
    }

    override suspend fun deleteHouseholdJoinKey(userId: String) {
        deletedJoinKeysOfUsers += userId
        write()
        joinKeys.values.removeAll { it == userId }
    }

    // A one-shot read like the real `get()`: a changed key is only seen by the next subscription.
    override fun getHouseholdJoinKey(userId: String): Flow<String?> =
        flow {
            joinKeyRequestsOfUsers += userId
            joinKeyFailure?.let { throw it }
            emit(joinKeys.entries.firstOrNull { it.value == userId }?.key)
        }

    override suspend fun getJoinKeyUserId(joinKey: String): String? {
        joinKeyLookups += joinKey
        return joinKeys[joinKey]
    }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userId: String,
        joinKey: String,
    ) {
        addedMembers += AddedMember(householdId = householdId, userId = userId, joinKey = joinKey)
        write()
        updateHouseholdDto(householdId) { it.copy(joinedMembers = (it.joinedMembers + userId).distinct()) }
        joinKeys.remove(joinKey)
    }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) {
        write()
        updateHouseholdDto(householdId) { it.copy(joinedMembers = it.joinedMembers - memberId) }
    }

    override suspend fun deleteHousehold(householdId: String) {
        write()
        households.value = households.value.filterNot { it.id == householdId }
    }

    override fun observeHouseholds(userId: String): Flow<List<HouseholdDto>> =
        flow {
            householdsSubscriptions++
            householdsFailure?.let { throw it }
            emitAll(households.map { all -> all.filter { userId in it.joinedMembers } })
        }

    override suspend fun getHouseholds(userId: String): List<HouseholdDto> {
        getHouseholdsFailure?.let { throw it }
        return households.value.filter { userId in it.joinedMembers }
    }

    override fun observeItems(householdId: String): Flow<List<ArticleItemDto>> =
        flow {
            itemsSubscriptions++
            itemsFailure?.let { throw it }
            emitAll(items)
        }

    override suspend fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
    ): String {
        createdHouseholds += CreatedHousehold(ownerId = ownerId, name = name, description = description)
        write()
        val id = "household-new-${++nextHouseholdId}"
        households.value +=
            HouseholdDto(id = id, name = name, description = description, ownerId = ownerId, joinedMembers = listOf(ownerId))
        return id
    }

    override suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    ) {
        updatedHouseholds += UpdatedHousehold(householdId = householdId, name = name, description = description)
        write()
        updateHouseholdDto(householdId) { it.copy(name = name, description = description) }
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
    ) {
        addedItems += AddedItem(householdId = householdId, text = text, position = position)
        write()
        items.value += ArticleItemDto(id = "item-new-${++nextItemId}", text = text, isChecked = false, position = position)
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) {
        write()
        updateItem(itemId) { it.copy(text = text) }
    }

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) {
        write()
        updateItem(itemId) { it.copy(isChecked = isChecked) }
    }

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) {
        write()
        updateItem(itemId) { it.copy(position = position) }
    }

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) {
        write()
        items.value = items.value.filterNot { it.id == itemId }
    }

    override suspend fun clearCheckedItems(householdId: String) {
        clearedHouseholds += householdId
        write()
        items.value = items.value.filterNot { it.isChecked }
    }
}

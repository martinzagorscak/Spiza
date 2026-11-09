package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.AddMemberResult
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.CreateHouseholdResult
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Every write is recorded as an attempt **before** [writeFailure] / [neverCompleteWrites] apply, so tests can assert
 * both the call and its failure. Successful writes are reflected in [households], [items] and [joinKey] like the real store.
 */
class FakeHouseholdRepository : HouseholdRepository {
    /** The signed-in user's households; written households are kept sorted by name like the real repository emits them. */
    val households = MutableStateFlow(listOf(household()))

    /** Items of every household (the fake ignores the household id when observing). */
    val items = MutableStateFlow<List<ArticleItem>>(emptyList())
    val joinKey = MutableStateFlow<String?>("join-key")

    /** When set, [observeHouseholds] fails with it (checked when the stream is collected and on every emission, like a listener error). */
    var householdsFailure: Throwable? = null

    /** When set, [observeItems] fails with it (checked each time the stream is collected). */
    var itemsFailure: Throwable? = null

    /** When set, every write throws it. */
    var writeFailure: Throwable? = null

    /**
     * What [addMemberToHousehold] returns. The members of [households] are not changed: the fake can not know whose key it was.
     */
    var addMemberResult: AddMemberResult = AddMemberResult.Added

    /**
     * Writes never complete, like a write that is still waiting for the server (a hanging call).
     * Do not use it to model an offline [updateHousehold] / [createHousehold]: the real repository gives up after a timeout
     * (update returns normally with a warning, create returns `Created(householdId = null)`), which the repository tests cover.
     */
    var neverCompleteWrites = false

    /**
     * When set, [createHousehold] records the attempt and returns it without creating anything, like the real repository
     * when the user is not signed in or reached the household limit. Null creates the household and returns [CreateHouseholdResult.Created].
     */
    var createHouseholdResult: CreateHouseholdResult? = null

    data class AddedItem(
        val text: String,
        val position: Double,
        val currentItemsCount: Int,
    )

    val addedItems = mutableListOf<AddedItem>()

    /** itemId to the new value of every item write. */
    val updatedTexts = mutableListOf<Pair<String, String>>()
    val checkedChanges = mutableListOf<Pair<String, Boolean>>()
    val movedItems = mutableListOf<Pair<String, Double>>()
    val deletedItems = mutableListOf<String>()

    /** The household id of every item write, in call order. */
    val itemWriteHouseholdIds = mutableListOf<String>()
    val clearedHouseholds = mutableListOf<String>()

    /** The household id of every [observeItems] collection (one per started items listener), in call order. */
    val observedItemsHouseholdIds = mutableListOf<String>()

    /** How many times [observeHouseholds] was collected (one per started households listener). */
    var observeHouseholdsCollections = 0

    /** The member id of every [removeMemberFromHousehold] call. */
    val removedMembers = mutableListOf<String>()
    val deletedHouseholds = mutableListOf<String>()

    /** householdId to the join key exactly as it was passed in. */
    val addedMembers = mutableListOf<Pair<String, String>>()

    /** name to description of every [createHousehold] call. */
    val createdHouseholds = mutableListOf<Pair<String, String>>()

    data class UpdatedHousehold(
        val householdId: String,
        val name: String,
        val description: String,
    )

    val updatedHouseholds = mutableListOf<UpdatedHousehold>()

    /** Number of [createHouseholdJoinKey] and [refreshHouseholdJoinKey] calls (each counted before it can fail). */
    var createJoinKeyCalls = 0
    var refreshJoinKeyCalls = 0

    private var joinKeyVersion = 0
    private var nextItemId = 0
    private var nextHouseholdId = 0

    private suspend fun write() {
        writeFailure?.let { throw it }
        if (neverCompleteWrites) awaitCancellation()
    }

    private fun updateItem(
        itemId: String,
        change: (ArticleItem) -> ArticleItem,
    ) {
        items.value = items.value.map { if (it.id == itemId) change(it) else it }.sortedBy { it.position }
    }

    private fun updateHouseholds(change: (List<Household>) -> List<Household>) {
        households.value = change(households.value).sortedBy { it.name.lowercase() }
    }

    override suspend fun createHouseholdJoinKey(): String? {
        createJoinKeyCalls++
        write()
        return "join-key-${++joinKeyVersion}".also { joinKey.value = it }
    }

    override fun getHouseholdJoinKey(): Flow<String?> = joinKey

    override suspend fun refreshHouseholdJoinKey() {
        refreshJoinKeyCalls++
        write()
        joinKey.value = "join-key-${++joinKeyVersion}"
    }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): AddMemberResult {
        addedMembers += householdId to userJoinKey
        write()
        return addMemberResult
    }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) {
        removedMembers += memberId
        write()
        // The signed-in user's households come from an `array-contains` listener, so a household they left disappears.
        if (memberId == TEST_USER.uid) {
            updateHouseholds { all -> all.filterNot { it.id == householdId } }
            return
        }
        updateHouseholds { all ->
            all.map { household ->
                if (household.id != householdId) return@map household
                household.copy(members = household.members.filterNot { it.id == memberId })
            }
        }
    }

    override suspend fun deleteHousehold(householdId: String) {
        deletedHouseholds += householdId
        write()
        updateHouseholds { all -> all.filterNot { it.id == householdId } }
    }

    override fun observeHouseholds(): Flow<List<Household>> =
        flow {
            observeHouseholdsCollections++
            householdsFailure?.let { throw it }
            emitAll(households.onEach { householdsFailure?.let { throw it } })
        }

    override fun getCachedHouseholdCount(): Flow<Int> = households.map { it.size }

    override fun observeItems(householdId: String): Flow<List<ArticleItem>> =
        flow {
            observedItemsHouseholdIds += householdId
            itemsFailure?.let { throw it }
            emitAll(items)
        }

    override suspend fun createHousehold(
        name: String,
        description: String,
    ): CreateHouseholdResult {
        createdHouseholds += name to description
        write()
        createHouseholdResult?.let { return it }
        val id = "household-new-${++nextHouseholdId}"
        // Created by the signed-in test user, who becomes its owner and only member.
        updateHouseholds { all -> all + household(id = id, name = name).copy(description = description) }
        return CreateHouseholdResult.Created(householdId = id)
    }

    override suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    ) {
        updatedHouseholds += UpdatedHousehold(householdId = householdId, name = name, description = description)
        write()
        updateHouseholds { all ->
            all.map { if (it.id == householdId) it.copy(name = name, description = description) else it }
        }
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
        currentItemsCount: Int,
    ) {
        addedItems += AddedItem(text, position, currentItemsCount)
        itemWriteHouseholdIds += householdId
        write()
        val added = ArticleItem(id = "item-new-${++nextItemId}", text = text, isChecked = false, position = position)
        items.value = (items.value + added).sortedBy { it.position }
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) {
        updatedTexts += itemId to text
        itemWriteHouseholdIds += householdId
        write()
        updateItem(itemId) { it.copy(text = text) }
    }

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) {
        checkedChanges += itemId to isChecked
        itemWriteHouseholdIds += householdId
        write()
        updateItem(itemId) { it.copy(isChecked = isChecked) }
    }

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) {
        movedItems += itemId to position
        itemWriteHouseholdIds += householdId
        write()
        updateItem(itemId) { it.copy(position = position) }
    }

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) {
        deletedItems += itemId
        itemWriteHouseholdIds += householdId
        write()
        items.value = items.value.filterNot { it.id == itemId }
    }

    override suspend fun clearCheckedItems(householdId: String) {
        itemWriteHouseholdIds += householdId
        clearedHouseholds += householdId
        write()
        items.value = items.value.filterNot { it.isChecked }
    }
}

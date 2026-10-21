package com.mzagorscak.spiza.data.household

import co.touchlab.kermit.Logger
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

// Firestore completes writes only once the server acknowledged them, which never happens while offline.
private const val LOG_TAG = "Spiza.Household"
private const val CREATE_TIMEOUT_MS = 10_000L
private const val ITEMS_RETRY_COUNT = 3
private const val ITEMS_RETRY_DELAY_MS = 1_000L

internal class HouseholdRepositoryImpl(
    private val dataSource: HouseholdDataSource,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val constraintsManager: ConstraintsManager,
) : HouseholdRepository {
    private val newlyCreatedHouseholdJoinKeyPublisher = MutableSharedFlow<String>()
    private val cachedUserProfiles = MutableStateFlow(emptyMap<String, UserProfile>()) // <userId, UserProfile>
    private val cachedHouseholds = MutableStateFlow(emptyMap<String, HouseholdDto>()) // <householdId, memberCount>

    override suspend fun createHouseholdJoinKey(): String? {
        val userId = authService.currentUser?.uid ?: error("No signed-in user")
        // Constraint verification
        if (constraintsManager.canUserCreateOrJoinHousehold(householdCount = cachedHouseholds.value.count()).not()) {
            return null
        }

        val joinKey = dataSource.createHouseholdJoinKey(userId)
        newlyCreatedHouseholdJoinKeyPublisher.emit(joinKey)
        return joinKey
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getHouseholdJoinKey(): Flow<String?> =
        authService.authState.flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                merge(
                    dataSource.getHouseholdJoinKey(user.uid),
                    newlyCreatedHouseholdJoinKeyPublisher,
                )
            }
        }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ) {
        // Constraint verification
        cachedHouseholds.value[householdId]?.let { household ->
            if (constraintsManager.canAddMemberToHousehold(memberCount = household.joinedMembers.count())) return
        }

        val userId = authService.currentUser?.uid ?: error("No signed-in user")
        Logger.i(tag = LOG_TAG) { "Adding member $userId to household $householdId with join key $userJoinKey" }
        dataSource.addMemberToHousehold(householdId, userJoinKey)
    }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) {
        val userId = authService.currentUser?.uid ?: error("No signed-in user")
        Logger.i(tag = LOG_TAG) { "User $userId removing member $memberId from household $householdId" }
        dataSource.removeMemberFromHousehold(householdId, memberId)
    }

    override suspend fun deleteHousehold(householdId: String) {
        val userId = authService.currentUser?.uid ?: error("No signed-in user")
        Logger.i(tag = LOG_TAG) { "User $userId deleting household $householdId" }
        dataSource.deleteHousehold(householdId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHouseholds(): Flow<List<Household>> =
        authService.authState.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                dataSource
                    .observeHouseholds(user.uid)
                    .map { households ->
                        cachedHouseholds.update { households.associateBy { it.id } }
                        val memberIdsToFetch = households.flatMap { it.joinedMembers }.distinct().toSet() - cachedUserProfiles.value.keys
                        val freshUsers = userRepository.getUsers(memberIdsToFetch.toList())
                        val allProfiles = cachedUserProfiles.value + freshUsers.associateBy { it.id }
                        val households = households.map { dto -> dto.toDomain(allProfiles) }.sortedBy { it.name.lowercase() }
                        cachedUserProfiles.update { allProfiles }
                        households
                    }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCachedHouseholdCount(): Flow<Int> = cachedHouseholds.mapLatest { it.count() }

    override fun observeItems(householdId: String): Flow<List<ArticleItem>> =
        dataSource
            .observeItems(householdId)
            .map { items -> items.map { it.toDomain() }.sortedBy { it.position } }
            .onEach { Logger.d(tag = LOG_TAG) { "Items update for $householdId: ${it.size} items" } }
            // A freshly created household is visible locally before the server has committed it, so the first
            // items listener can be rejected by the rules (they read the parent document). Retrying resolves it.
            .retryWhen { cause, attempt ->
                Logger.w(throwable = cause, tag = LOG_TAG) { "Items listener of $householdId failed (attempt $attempt)" }
                (attempt < ITEMS_RETRY_COUNT).also { if (it) delay(ITEMS_RETRY_DELAY_MS.milliseconds) }
            }

    override suspend fun createHousehold(
        name: String,
        description: String,
    ): String? {
        // Constraint verification
        if (constraintsManager.canUserCreateOrJoinHousehold(householdCount = cachedHouseholds.value.count()).not()) {
            return null
        }

        val ownerId = authService.currentUser?.uid ?: error("No signed-in user")
        val id = withTimeoutOrNull(CREATE_TIMEOUT_MS.milliseconds) { dataSource.createHousehold(ownerId, name, description) }
        return id
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
        currentItemsCount: Int,
    ) {
        // Constraint verification
        if (constraintsManager.canAddItemToHousehold(itemCount = currentItemsCount).not()) return

        dataSource.addItem(householdId, text, position)
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) = dataSource.updateItemText(householdId, itemId, text)

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) = dataSource.setItemChecked(householdId, itemId, isChecked)

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) = dataSource.moveItem(householdId, itemId, position)

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) = dataSource.deleteItem(householdId, itemId)

    override suspend fun clearCheckedItems(householdId: String) = dataSource.clearCheckedItems(householdId)
}

private fun HouseholdDto.toDomain(profileMap: Map<String, UserProfile>): Household =
    Household(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        members = joinedMembers
            .filter { profileMap.containsKey(it) }
            .mapNotNull { memberId -> profileMap[memberId] },
    )

private fun ArticleItemDto.toDomain() = ArticleItem(id = id, text = text, isChecked = isChecked, position = position)

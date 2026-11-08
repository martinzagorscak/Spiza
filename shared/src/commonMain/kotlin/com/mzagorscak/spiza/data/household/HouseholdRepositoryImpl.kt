package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.ExpectedErrors
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.crash.isQuotaExceeded
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.CreateHouseholdResult
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

// Firestore completes writes only once the server acknowledged them, which never happens while offline.
private const val CREATE_TIMEOUT_MS = 10_000L
private const val UPDATE_TIMEOUT_MS = 10_000L
private const val ITEMS_RETRY_COUNT = 3
private const val OBSERVATION_RETRY_DELAY_MS = 1_000L

// Join keys are generated as lowercase UUIDs and used as document ids, so anything else can not be a valid key
// (and an invalid path such as "" or "a/b" would make Firestore throw, or crash on iOS).
private val JOIN_KEY_PATTERN = Regex("^[a-z0-9-]{1,64}$")

internal class HouseholdRepositoryImpl(
    private val dataSource: HouseholdDataSource,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val constraintsManager: ConstraintsManager,
    private val crashLogger: CrashLogger,
) : HouseholdRepository {
    private val refreshHouseholdJoinKeyPublisher = MutableSharedFlow<Unit>()
    private val newlyCreatedHouseholdJoinKeyPublisher = MutableSharedFlow<String>()
    private val cachedUserProfiles = MutableStateFlow(emptyMap<String, UserProfile>()) // <userId, UserProfile>
    private val cachedHouseholds = MutableStateFlow(emptyMap<String, HouseholdDto>()) // <householdId, memberCount>

    override suspend fun createHouseholdJoinKey(): String? {
        val userId = authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to create a household join key while not signed in").let { return null }

        // Constraint verification
        if (constraintsManager.canUserCreateOrJoinHousehold(householdCount = cachedHouseholds.value.count()).not()) {
            crashLogger.logWarning(
                message = "User $userId tried to create a household join key but has already joined the maximum number of households",
            )
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
                    refreshHouseholdJoinKeyPublisher
                        .onStart { emit(Unit) }
                        .flatMapLatest {
                            dataSource
                                .getHouseholdJoinKey(user.uid)
                                .catchAndRecord(crashLogger = crashLogger, context = "Fetching household join key failed") {
                                    emit(null)
                                }
                        },
                    newlyCreatedHouseholdJoinKeyPublisher,
                )
            }
        }

    override suspend fun refreshHouseholdJoinKey() {
        authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to refresh a household join key while not signed in").let { return }

        refreshHouseholdJoinKeyPublisher.emit(Unit)
    }

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): Boolean {
        authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to add a member to household $householdId")
                .let { return false }

        // Constraint verification
        cachedHouseholds.value[householdId]?.let { household ->
            if (constraintsManager.canAddMemberToHousehold(memberCount = household.joinedMembers.count()).not()) {
                crashLogger.logWarning(
                    message = "User tried to add member to household but the household has already reached the max number of members",
                )
                return false
            }
        }

        val joinKey = userJoinKey.trim().lowercase()
        if (JOIN_KEY_PATTERN.matches(joinKey).not()) return false

        return dataSource.addMemberToHousehold(householdId = householdId, userJoinKey = joinKey)
    }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) {
        authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to remove member $memberId from household with id $householdId")
                .let { return }

        dataSource.removeMemberFromHousehold(householdId = householdId, memberId = memberId)
    }

    override suspend fun deleteHousehold(householdId: String) {
        authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to delete household $householdId")
                .let { return }

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
                        val freshUsers = fetchUsers(memberIdsToFetch.toList())
                        val allProfiles = cachedUserProfiles.value + freshUsers.associateBy { it.id }
                        val households = households.map { dto -> dto.toDomain(allProfiles) }.sortedBy { it.name.lowercase() }
                        cachedUserProfiles.update { allProfiles }
                        households
                    }.retryWhen { cause, attempt ->
                        crashLogger.logWarning(
                            message = "Households listener of user ${user.uid} failed (attempt $attempt): ${cause.message}",
                        )
                        shouldRetryObservation(cause = cause, attempt = attempt)
                    }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCachedHouseholdCount(): Flow<Int> = cachedHouseholds.mapLatest { it.count() }

    override fun observeItems(householdId: String): Flow<List<ArticleItem>> =
        dataSource
            .observeItems(householdId)
            .map { items -> items.map { it.toDomain() }.sortedBy { it.position } }
            // A freshly created household is visible locally before the server has committed it, so the first
            // items listener can be rejected by the rules (they read the parent document). Retrying resolves it.
            .retryWhen { cause, attempt ->
                crashLogger.logWarning(message = "Items listener of household $householdId failed (attempt $attempt): ${cause.message}")
                shouldRetryObservation(cause = cause, attempt = attempt)
            }

    override suspend fun createHousehold(
        name: String,
        description: String,
    ): CreateHouseholdResult {
        val ownerId = authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to create a household")
                .let { return CreateHouseholdResult.NotSignedIn }

        // Constraint verification
        val householdCount = cachedHouseholds.value.count()
        if (constraintsManager.canUserCreateOrJoinHousehold(householdCount = householdCount).not()) {
            crashLogger.logWarning(
                message = "User $ownerId tried to create a household but has already joined the maximum number of households",
            )
            return CreateHouseholdResult.LimitReached(limit = constraintsManager.maxHouseholdsPerUser)
        }

        // Offline the household is already created locally and synced later, so a timeout is not a failure.
        val id =
            withTimeoutOrNull(CREATE_TIMEOUT_MS.milliseconds) {
                dataSource.createHousehold(ownerId = ownerId, name = name, description = description)
            }
        if (id == null) crashLogger.logWarning(message = "Creating a household of user $ownerId timed out, it will sync when online")
        if (constraintsManager.canUserCreateOrJoinHousehold(householdCount + 1).not() && id != null) {
            dataSource.deleteHouseholdJoinKey(ownerId)
            refreshHouseholdJoinKey()
        }
        return CreateHouseholdResult.Created(householdId = id)
    }

    override suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    ) {
        authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to update household $householdId")
                .let { return }

        // Like create, the update only completes once the server acknowledged it, which never happens offline.
        // Firestore already applied it locally and syncs it later, so a timeout is not a failure.
        withTimeoutOrNull(UPDATE_TIMEOUT_MS.milliseconds) {
            dataSource.updateHousehold(householdId = householdId, name = name, description = description)
        }
            ?: crashLogger.logWarning(message = "Updating household $householdId timed out, it will sync when online")
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
        currentItemsCount: Int,
    ) {
        authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to add item to household with id $householdId")
                .let { return }

        // Constraint verification
        if (constraintsManager.canAddItemToHousehold(itemCount = currentItemsCount).not()) {
            crashLogger.logWarning(
                message = "User tried to add item to household, household already reached the max number of items ($currentItemsCount)",
            )
            return
        }

        dataSource.addItem(householdId = householdId, text = text, position = position)
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) = dataSource.updateItemText(householdId = householdId, itemId = itemId, text = text)

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) = dataSource.setItemChecked(householdId = householdId, itemId = itemId, isChecked = isChecked)

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) = dataSource.moveItem(householdId = householdId, itemId = itemId, position = position)

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) = dataSource.deleteItem(householdId = householdId, itemId = itemId)

    override suspend fun clearCheckedItems(householdId: String) = dataSource.clearCheckedItems(householdId)

    // A failed profile lookup must not fail the households stream; the members are fetched again on the next snapshot.
    private suspend fun fetchUsers(userIds: List<String>): List<UserProfile> =
        try {
            userRepository.getUsers(userIds)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            crashLogger.recordError(context = "Fetching household members failed", error = error, expected = ExpectedErrors.streams)
            emptyList()
        }

    // Every retry re-reads the whole query, so a used-up quota is not retried: it burns more reads and can not succeed.
    private suspend fun shouldRetryObservation(
        cause: Throwable,
        attempt: Long,
    ): Boolean {
        if (cause.isQuotaExceeded()) return false
        return (attempt < ITEMS_RETRY_COUNT).also { if (it) delay(OBSERVATION_RETRY_DELAY_MS.milliseconds) }
    }

    private fun recordNonAuthenticatedError(message: String) =
        crashLogger.recordException(Throwable(message = "Non-authenticated user error: $message"))
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

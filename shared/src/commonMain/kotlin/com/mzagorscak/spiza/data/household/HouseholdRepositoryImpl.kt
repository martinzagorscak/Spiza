package com.mzagorscak.spiza.data.household

import co.touchlab.kermit.Logger
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

// Firestore completes writes only once the server acknowledged them, which never happens while offline.
private const val LOG_TAG = "Spiza.Household"
private const val CREATE_TIMEOUT_MS = 10_000L
private const val ITEMS_RETRY_COUNT = 3
private const val ITEMS_RETRY_DELAY_MS = 1_000L

class HouseholdCreationTimeoutException : Exception("Creating the household timed out. Check your connection.")

internal class HouseholdRepositoryImpl(
    private val dataSource: HouseholdDataSource,
    private val authService: AuthService,
) : HouseholdRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHouseholds(): Flow<List<Household>> =
        authService.authState.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                dataSource
                    .observeHouseholds(user.uid)
                    .map { households -> households.map { it.toDomain() }.sortedBy { it.name.lowercase() } }
                    .onEach { Logger.d(tag = LOG_TAG) { "Households update: ${it.size} for user ${user.uid}" } }
                    .onCompletion { cause ->
                        if (cause != null) Logger.e(throwable = cause, tag = LOG_TAG) { "Households stream failed for user ${user.uid}" }
                    }
            }
        }

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
    ): String {
        val ownerId = authService.currentUser?.uid ?: error("No signed-in user")
        Logger.i(tag = LOG_TAG) { "Creating household for owner $ownerId" }
        val id =
            withTimeoutOrNull(CREATE_TIMEOUT_MS.milliseconds) { dataSource.createHousehold(ownerId, name, description) }
                ?: run {
                    Logger.e(tag = LOG_TAG) { "Creating household timed out after ${CREATE_TIMEOUT_MS}ms" }
                    throw HouseholdCreationTimeoutException()
                }
        Logger.i(tag = LOG_TAG) { "Household created: $id" }
        return id
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
    ) = dataSource.addItem(householdId, text, position)

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

private fun HouseholdDto.toDomain() =
    Household(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        joinedMembers = joinedMembers,
        pendingMembers = pendingMembers,
    )

private fun ArticleItemDto.toDomain() = ArticleItem(id = id, text = text, isChecked = isChecked, position = position)

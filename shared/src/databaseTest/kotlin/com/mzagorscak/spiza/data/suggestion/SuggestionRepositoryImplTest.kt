package com.mzagorscak.spiza.data.suggestion

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.data.local.SpizaDatabase
import com.mzagorscak.spiza.data.local.inMemorySpizaDatabase
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SuggestionRepositoryImplTest {
    private lateinit var database: SpizaDatabase
    private lateinit var repository: SuggestionRepositoryImpl

    @BeforeTest
    fun setUp() {
        database = inMemorySpizaDatabase()
        repository = SuggestionRepositoryImpl(suggestionDao = database.suggestionDao())
    }

    @AfterTest
    fun tearDown() = database.close()

    /**
     * Inserted in this order (ids 1..4), so an ordering that ignores its tie-break falls back to insertion order and fails:
     * apple and Banana share the highest use count, cherry and date the lowest, and only case-insensitive sorting puts
     * "Banana" between "apple" and "cherry".
     */
    private suspend fun storeSortingSamples() {
        listOf(
            SuggestionEntity(name = "apple", normalizedName = "apple", useCount = 3, lastUsedAt = 100L),
            SuggestionEntity(name = "Banana", normalizedName = "banana", useCount = 3, lastUsedAt = 400L),
            SuggestionEntity(name = "cherry", normalizedName = "cherry", useCount = 1, lastUsedAt = 300L),
            SuggestionEntity(name = "date", normalizedName = "date", useCount = 1, lastUsedAt = 200L),
        ).forEach { database.suggestionDao().insert(it) }
    }

    private suspend fun assertOrder(
        sortingType: SuggestionSortingType,
        vararg expectedNames: String,
    ) {
        storeSortingSamples()

        repository.observeAll(sortingType).test {
            assertThat(awaitItem().map { it.name }).containsExactly(*expectedNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun allSuggestions() = repository.observeAll(SuggestionSortingType.LAST_USED_DESC).first()

    @Test
    fun addOrUpdate_insertsANewSuggestion_usedOnce() =
        runTest {
            repository.addOrUpdate(name = "Milk", normalizedName = "milk", timestamp = 100L)

            val stored = allSuggestions().single()
            assertThat(stored).isEqualTo(
                ShoppingItemSuggestion(id = stored.id, name = "Milk", normalizedName = "milk", useCount = 1, lastUsedAt = 100L),
            )
        }

    @Test
    fun addOrUpdate_updatesTheSameSuggestion_withUseCountLatestSpellingAndLastUse_forTheSameNormalizedName() =
        runTest {
            repository.addOrUpdate(name = "milk", normalizedName = "milk", timestamp = 100L)
            val firstId = allSuggestions().single().id

            repository.addOrUpdate(name = "Milk", normalizedName = "milk", timestamp = 200L)

            assertThat(allSuggestions()).containsExactly(
                ShoppingItemSuggestion(id = firstId, name = "Milk", normalizedName = "milk", useCount = 2, lastUsedAt = 200L),
            )
        }

    @Test
    fun observeAll_ordersByLastUseNewestFirst_whenLastUsedDesc() =
        runTest { assertOrder(SuggestionSortingType.LAST_USED_DESC, "Banana", "cherry", "date", "apple") }

    @Test
    fun observeAll_ordersByLastUseOldestFirst_whenLastUsedAsc() =
        runTest { assertOrder(SuggestionSortingType.LAST_USED_ASC, "apple", "date", "cherry", "Banana") }

    @Test
    fun observeAll_ordersByNameIgnoringCase_whenAlphabeticalAsc() =
        runTest { assertOrder(SuggestionSortingType.ALPHABETICAL_ASC, "apple", "Banana", "cherry", "date") }

    @Test
    fun observeAll_ordersByNameIgnoringCaseReversed_whenAlphabeticalDesc() =
        runTest { assertOrder(SuggestionSortingType.ALPHABETICAL_DESC, "date", "cherry", "Banana", "apple") }

    @Test
    fun observeAll_ordersByUseCountThenLastUse_highestFirst_whenMostUsedDesc() =
        runTest { assertOrder(SuggestionSortingType.MOST_USED_DESC, "Banana", "apple", "cherry", "date") }

    @Test
    fun observeAll_ordersByUseCountThenLastUse_lowestFirst_whenMostUsedAsc() =
        runTest { assertOrder(SuggestionSortingType.MOST_USED_ASC, "date", "cherry", "apple", "Banana") }

    @Test
    fun delete_removesOnlyThatSuggestion() =
        runTest {
            repository.addOrUpdate(name = "Milk", normalizedName = "milk", timestamp = 100L)
            repository.addOrUpdate(name = "Bread", normalizedName = "bread", timestamp = 200L)
            val milkId = allSuggestions().single { it.normalizedName == "milk" }.id

            repository.delete(milkId)

            assertThat(allSuggestions().map { it.name }).containsExactly("Bread")
        }

    @Test
    fun deleteAll_removesEverySuggestion() =
        runTest {
            repository.addOrUpdate(name = "Milk", normalizedName = "milk", timestamp = 100L)
            repository.addOrUpdate(name = "Bread", normalizedName = "bread", timestamp = 200L)

            repository.deleteAll()

            assertThat(allSuggestions()).isEmpty()
        }
}

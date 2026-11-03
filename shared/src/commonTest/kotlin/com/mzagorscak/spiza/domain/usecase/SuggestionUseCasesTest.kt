package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.fakes.FakeSuggestionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TIMESTAMP_MS = 1_700_000_000_000L

class SuggestionUseCasesTest {
    private val repository = FakeSuggestionRepository()

    @Test
    fun addOrUpdate_trimsName_andNormalizesItToLowercase() =
        runTest {
            AddOrUpdateSuggestionUseCaseImpl(repository)(name = "  Fresh Milk ", timestamp = TIMESTAMP_MS)

            assertEquals(listOf("Fresh Milk"), repository.savedNames)
            assertEquals(listOf("fresh milk"), repository.savedNormalizedNames)
            assertEquals(listOf(TIMESTAMP_MS), repository.savedTimestamps)
        }

    @Test
    fun addOrUpdate_givesTheSameNormalizedName_regardlessOfCase() =
        runTest {
            val addOrUpdate = AddOrUpdateSuggestionUseCaseImpl(repository)

            addOrUpdate(name = "Milk", timestamp = TIMESTAMP_MS)
            addOrUpdate(name = "MILK", timestamp = TIMESTAMP_MS)

            assertEquals(listOf("milk", "milk"), repository.savedNormalizedNames)
        }

    @Test
    fun addOrUpdate_ignoresBlankNames() =
        runTest {
            val addOrUpdate = AddOrUpdateSuggestionUseCaseImpl(repository)

            addOrUpdate(name = "", timestamp = TIMESTAMP_MS)
            addOrUpdate(name = "   ", timestamp = TIMESTAMP_MS)

            assertTrue(repository.savedNames.isEmpty())
        }

    @Test
    fun observe_passesTheSortingType_andDefaultsToAlphabetical() {
        val observe = ObserveSuggestionsUseCaseImpl(repository)

        observe()
        assertEquals(SuggestionSortingType.ALPHABETICAL_ASC, repository.lastSortingType)

        observe(SuggestionSortingType.MOST_USED_DESC)
        assertEquals(SuggestionSortingType.MOST_USED_DESC, repository.lastSortingType)
    }

    @Test
    fun delete_andDeleteAll_forwardToTheRepository() =
        runTest {
            DeleteSuggestionUseCaseImpl(repository)(id = 7L)
            DeleteAllSuggestionsUseCaseImpl(repository)()

            assertEquals(listOf(7L), repository.deletedIds)
            assertEquals(1, repository.deleteAllCount)
        }
}

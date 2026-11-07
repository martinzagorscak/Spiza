package com.mzagorscak.spiza.data.suggestion

import com.mzagorscak.spiza.data.suggestion.local.SuggestionDao
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** In-memory DAO: the real `getAll` and `upsert` default methods run against it, so their logic is covered too. */
private class FakeSuggestionDao : SuggestionDao {
    val entities = MutableStateFlow<List<SuggestionEntity>>(emptyList())
    private var nextId = 1L

    private fun sorted(comparator: Comparator<SuggestionEntity>): Flow<List<SuggestionEntity>> = entities.map { it.sortedWith(comparator) }

    override fun getAllLastUsedDesc() = sorted(compareByDescending { it.lastUsedAt })

    override fun getAllLastUsedAsc() = sorted(compareBy { it.lastUsedAt })

    override fun getAllAlphabeticalAsc() = sorted(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    override fun getAllAlphabeticalDesc() = sorted(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })

    override fun getAllMostUsedDesc() = sorted(compareByDescending<SuggestionEntity> { it.useCount }.thenByDescending { it.lastUsedAt })

    override fun getAllMostUsedAsc() = sorted(compareBy<SuggestionEntity> { it.useCount }.thenBy { it.lastUsedAt })

    override suspend fun getByNormalizedName(normalizedName: String): SuggestionEntity? =
        entities.value.firstOrNull { it.normalizedName == normalizedName }

    override suspend fun insert(suggestion: SuggestionEntity) {
        val stored = if (suggestion.id == 0L) suggestion.copy(id = nextId++) else suggestion
        entities.value = entities.value.filterNot { it.id == stored.id } + stored
    }

    override suspend fun deleteById(id: Long) {
        entities.value = entities.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        entities.value = emptyList()
    }
}

class SuggestionRepositoryImplTest {
    private val dao = FakeSuggestionDao()
    private val repository = SuggestionRepositoryImpl(suggestionDao = dao)

    @Test
    fun addOrUpdate_insertsANewSuggestion_andMapsItToTheDomain() =
        runTest {
            repository.addOrUpdate(name = "Milk", normalizedName = "milk", timestamp = 100L)

            assertEquals(
                listOf(ShoppingItemSuggestion(id = 1L, name = "Milk", normalizedName = "milk", useCount = 1, lastUsedAt = 100L)),
                repository.observeAll().first(),
            )
        }

    @Test
    fun addOrUpdate_bumpsUseCount_andKeepsTheLatestSpelling_forTheSameNormalizedName() =
        runTest {
            repository.addOrUpdate(name = "milk", normalizedName = "milk", timestamp = 100L)
            repository.addOrUpdate(name = "Milk", normalizedName = "milk", timestamp = 200L)

            val suggestion = repository.observeAll().first().single()
            assertEquals("Milk", suggestion.name)
            assertEquals(2, suggestion.useCount)
            assertEquals(200L, suggestion.lastUsedAt)
        }

    @Test
    fun observeAll_usesTheRequestedSorting() =
        runTest {
            repository.addOrUpdate(name = "Bread", normalizedName = "bread", timestamp = 300L)
            repository.addOrUpdate(name = "apples", normalizedName = "apples", timestamp = 100L)
            repository.addOrUpdate(name = "Apples", normalizedName = "apples", timestamp = 200L)

            assertEquals(listOf("Apples", "Bread"), repository.observeAll(SuggestionSortingType.ALPHABETICAL_ASC).first().map { it.name })
            assertEquals(listOf("Bread", "Apples"), repository.observeAll(SuggestionSortingType.LAST_USED_DESC).first().map { it.name })
            assertEquals(listOf("Apples", "Bread"), repository.observeAll(SuggestionSortingType.MOST_USED_DESC).first().map { it.name })
        }
}

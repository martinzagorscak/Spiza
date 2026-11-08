package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Every write is recorded as an attempt **before** [writeFailure] applies; successful writes update [suggestions] like the Room
 * table (upsert by normalized name). [observeAll] emits [suggestions] in stored order whatever the sorting: the SQL orderings are
 * covered by `SuggestionRepositoryImplTest` on the real database.
 */
class FakeSuggestionRepository : SuggestionRepository {
    val suggestions = MutableStateFlow<List<ShoppingItemSuggestion>>(emptyList())

    /** When set, [observeAll] fails with it (checked each time the stream is collected). */
    var observeFailure: Throwable? = null

    /** When set, every write throws it. */
    var writeFailure: Throwable? = null

    data class Saved(
        val name: String,
        val normalizedName: String,
        val timestamp: Long,
    )

    /** Every [addOrUpdate] call with its arguments. */
    val saved = mutableListOf<Saved>()

    /** The sorting of every [observeAll] collection, in call order. */
    val requestedSortingTypes = mutableListOf<SuggestionSortingType>()
    val deletedIds = mutableListOf<Long>()
    var deleteAllCalls = 0

    override fun observeAll(sortingType: SuggestionSortingType): Flow<List<ShoppingItemSuggestion>> =
        flow {
            requestedSortingTypes += sortingType
            observeFailure?.let { throw it }
            emitAll(suggestions)
        }

    override suspend fun addOrUpdate(
        name: String,
        normalizedName: String,
        timestamp: Long,
    ) {
        saved += Saved(name = name, normalizedName = normalizedName, timestamp = timestamp)
        writeFailure?.let { throw it }
        val existing = suggestions.value.firstOrNull { it.normalizedName == normalizedName }
        val stored =
            existing?.copy(name = name, useCount = existing.useCount + 1, lastUsedAt = timestamp)
                ?: ShoppingItemSuggestion(
                    id = (suggestions.value.maxOfOrNull { it.id } ?: 0L) + 1,
                    name = name,
                    normalizedName = normalizedName,
                    useCount = 1,
                    lastUsedAt = timestamp,
                )
        suggestions.value = suggestions.value.filterNot { it.id == stored.id } + stored
    }

    override suspend fun delete(id: Long) {
        deletedIds += id
        writeFailure?.let { throw it }
        suggestions.value = suggestions.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        deleteAllCalls++
        writeFailure?.let { throw it }
        suggestions.value = emptyList()
    }
}

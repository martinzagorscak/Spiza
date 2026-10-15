package com.mzagorscak.spiza.data.suggestion.local

import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SuggestionLocalDataSource {
    fun observeAll(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<ShoppingItemSuggestion>>

    suspend fun addOrUpdate(
        name: String,
        timestamp: Long,
    )

    suspend fun delete(id: Long)
}

internal class SuggestionLocalDataSourceImpl(
    private val suggestionDao: SuggestionDao,
) : SuggestionLocalDataSource {
    override fun observeAll(sortingType: SuggestionSortingType): Flow<List<ShoppingItemSuggestion>> =
        suggestionDao.getAll(sortingType).map { suggestionEntities ->
            suggestionEntities.map { it.toDomainModel() }
        }

    override suspend fun addOrUpdate(
        name: String,
        timestamp: Long,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        val normalizedName = trimmedName.lowercase()
        suggestionDao.upsert(
            name = trimmedName,
            normalizedName = normalizedName,
            lastUsedAt = timestamp,
        )
    }

    override suspend fun delete(id: Long) {
        suggestionDao.deleteById(id)
    }
}

private fun SuggestionEntity.toDomainModel(): ShoppingItemSuggestion =
    ShoppingItemSuggestion(
        id = id,
        name = name,
        normalizedName = normalizedName,
        useCount = useCount,
        lastUsedAt = lastUsedAt,
    )

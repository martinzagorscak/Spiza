package com.mzagorscak.spiza.data.suggestion

import com.mzagorscak.spiza.data.suggestion.local.SuggestionDao
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class SuggestionRepositoryImpl(
    private val suggestionDao: SuggestionDao,
) : SuggestionRepository {
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

    override suspend fun deleteAll() {
        suggestionDao.deleteAll()
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

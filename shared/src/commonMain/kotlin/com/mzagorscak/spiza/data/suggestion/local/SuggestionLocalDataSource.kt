package com.mzagorscak.spiza.data.suggestion.local

import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import kotlinx.coroutines.flow.Flow

interface SuggestionLocalDataSource {
    fun observeAll(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<SuggestionEntity>>

    suspend fun addOrUpdate(
        name: String,
        timestamp: Long,
    )

    suspend fun delete(id: Long)
}

internal class SuggestionLocalDataSourceImpl(
    private val suggestionDao: SuggestionDao,
) : SuggestionLocalDataSource {
    override fun observeAll(sortingType: SuggestionSortingType): Flow<List<SuggestionEntity>> = suggestionDao.getAll(sortingType)

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

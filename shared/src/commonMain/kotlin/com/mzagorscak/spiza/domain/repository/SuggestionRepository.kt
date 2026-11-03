package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import kotlinx.coroutines.flow.Flow

interface SuggestionRepository {
    fun observeAll(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<ShoppingItemSuggestion>>

    /** [normalizedName] identifies the suggestion: adding one that already exists bumps its use count. */
    suspend fun addOrUpdate(
        name: String,
        normalizedName: String,
        timestamp: Long,
    )

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

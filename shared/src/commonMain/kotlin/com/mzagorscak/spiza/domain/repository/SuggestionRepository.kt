package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import kotlinx.coroutines.flow.Flow

interface SuggestionRepository {
    fun observeAll(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<ShoppingItemSuggestion>>

    suspend fun addOrUpdate(
        name: String,
        timestamp: Long,
    )

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

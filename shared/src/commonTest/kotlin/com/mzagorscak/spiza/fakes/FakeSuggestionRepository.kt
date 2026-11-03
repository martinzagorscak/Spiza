package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSuggestionRepository : SuggestionRepository {
    val suggestions = MutableStateFlow<List<ShoppingItemSuggestion>>(emptyList())
    val savedNames = mutableListOf<String>()

    override fun observeAll(sortingType: SuggestionSortingType): Flow<List<ShoppingItemSuggestion>> = suggestions

    override suspend fun addOrUpdate(
        name: String,
        timestamp: Long,
    ) {
        savedNames += name
    }

    override suspend fun delete(id: Long) = Unit

    override suspend fun deleteAll() = Unit
}

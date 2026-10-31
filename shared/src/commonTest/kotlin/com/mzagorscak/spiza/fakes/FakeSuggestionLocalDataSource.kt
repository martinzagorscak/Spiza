package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.suggestion.local.SuggestionLocalDataSource
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSuggestionLocalDataSource : SuggestionLocalDataSource {
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

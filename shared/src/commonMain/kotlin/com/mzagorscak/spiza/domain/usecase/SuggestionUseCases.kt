package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.data.suggestion.local.SuggestionLocalDataSource
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class ObserveSuggestionsUseCase(
    private val localDataSource: SuggestionLocalDataSource,
) {
    operator fun invoke(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<ShoppingItemSuggestion>> =
        localDataSource.observeAll(sortingType)
}

class AddOrUpdateSuggestionUseCase(
    private val localDataSource: SuggestionLocalDataSource,
) {
    suspend operator fun invoke(
        name: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    ) {
        localDataSource.addOrUpdate(name, timestamp)
    }
}

class DeleteSuggestionUseCase(
    private val localDataSource: SuggestionLocalDataSource,
) {
    suspend operator fun invoke(id: Long) {
        localDataSource.delete(id)
    }
}

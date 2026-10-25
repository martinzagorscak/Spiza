package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.data.suggestion.local.SuggestionLocalDataSource
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

interface ObserveSuggestionsUseCase {
    operator fun invoke(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<ShoppingItemSuggestion>>
}

class ObserveSuggestionsUseCaseImpl(
    private val localDataSource: SuggestionLocalDataSource,
) : ObserveSuggestionsUseCase {
    override operator fun invoke(sortingType: SuggestionSortingType): Flow<List<ShoppingItemSuggestion>> =
        localDataSource.observeAll(sortingType)
}

interface AddOrUpdateSuggestionUseCase {
    suspend operator fun invoke(
        name: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    )
}

class AddOrUpdateSuggestionUseCaseImpl(
    private val localDataSource: SuggestionLocalDataSource,
) : AddOrUpdateSuggestionUseCase {
    override suspend operator fun invoke(
        name: String,
        timestamp: Long,
    ) {
        localDataSource.addOrUpdate(name, timestamp)
    }
}

interface DeleteSuggestionUseCase {
    suspend operator fun invoke(id: Long)
}

class DeleteSuggestionUseCaseImpl(
    private val localDataSource: SuggestionLocalDataSource,
) : DeleteSuggestionUseCase {
    override suspend operator fun invoke(id: Long) {
        localDataSource.delete(id)
    }
}

interface DeleteAllSuggestionsUseCase {
    suspend operator fun invoke()
}

class DeleteAllSuggestionsUseCaseImpl(
    private val localDataSource: SuggestionLocalDataSource,
) : DeleteAllSuggestionsUseCase {
    override suspend operator fun invoke() {
        localDataSource.deleteAll()
    }
}

package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

interface ObserveSuggestionsUseCase {
    operator fun invoke(sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC): Flow<List<ShoppingItemSuggestion>>
}

class ObserveSuggestionsUseCaseImpl(
    private val repository: SuggestionRepository,
) : ObserveSuggestionsUseCase {
    override operator fun invoke(sortingType: SuggestionSortingType): Flow<List<ShoppingItemSuggestion>> =
        repository.observeAll(sortingType)
}

interface AddOrUpdateSuggestionUseCase {
    suspend operator fun invoke(
        name: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    )
}

class AddOrUpdateSuggestionUseCaseImpl(
    private val repository: SuggestionRepository,
) : AddOrUpdateSuggestionUseCase {
    override suspend operator fun invoke(
        name: String,
        timestamp: Long,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        // Case-insensitive dedup: "Milk" and "milk" are the same suggestion.
        repository.addOrUpdate(
            name = trimmedName,
            normalizedName = trimmedName.lowercase(),
            timestamp = timestamp,
        )
    }
}

interface DeleteSuggestionUseCase {
    suspend operator fun invoke(id: Long)
}

class DeleteSuggestionUseCaseImpl(
    private val repository: SuggestionRepository,
) : DeleteSuggestionUseCase {
    override suspend operator fun invoke(id: Long) {
        repository.delete(id)
    }
}

interface DeleteAllSuggestionsUseCase {
    suspend operator fun invoke()
}

class DeleteAllSuggestionsUseCaseImpl(
    private val repository: SuggestionRepository,
) : DeleteAllSuggestionsUseCase {
    override suspend operator fun invoke() {
        repository.deleteAll()
    }
}

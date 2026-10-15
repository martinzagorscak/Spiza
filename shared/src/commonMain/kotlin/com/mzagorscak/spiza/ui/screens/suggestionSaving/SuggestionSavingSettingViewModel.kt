package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import kotlinx.coroutines.flow.StateFlow

data class SuggestionSavingSettingState(
    val isEnabled: Boolean = false,
    val sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC,
    val suggestions: List<PresentableShoppingItemSuggestion> = emptyList(),
)

abstract class SuggestionSavingSettingViewModel : ViewModel() {
    abstract fun state(): StateFlow<SuggestionSavingSettingState>

    abstract fun onToggleSavingEnabled(enabled: Boolean)

    abstract fun setSortingType(sortingType: SuggestionSortingType)

    abstract fun onRemoveSuggestion(suggestionId: Long)
}

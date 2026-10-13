package com.mzagorscak.spiza.ui.screens.createHousehold

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "Spiza.Household"

internal class CreateHouseholdViewModelImpl(
    private val createHousehold: CreateHouseholdUseCase,
) : CreateHouseholdViewModel() {
    private val state = MutableStateFlow(CreateHouseholdState())

    override fun state(): StateFlow<CreateHouseholdState> = state.asStateFlow()

    override fun onNameChange(name: String) {
        state.update { it.copy(name = name, hasError = false) }
    }

    override fun onDescriptionChange(description: String) {
        state.update { it.copy(description = description, hasError = false) }
    }

    override fun onCreateClicked() {
        val current = state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            state.update { it.copy(isSubmitting = true, hasError = false) }
            try {
                createHousehold(current.name, current.description)
                state.update { it.copy(isSubmitting = false, isCreated = true) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Logger.e(throwable = error, tag = LOG_TAG) { "Creating household failed" }
                state.update { it.copy(isSubmitting = false, hasError = true) }
            }
        }
    }
}

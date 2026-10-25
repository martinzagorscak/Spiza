package com.mzagorscak.spiza.ui.screens.createHousehold

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateHouseholdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val EMPTY = ""

internal class CreateHouseholdViewModelImpl(
    scenario: HouseholdScenario = HouseholdScenario.Create,
    private val createHousehold: CreateHouseholdUseCase,
    private val updateHousehold: UpdateHouseholdUseCase,
    private val crashLogger: CrashLogger,
) : CreateHouseholdViewModel() {
    private val state = MutableStateFlow(
        CreateHouseholdState(
            id = when (scenario) {
                is HouseholdScenario.Create -> null
                is HouseholdScenario.Edit -> scenario.householdId
            },
            name = when (scenario) {
                is HouseholdScenario.Create -> EMPTY
                is HouseholdScenario.Edit -> scenario.householdName
            },
            description = when (scenario) {
                is HouseholdScenario.Create -> EMPTY
                is HouseholdScenario.Edit -> scenario.householdDescription
            },
        ),
    )

    override fun state(): StateFlow<CreateHouseholdState> = state.asStateFlow()

    override fun onNameChange(name: String) {
        if (name.length > CreateHouseholdState.MAX_NAME_LENGTH) return
        state.update { it.copy(name = name, hasError = false) }
    }

    override fun onDescriptionChange(description: String) {
        if (description.length > CreateHouseholdState.MAX_DESCRIPTION_LENGTH) return
        state.update { it.copy(description = description, hasError = false) }
    }

    override fun createHousehold() {
        val current = state.value
        if (current.canSubmit.not()) return
        viewModelScope.launch {
            state.update { it.copy(isSubmitting = true, hasError = false) }
            try {
                if (current.isEditScenario) {
                    val householdId = current.id ?: return@launch
                    updateHousehold(householdId, current.name, current.description)
                } else {
                    createHousehold(current.name, current.description)
                }
                state.update { it.copy(isSubmitting = false, isCreated = true) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                crashLogger.recordException(
                    Throwable(
                        message = if (current.isEditScenario) {
                            "Updating household failed; ${error.message}"
                        } else {
                            "Creating household failed; ${error.message}"
                        },
                        cause = error.cause,
                    ),
                )
                state.update { it.copy(isSubmitting = false, hasError = true) }
            }
        }
    }
}

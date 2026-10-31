package com.mzagorscak.spiza.ui.screens.createHousehold

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateHouseholdUseCase
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
        state.update { it.copy(isSubmitting = true, hasError = false) }
        launchSafely(
            crashLogger = crashLogger,
            context = if (current.isEditScenario) "Updating household failed" else "Creating household failed",
            onError = { state.update { it.copy(isSubmitting = false, hasError = true) } },
        ) {
            if (current.isEditScenario) {
                val householdId = current.id ?: return@launchSafely
                updateHousehold(householdId, current.name, current.description)
            } else {
                createHousehold(current.name, current.description)
            }
            state.update { it.copy(isSubmitting = false, isCreated = true) }
        }
    }
}

package com.mzagorscak.spiza.ui.screens.createHousehold

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

internal sealed class HouseholdScenario {
    data object Create : HouseholdScenario()

    data class Edit(
        val householdId: String,
        val householdName: String,
        val householdDescription: String,
    ) : HouseholdScenario()
}

data class CreateHouseholdState(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val isCreated: Boolean = false,
) {
    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_DESCRIPTION_LENGTH = 200
    }

    val isEditScenario get() = id != null
    val canSubmit: Boolean get() = name.isNotBlank() && isSubmitting.not()
}

abstract class CreateHouseholdViewModel : ViewModel() {
    abstract fun state(): StateFlow<CreateHouseholdState>

    abstract fun onNameChange(name: String)

    abstract fun onDescriptionChange(description: String)

    abstract fun createHousehold()
}

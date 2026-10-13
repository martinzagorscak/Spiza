package com.mzagorscak.spiza.ui.screens.createHousehold

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

data class CreateHouseholdState(
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val isCreated: Boolean = false,
) {
    val canSubmit: Boolean get() = name.isNotBlank() && !isSubmitting
}

abstract class CreateHouseholdViewModel : ViewModel() {
    abstract fun state(): StateFlow<CreateHouseholdState>

    abstract fun onNameChange(name: String)

    abstract fun onDescriptionChange(description: String)

    abstract fun onCreateClicked()
}

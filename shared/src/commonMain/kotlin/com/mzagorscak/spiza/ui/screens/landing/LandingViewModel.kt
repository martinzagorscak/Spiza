package com.mzagorscak.spiza.ui.screens.landing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

data class LandingState(
    val displayName: String? = null,
)

abstract class LandingViewModel : ViewModel() {
    abstract val state: StateFlow<LandingState>

    abstract fun onSignOutClicked()
}

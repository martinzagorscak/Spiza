package com.mzagorscak.spiza.ui.screens.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

abstract class LoginViewModel : ViewModel() {
    abstract val state: StateFlow<LoginState>

    abstract fun onSignInWithGoogleClicked()
}

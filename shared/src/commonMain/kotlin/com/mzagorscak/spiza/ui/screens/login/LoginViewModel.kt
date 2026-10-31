package com.mzagorscak.spiza.ui.screens.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

data class LoginState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

abstract class LoginViewModel : ViewModel() {
    abstract fun state(): StateFlow<LoginState>

    abstract fun signInWithGoogle()

    abstract fun onLoginErrorShown()
}

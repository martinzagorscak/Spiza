package com.mzagorscak.spiza.ui.screens.login

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.usecase.SignInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginViewModelImpl(
    private val signIn: SignInUseCase,
) : LoginViewModel() {
    private val state = MutableStateFlow(LoginState())

    override fun state(): StateFlow<LoginState> = state.asStateFlow()

    // Successful sign-in is not handled here: auth state changes and navigation reacts to it.
    override fun signInWithGoogle() {
        if (state.value.isLoading) return
        viewModelScope.launch {
            state.update { LoginState(isLoading = true) }
            val error =
                when (val result = signIn(AuthProvider.Google)) {
                    is AuthResult.Failure -> result.error.message ?: "Sign in failed"
                    is AuthResult.Cancelled -> result.message ?: "Sign in failed"
                    is AuthResult.Success -> null
                }
            state.update { LoginState(isLoading = error == null, error = error) }
        }
    }

    override fun onLoginErrorShown() {
        state.update { it.copy(error = null) }
    }
}

package com.mzagorscak.spiza.ui.screens.login

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginViewModelImpl(
    private val authService: AuthService,
) : LoginViewModel() {
    private val _state = MutableStateFlow(LoginState())
    override val state: StateFlow<LoginState> = _state.asStateFlow()

    // Successful sign-in is not handled here: auth state changes and navigation reacts to it.
    override fun onSignInWithGoogleClicked() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { LoginState(isLoading = true) }
            val error =
                when (val result = authService.signIn(AuthProvider.Google)) {
                    is AuthResult.Failure -> result.error.message ?: "Sign in failed"
                    is AuthResult.Cancelled, is AuthResult.Success -> null
                }
            _state.update { LoginState(isLoading = false, error = error) }
        }
    }
}

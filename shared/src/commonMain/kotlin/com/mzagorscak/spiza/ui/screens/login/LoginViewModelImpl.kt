package com.mzagorscak.spiza.ui.screens.login

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.usecase.SignInUseCase
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class LoginViewModelImpl(
    private val signIn: SignInUseCase,
    private val crashLogger: CrashLogger,
) : LoginViewModel() {
    private val state = MutableStateFlow(LoginState())

    override fun state(): StateFlow<LoginState> = state.asStateFlow()

    // Successful sign-in is not handled here: auth state changes and navigation reacts to it, so loading stays on.
    // Failures are already recorded by the repository; a cancelled sign-in is a user choice and shows nothing.
    override fun signInWithGoogle() {
        if (state.value.isLoading) return
        state.update { LoginState(isLoading = true) }
        launchSafely(
            crashLogger = crashLogger,
            context = "Sign-in failed",
            onError = { state.update { LoginState(hasError = true) } },
        ) {
            when (signIn(AuthProvider.Google)) {
                is AuthResult.Success -> Unit
                is AuthResult.Cancelled -> state.update { LoginState() }
                is AuthResult.Failure -> state.update { LoginState(hasError = true) }
            }
        }
    }

    override fun onLoginErrorShown() {
        state.update { it.copy(hasError = false) }
    }
}

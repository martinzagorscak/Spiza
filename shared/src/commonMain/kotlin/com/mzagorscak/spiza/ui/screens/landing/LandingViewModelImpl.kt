package com.mzagorscak.spiza.ui.screens.landing

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.auth.AuthService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class LandingViewModelImpl(
    private val authService: AuthService,
) : LandingViewModel() {
    override val state: StateFlow<LandingState> =
        authService.authState
            .map { LandingState(displayName = it?.displayName) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LandingState(displayName = authService.currentUser?.displayName),
            )

    override fun onSignOutClicked() {
        viewModelScope.launch { authService.signOut() }
    }
}

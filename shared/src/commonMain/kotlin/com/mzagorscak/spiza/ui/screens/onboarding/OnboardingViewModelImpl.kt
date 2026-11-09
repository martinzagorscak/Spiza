package com.mzagorscak.spiza.ui.screens.onboarding

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.usecase.SetOnboardingCompletedUseCase
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class OnboardingViewModelImpl(
    isReplay: Boolean,
    private val setOnboardingCompleted: SetOnboardingCompletedUseCase,
    private val crashLogger: CrashLogger,
) : OnboardingViewModel() {
    private val state = MutableStateFlow(OnboardingState(isReplay = isReplay))

    override fun state(): StateFlow<OnboardingState> = state.asStateFlow()

    override fun onFinish() {
        val current = state.value
        if (current.canFinish.not()) return
        if (current.isReplay) {
            state.update { it.copy(isFinished = true) }
            return
        }
        state.update { it.copy(isCompleting = true, hasError = false) }
        launchSafely(
            crashLogger = crashLogger,
            context = "Completing onboarding failed",
            onError = { state.update { it.copy(isCompleting = false, hasError = true) } },
        ) {
            setOnboardingCompleted()
            state.update { it.copy(isCompleting = false, isFinished = true) }
        }
    }

    override fun onErrorShown() {
        state.update { it.copy(hasError = false) }
    }
}

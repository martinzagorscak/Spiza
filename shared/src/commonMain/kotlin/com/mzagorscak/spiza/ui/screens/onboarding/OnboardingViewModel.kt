package com.mzagorscak.spiza.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * [isReplay] is set when the onboarding was opened again from settings: the flag is already stored, so finishing only closes it.
 * [isCompleting] is true while the completed flag is being stored on first run; finishing is blocked meanwhile.
 * [isFinished] tells the screen to leave; [hasError] is a transient write failure shown once and cleared by `onErrorShown`.
 */
data class OnboardingState(
    val isReplay: Boolean = false,
    val isCompleting: Boolean = false,
    val hasError: Boolean = false,
    val isFinished: Boolean = false,
) {
    val canFinish: Boolean get() = isCompleting.not() && isFinished.not()
}

abstract class OnboardingViewModel : ViewModel() {
    abstract fun state(): StateFlow<OnboardingState>

    /** Skip and "Get started" both finish the onboarding. */
    abstract fun onFinish()

    abstract fun onErrorShown()
}

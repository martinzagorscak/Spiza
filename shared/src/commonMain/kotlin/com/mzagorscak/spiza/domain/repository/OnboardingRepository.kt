package com.mzagorscak.spiza.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    /** Whether the onboarding was finished or skipped on this device. */
    val onboardingCompleted: Flow<Boolean>

    /** Synchronous snapshot used for the initial app state, so the start screen never flickers. */
    val isOnboardingCompleted: Boolean

    suspend fun setOnboardingCompleted()
}

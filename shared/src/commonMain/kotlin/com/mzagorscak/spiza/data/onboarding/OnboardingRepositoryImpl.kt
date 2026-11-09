package com.mzagorscak.spiza.data.onboarding

import com.mzagorscak.spiza.data.settings.SettingsDataSource
import com.mzagorscak.spiza.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

internal class OnboardingRepositoryImpl(
    private val settingsDataSource: SettingsDataSource,
) : OnboardingRepository {
    override val onboardingCompleted: Flow<Boolean> = settingsDataSource.onboardingCompleted

    override val isOnboardingCompleted: Boolean get() = settingsDataSource.onboardingCompleted.value

    override suspend fun setOnboardingCompleted() = settingsDataSource.setOnboardingCompleted()
}

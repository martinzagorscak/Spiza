package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

interface ObserveOnboardingCompletedUseCase {
    operator fun invoke(): Flow<Boolean>
}

class ObserveOnboardingCompletedUseCaseImpl(
    private val repository: OnboardingRepository,
) : ObserveOnboardingCompletedUseCase {
    override operator fun invoke(): Flow<Boolean> = repository.onboardingCompleted
}

interface GetOnboardingCompletedUseCase {
    operator fun invoke(): Boolean
}

class GetOnboardingCompletedUseCaseImpl(
    private val repository: OnboardingRepository,
) : GetOnboardingCompletedUseCase {
    override operator fun invoke(): Boolean = repository.isOnboardingCompleted
}

interface SetOnboardingCompletedUseCase {
    suspend operator fun invoke()
}

class SetOnboardingCompletedUseCaseImpl(
    private val repository: OnboardingRepository,
) : SetOnboardingCompletedUseCase {
    override suspend operator fun invoke() = repository.setOnboardingCompleted()
}

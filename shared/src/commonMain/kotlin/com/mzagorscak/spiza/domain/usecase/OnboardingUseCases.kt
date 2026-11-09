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

interface CompleteOnboardingUseCase {
    suspend operator fun invoke()
}

class CompleteOnboardingUseCaseImpl(
    private val repository: OnboardingRepository,
) : CompleteOnboardingUseCase {
    override suspend operator fun invoke() = repository.setOnboardingCompleted()
}

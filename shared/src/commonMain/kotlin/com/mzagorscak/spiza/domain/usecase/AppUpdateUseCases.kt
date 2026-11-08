package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.AppUpdateStatus
import com.mzagorscak.spiza.domain.repository.AppUpdateRepository

interface GetAppUpdateStatusUseCase {
    suspend operator fun invoke(): AppUpdateStatus
}

class GetAppUpdateStatusUseCaseImpl(
    private val repository: AppUpdateRepository,
) : GetAppUpdateStatusUseCase {
    override suspend operator fun invoke(): AppUpdateStatus {
        val currentVersion = repository.currentVersion()
        // An unknown installed version must never lock the user out.
        if (currentVersion <= 0) return AppUpdateStatus.UpToDate

        val config = repository.getVersionConfig()
        return when {
            currentVersion < config.minSupportedVersion -> AppUpdateStatus.UpdateRequired

            currentVersion < config.recommendedVersion &&
                config.recommendedVersion != repository.getDismissedRecommendedVersion() ->
                AppUpdateStatus.UpdateRecommended(recommendedVersion = config.recommendedVersion)

            else -> AppUpdateStatus.UpToDate
        }
    }
}

interface DismissRecommendedUpdateUseCase {
    /** Stops recommending [recommendedVersion]; a newer recommended version is shown again. */
    suspend operator fun invoke(recommendedVersion: Long)
}

class DismissRecommendedUpdateUseCaseImpl(
    private val repository: AppUpdateRepository,
) : DismissRecommendedUpdateUseCase {
    override suspend operator fun invoke(recommendedVersion: Long) = repository.setDismissedRecommendedVersion(recommendedVersion)
}

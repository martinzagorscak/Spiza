package com.mzagorscak.spiza.domain.model

/** Whether the installed app version is still allowed to run, see `GetAppUpdateStatusUseCase`. */
sealed interface AppUpdateStatus {
    data object UpToDate : AppUpdateStatus

    /** A newer version is available; the user may postpone it. Dismissing stores [recommendedVersion]. */
    data class UpdateRecommended(
        val recommendedVersion: Long,
    ) : AppUpdateStatus

    /** The installed version is no longer supported; the app must not be used until it is updated. */
    data object UpdateRequired : AppUpdateStatus
}

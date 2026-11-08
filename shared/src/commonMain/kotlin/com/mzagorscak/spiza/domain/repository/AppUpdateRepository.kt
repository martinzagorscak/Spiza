package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.AppVersionConfig

interface AppUpdateRepository {
    /** Installed version encoded like [AppVersionConfig]; 0 or less when it is unknown. */
    fun currentVersion(): Long

    /**
     * Never fails because of the remote config: when it cannot be fetched, the last activated values are used,
     * and when there are none, both thresholds are 0 (nothing is enforced).
     */
    suspend fun getVersionConfig(): AppVersionConfig

    /** The recommended version the user already postponed, or null if none. */
    suspend fun getDismissedRecommendedVersion(): Long?

    suspend fun setDismissedRecommendedVersion(version: Long)
}

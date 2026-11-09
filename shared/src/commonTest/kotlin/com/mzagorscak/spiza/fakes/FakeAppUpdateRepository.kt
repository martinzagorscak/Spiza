package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.AppVersionConfig
import com.mzagorscak.spiza.domain.repository.AppUpdateRepository
import kotlinx.coroutines.CompletableDeferred

/** Every dismissal is recorded as an attempt before [writeFailure] applies; successful ones are reflected in [dismissedVersion]. */
class FakeAppUpdateRepository : AppUpdateRepository {
    /** Installed version; 0 means unknown. */
    var installedVersion: Long = 10_000L

    /** What the remote config returns; zeros like the Remote Config defaults. */
    var versionConfig = AppVersionConfig(minSupportedVersion = 0, recommendedVersion = 0)

    /** The persisted dismissed recommended version. */
    var dismissedVersion: Long? = null

    /** When set, [getVersionConfig] throws it (the real repository fails open; this simulates an unexpected bug). */
    var versionConfigFailure: Throwable? = null

    /** When set, [setDismissedRecommendedVersion] throws it. */
    var writeFailure: Throwable? = null

    /** When set, [getVersionConfig] suspends until it is completed, like a slow fetch. */
    var versionConfigGate: CompletableDeferred<Unit>? = null

    var versionConfigRequests = 0
        private set

    val dismissedAttempts = mutableListOf<Long>()

    override fun currentVersion(): Long = installedVersion

    override suspend fun getVersionConfig(): AppVersionConfig {
        versionConfigRequests++
        versionConfigGate?.await()
        versionConfigFailure?.let { throw it }
        return versionConfig
    }

    override suspend fun getDismissedRecommendedVersion(): Long? = dismissedVersion

    override suspend fun setDismissedRecommendedVersion(version: Long) {
        dismissedAttempts += version
        writeFailure?.let { throw it }
        dismissedVersion = version
    }
}

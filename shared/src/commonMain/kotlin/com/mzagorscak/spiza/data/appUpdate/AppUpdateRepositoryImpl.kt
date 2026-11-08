package com.mzagorscak.spiza.data.appUpdate

import com.mzagorscak.spiza.Platform
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigDataSource
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigKeys
import com.mzagorscak.spiza.data.settings.SettingsDataSource
import com.mzagorscak.spiza.domain.model.AppVersionConfig
import com.mzagorscak.spiza.domain.repository.AppUpdateRepository
import com.mzagorscak.spiza.getPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private const val FETCH_TIMEOUT_MS = 5_000L

private const val FETCH_FAILED_CONTEXT = "Fetching app update config failed"

// Being offline (including Remote Config throttling, see the data sources) is normal. Anything else is recorded as a non-fatal.
private val EXPECTED_FETCH_ERRORS = setOf(DataException.Reason.Offline)

/** Thresholds used when no config is available at all: 0 never triggers an update. */
private val FAIL_OPEN_CONFIG = AppVersionConfig(minSupportedVersion = 0, recommendedVersion = 0)

internal class AppUpdateRepositoryImpl(
    private val remoteConfigDataSource: RemoteConfigDataSource,
    private val settingsDataSource: SettingsDataSource,
    private val versionProvider: AppVersionProvider,
    private val crashLogger: CrashLogger,
    platform: Platform = getPlatform(),
) : AppUpdateRepository {
    private val minSupportedVersionKey =
        when (platform) {
            Platform.Android -> RemoteConfigKeys.ANDROID_MIN_SUPPORTED_VERSION
            Platform.Ios -> RemoteConfigKeys.IOS_MIN_SUPPORTED_VERSION
        }
    private val recommendedVersionKey =
        when (platform) {
            Platform.Android -> RemoteConfigKeys.ANDROID_RECOMMENDED_VERSION
            Platform.Ios -> RemoteConfigKeys.IOS_RECOMMENDED_VERSION
        }

    override fun currentVersion(): Long = versionProvider.currentVersion()

    // A failed fetch keeps the last activated values, so a forced update stays in force while offline.
    override suspend fun getVersionConfig(): AppVersionConfig {
        fetchLatestConfig()
        return try {
            AppVersionConfig(
                minSupportedVersion = remoteConfigDataSource.getLong(minSupportedVersionKey),
                recommendedVersion = remoteConfigDataSource.getLong(recommendedVersionKey),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            crashLogger.recordError(context = "Reading app update config failed", error = error, expected = emptySet())
            FAIL_OPEN_CONFIG
        }
    }

    override suspend fun getDismissedRecommendedVersion(): Long? = settingsDataSource.dismissedRecommendedVersion.value

    override suspend fun setDismissedRecommendedVersion(version: Long) = settingsDataSource.setDismissedRecommendedVersion(version)

    private suspend fun fetchLatestConfig() {
        val fetched =
            try {
                withTimeoutOrNull(FETCH_TIMEOUT_MS.milliseconds) { remoteConfigDataSource.fetchAndActivate() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                crashLogger.recordError(context = FETCH_FAILED_CONTEXT, error = error, expected = EXPECTED_FETCH_ERRORS)
                return
            }
        if (fetched == null) crashLogger.logWarning(message = "$FETCH_FAILED_CONTEXT (timeout)")
    }
}

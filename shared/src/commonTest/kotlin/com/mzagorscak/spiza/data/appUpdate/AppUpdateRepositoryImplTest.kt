package com.mzagorscak.spiza.data.appUpdate

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.single
import com.mzagorscak.spiza.Platform
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigKeys
import com.mzagorscak.spiza.data.settings.SettingsDataSourceImpl
import com.mzagorscak.spiza.domain.model.AppVersionConfig
import com.mzagorscak.spiza.fakes.FakeAppVersionProvider
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeRemoteConfigDataSource
import com.mzagorscak.spiza.fakes.FakeSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppUpdateRepositoryImplTest {
    private val remoteConfigDataSource =
        FakeRemoteConfigDataSource().apply {
            publish(key = RemoteConfigKeys.ANDROID_MIN_SUPPORTED_VERSION, value = 10_100)
            publish(key = RemoteConfigKeys.ANDROID_RECOMMENDED_VERSION, value = 10_200)
            publish(key = RemoteConfigKeys.IOS_MIN_SUPPORTED_VERSION, value = 20_100)
            publish(key = RemoteConfigKeys.IOS_RECOMMENDED_VERSION, value = 20_200)
            activate(key = RemoteConfigKeys.ANDROID_MIN_SUPPORTED_VERSION, value = 10_000)
            activate(key = RemoteConfigKeys.ANDROID_RECOMMENDED_VERSION, value = 10_001)
        }
    private val storage = FakeSettings()
    private val crashLogger = FakeCrashLogger()

    private val lastActivated = AppVersionConfig(minSupportedVersion = 10_000, recommendedVersion = 10_001)

    private fun repository(platform: Platform = Platform.Android) =
        AppUpdateRepositoryImpl(
            remoteConfigDataSource = remoteConfigDataSource,
            settingsDataSource = SettingsDataSourceImpl(settings = storage),
            versionProvider = FakeAppVersionProvider(),
            crashLogger = crashLogger,
            platform = platform,
        )

    @Test
    fun getVersionConfig_returnsTheFetchedValues_forTheCurrentPlatformOnly() =
        runTest {
            assertThat(repository(platform = Platform.Android).getVersionConfig())
                .isEqualTo(AppVersionConfig(minSupportedVersion = 10_100, recommendedVersion = 10_200))
            assertThat(repository(platform = Platform.Ios).getVersionConfig())
                .isEqualTo(AppVersionConfig(minSupportedVersion = 20_100, recommendedVersion = 20_200))
        }

    @Test
    fun setDismissedRecommendedVersion_persistsIt_soTheNextAppStartRestoresIt() =
        runTest {
            assertThat(repository().getDismissedRecommendedVersion()).isNull()

            repository().setDismissedRecommendedVersion(10_203)

            assertThat(repository().getDismissedRecommendedVersion()).isEqualTo(10_203L)
        }

    // region Error handling

    @Test
    fun getVersionConfig_returnsTheLastActivatedValues_andLogsAWarning_whenOffline() =
        runTest {
            remoteConfigDataSource.fetchFailure = DataException(reason = DataException.Reason.Offline, message = "No connection")

            val config = repository().getVersionConfig()

            assertThat(config).isEqualTo(lastActivated)
            assertThat(crashLogger.warnings).single().contains("No connection")
            assertThat(crashLogger.exceptions).isEmpty()
        }

    @Test
    fun getVersionConfig_returnsTheLastActivatedValues_andRecordsANonFatal_whenTheFetchFailsUnexpectedly() =
        runTest {
            val failure = DataException(reason = DataException.Reason.Unknown, message = "Remote Config SDK error")
            remoteConfigDataSource.fetchFailure = failure

            val config = repository().getVersionConfig()

            assertThat(config).isEqualTo(lastActivated)
            assertThat(crashLogger.exceptions).single().transform { it.cause?.message }.isEqualTo(failure.message)
        }

    @Test
    fun getVersionConfig_returnsTheLastActivatedValues_andLogsAWarning_whenTheFetchTimesOut() =
        runTest {
            remoteConfigDataSource.neverCompleteFetch = true

            val config = repository().getVersionConfig()

            assertThat(config).isEqualTo(lastActivated)
            assertThat(crashLogger.warnings).single().contains("timeout")
            assertThat(crashLogger.exceptions).isEmpty()
        }

    @Test
    fun getVersionConfig_failsOpen_whenNeitherFetchedNorActivatedValuesCanBeRead() =
        runTest {
            remoteConfigDataSource.fetchFailure = DataException(reason = DataException.Reason.Offline, message = "No connection")
            remoteConfigDataSource.readFailure = IllegalStateException("Remote Config not initialised")

            val config = repository().getVersionConfig()

            assertThat(config).isEqualTo(AppVersionConfig(minSupportedVersion = 0, recommendedVersion = 0))
            assertThat(crashLogger.exceptions).single().transform { it.cause?.message }.isEqualTo("Remote Config not initialised")
        }

    // endregion
}

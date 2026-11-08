package com.mzagorscak.spiza.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.domain.model.AppUpdateStatus
import com.mzagorscak.spiza.domain.model.AppVersionConfig
import com.mzagorscak.spiza.fakes.FakeAppUpdateRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val INSTALLED = 10_203L

class AppUpdateUseCasesTest {
    private val repository = FakeAppUpdateRepository().apply { installedVersion = INSTALLED }
    private val getAppUpdateStatus = GetAppUpdateStatusUseCaseImpl(repository = repository)

    private fun config(
        minSupported: Long = 0,
        recommended: Long = 0,
    ) = AppVersionConfig(minSupportedVersion = minSupported, recommendedVersion = recommended)

    @Test
    fun getAppUpdateStatus_returnsUpdateRequired_whenBelowMinSupported_evenIfAnUpdateIsAlsoRecommended() =
        runTest {
            repository.versionConfig = config(minSupported = INSTALLED + 1, recommended = INSTALLED + 2)

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpdateRequired)
        }

    @Test
    fun getAppUpdateStatus_returnsUpToDate_whenEqualToBothThresholds() =
        runTest {
            repository.versionConfig = config(minSupported = INSTALLED, recommended = INSTALLED)

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpToDate)
        }

    @Test
    fun getAppUpdateStatus_returnsUpdateRecommended_withTheRecommendedVersion_whenBelowRecommended() =
        runTest {
            repository.versionConfig = config(minSupported = INSTALLED, recommended = INSTALLED + 1)

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpdateRecommended(recommendedVersion = INSTALLED + 1))
        }

    @Test
    fun getAppUpdateStatus_returnsUpToDate_whenTheRecommendedVersionWasDismissed() =
        runTest {
            repository.versionConfig = config(recommended = INSTALLED + 1)
            repository.dismissedVersion = INSTALLED + 1

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpToDate)
        }

    @Test
    fun getAppUpdateStatus_returnsUpdateRecommended_whenANewerVersionThanTheDismissedOneIsRecommended() =
        runTest {
            repository.versionConfig = config(recommended = INSTALLED + 2)
            repository.dismissedVersion = INSTALLED + 1

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpdateRecommended(recommendedVersion = INSTALLED + 2))
        }

    @Test
    fun getAppUpdateStatus_returnsUpToDate_whenThresholdsAreNotSet() =
        runTest {
            repository.versionConfig = config(minSupported = 0, recommended = 0)

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpToDate)
        }

    // region Error handling

    @Test
    fun getAppUpdateStatus_returnsUpToDate_whenTheInstalledVersionIsUnknown() =
        runTest {
            repository.installedVersion = 0
            repository.versionConfig = config(minSupported = INSTALLED, recommended = INSTALLED)

            assertThat(getAppUpdateStatus()).isEqualTo(AppUpdateStatus.UpToDate)
        }

    // endregion
}

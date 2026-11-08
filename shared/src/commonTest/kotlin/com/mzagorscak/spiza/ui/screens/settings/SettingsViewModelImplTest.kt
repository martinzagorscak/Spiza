package com.mzagorscak.spiza.ui.screens.settings

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.usecase.CanUserJoinHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetHouseholdJoinKeyUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.RefreshHouseholdJoinKeyUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SignOutUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.FakeSettingsRepository
import com.mzagorscak.spiza.fakes.FakeUserRepository
import com.mzagorscak.spiza.fakes.TEST_PROFILE
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelImplTest {
    private val households = FakeHouseholdRepository()
    private val settings = FakeSettingsRepository()
    private val users = FakeUserRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModelImpl(
            observeUserProfile = ObserveUserProfileUseCaseImpl(users),
            canUserJoinHousehold = CanUserJoinHouseholdUseCaseImpl(repository = households, constraintsManager = ConstraintsManagerImpl),
            createHouseholdJoinKey = CreateHouseholdJoinKeyUseCaseImpl(households),
            getHouseholdJoinKey = GetHouseholdJoinKeyUseCaseImpl(households),
            refreshHouseholdJoinKey = RefreshHouseholdJoinKeyUseCaseImpl(households),
            observeThemeMode = ObserveThemeModeUseCaseImpl(settings),
            observeAppLanguage = ObserveAppLanguageUseCaseImpl(settings),
            setThemeMode = SetThemeModeUseCaseImpl(settings),
            setAppLanguage = SetAppLanguageUseCaseImpl(settings),
            signOut = SignOutUseCaseImpl(users),
            crashLogger = logger,
        )

    @Test
    fun state_combinesProfileJoinKeyAndPreferences() =
        runTest {
            settings.themeModes.value = ThemeMode.Dark
            settings.languages.value = AppLanguage.Croatian

            val state = viewModel().state().value

            assertThat(state.profile).isEqualTo(TEST_PROFILE.toPresentable(joinKey = "join-key"))
            assertThat(state.showHouseholdJoinKeySection).isTrue()
            assertThat(state.themeMode).isEqualTo(ThemeMode.Dark)
            assertThat(state.language).isEqualTo(AppLanguage.Croatian)
        }

    @Test
    fun createJoinKey_createsTheJoinKey_andShowsIt() =
        runTest {
            households.joinKey.value = null
            val viewModel = viewModel()

            viewModel.createJoinKey()

            assertThat(households.createJoinKeyCalls).isEqualTo(1)
            assertThat(
                viewModel
                    .state()
                    .value.profile.joinKey,
            ).isEqualTo("join-key-1")
            assertThat(viewModel.state().value.errorState).isEqualTo(SettingsState.ErrorState.None)
        }

    @Test
    fun refreshJoinKey_refreshesTheJoinKey_andShowsTheNewOne() =
        runTest {
            val viewModel = viewModel()

            viewModel.refreshJoinKey()

            assertThat(households.refreshJoinKeyCalls).isEqualTo(1)
            assertThat(
                viewModel
                    .state()
                    .value.profile.joinKey,
            ).isEqualTo("join-key-1")
            assertThat(viewModel.state().value.errorState).isEqualTo(SettingsState.ErrorState.None)
        }

    @Test
    fun onThemeModeSelected_savesTheMode_andShowsItInState() =
        runTest {
            val viewModel = viewModel()

            viewModel.onThemeModeSelected(ThemeMode.Light)

            assertThat(settings.currentThemeMode).isEqualTo(ThemeMode.Light)
            assertThat(viewModel.state().value.themeMode).isEqualTo(ThemeMode.Light)
        }

    @Test
    fun onLanguageSelected_savesTheLanguage_andShowsItInState() =
        runTest {
            val viewModel = viewModel()

            viewModel.onLanguageSelected(AppLanguage.Croatian)

            assertThat(settings.currentLanguage).isEqualTo(AppLanguage.Croatian)
            assertThat(viewModel.state().value.language).isEqualTo(AppLanguage.Croatian)
        }

    @Test
    fun onSignOut_signsOut_andShowsPlaceholderProfile() =
        runTest {
            val viewModel = viewModel()

            viewModel.onSignOut()

            assertThat(users.signOutCalls).isEqualTo(1)
            val state = viewModel.state().value
            assertThat(state.profile).isEqualTo(SettingsState.PlaceholderProfile)
            assertThat(state.isSigningOut).isFalse()
        }

    // region Error handling

    @Test
    fun createJoinKey_setsQuotaExceededError_untilItIsShown() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.createJoinKey()

            assertThat(viewModel.state().value.errorState).isEqualTo(SettingsState.ErrorState.QuotaExceeded)
            assertThat(logger.exceptions).hasSize(1)

            viewModel.onErrorShown()

            assertThat(viewModel.state().value.errorState).isEqualTo(SettingsState.ErrorState.None)
        }

    @Test
    fun createJoinKey_setsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            val viewModel = viewModel()

            viewModel.createJoinKey()

            assertThat(viewModel.state().value.errorState).isEqualTo(SettingsState.ErrorState.Generic)
        }

    @Test
    fun onSignOut_ignoresRepeatedTaps_whileSigningOut() =
        runTest {
            users.neverCompleteSignOut = true
            val viewModel = viewModel()

            viewModel.onSignOut()
            viewModel.onSignOut()

            assertThat(viewModel.state().value.isSigningOut).isTrue()
            assertThat(users.signOutCalls).isEqualTo(1)
        }

    @Test
    fun onSignOut_stopsSigningOut_andSetsGenericError_whenSignOutFails() =
        runTest {
            val error = IllegalStateException("sign-out failed")
            users.signOutFailure = error
            val viewModel = viewModel()

            viewModel.onSignOut()

            val state = viewModel.state().value
            assertThat(state.isSigningOut).isFalse()
            assertThat(state.errorState).isEqualTo(SettingsState.ErrorState.Generic)
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(error.message)
        }

    // endregion
}

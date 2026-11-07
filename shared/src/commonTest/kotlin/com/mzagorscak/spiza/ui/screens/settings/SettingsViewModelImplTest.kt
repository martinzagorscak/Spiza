package com.mzagorscak.spiza.ui.screens.settings

import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun createJoinKey_setsQuotaExceededError_untilItIsShown() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.createJoinKey()

            assertEquals(SettingsState.ErrorState.QuotaExceeded, viewModel.state().value.errorState)
            assertEquals(1, logger.exceptions.size)

            viewModel.onErrorShown()

            assertEquals(SettingsState.ErrorState.None, viewModel.state().value.errorState)
        }

    @Test
    fun createJoinKey_setsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            val viewModel = viewModel()

            viewModel.createJoinKey()

            assertEquals(SettingsState.ErrorState.Generic, viewModel.state().value.errorState)
        }
}

package com.mzagorscak.spiza.ui.screens.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.usecase.GetCurrentAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetOnboardingCompletedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveOnboardingCompletedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeOnboardingRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelImplTest {
    private val settings = FakeSettingsRepository()
    private val userRepository = FakeUserRepository()
    private val onboarding = FakeOnboardingRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        AppViewModelImpl(
            observeThemeMode = ObserveThemeModeUseCaseImpl(settings),
            observeAppLanguage = ObserveAppLanguageUseCaseImpl(settings),
            observeUserProfile = ObserveUserProfileUseCaseImpl(userRepository),
            observeOnboardingCompleted = ObserveOnboardingCompletedUseCaseImpl(onboarding),
            getCurrentThemeMode = GetCurrentThemeModeUseCaseImpl(settings),
            getCurrentAppLanguage = GetCurrentAppLanguageUseCaseImpl(settings),
            getCurrentUserProfile = GetCurrentUserProfileUseCaseImpl(userRepository),
            getOnboardingCompleted = GetOnboardingCompletedUseCaseImpl(onboarding),
            crashLogger = logger,
        )

    @Test
    fun state_followsThemeLanguageAndSignInChanges() =
        runTest {
            val viewModel = viewModel()
            assertThat(viewModel.state().value)
                .isEqualTo(
                    AppState(
                        themeMode = ThemeMode.System,
                        language = AppLanguage.English,
                        isSignedIn = true,
                        isOnboardingCompleted = false,
                    ),
                )

            settings.themeModes.value = ThemeMode.Dark
            settings.languages.value = AppLanguage.Croatian
            userRepository.currentUserProfile.value = null

            assertThat(viewModel.state().value)
                .isEqualTo(
                    AppState(
                        themeMode = ThemeMode.Dark,
                        language = AppLanguage.Croatian,
                        isSignedIn = false,
                        isOnboardingCompleted = false,
                    ),
                )
        }

    @Test
    fun state_startsWithTheStoredOnboardingFlag_andFollowsCompletion() =
        runTest {
            val viewModel = viewModel()
            assertThat(viewModel.state().value.isOnboardingCompleted).isFalse()

            onboarding.setOnboardingCompleted()

            assertThat(viewModel.state().value.isOnboardingCompleted).isTrue()
        }

    // region Error handling

    @Test
    fun state_keepsTheCurrentPreferences_whenTheStreamFails() =
        runTest {
            // The seeded state is what the root UI renders before (or instead of) the streams, so it must not fall back to defaults
            // (a completed onboarding falling back to false would show it again).
            settings.themeModes.value = ThemeMode.Dark
            settings.languages.value = AppLanguage.Croatian
            onboarding.completed.value = true
            val failure = IllegalStateException("datastore corrupted")
            settings.themeModeFailure = failure

            val state = viewModel().state().value

            assertThat(state).isEqualTo(
                AppState(
                    themeMode = ThemeMode.Dark,
                    language = AppLanguage.Croatian,
                    isSignedIn = true,
                    isOnboardingCompleted = true,
                ),
            )
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    // endregion
}

package com.mzagorscak.spiza.ui.screens.onboarding

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.usecase.SetOnboardingCompletedUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeOnboardingRepository
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
class OnboardingViewModelImplTest {
    private val onboarding = FakeOnboardingRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(isReplay: Boolean = false) =
        OnboardingViewModelImpl(
            isReplay = isReplay,
            setOnboardingCompleted = SetOnboardingCompletedUseCaseImpl(onboarding),
            crashLogger = logger,
        )

    @Test
    fun onFinish_storesTheFlagAndFinishes_onFirstRun() =
        runTest {
            val viewModel = viewModel()

            viewModel.onFinish()

            assertThat(onboarding.completed.value).isTrue()
            assertThat(viewModel.state().value).isEqualTo(OnboardingState(isReplay = false, isFinished = true))
        }

    @Test
    fun onFinish_writesOnce_whenTappedWhileCompleting() =
        runTest {
            onboarding.neverCompleteWrites = true
            val viewModel = viewModel()
            viewModel.onFinish()

            viewModel.onFinish()

            assertThat(onboarding.completeAttempts).isEqualTo(1)
            assertThat(viewModel.state().value.canFinish).isFalse()
        }

    @Test
    fun onFinish_finishesWithoutWriting_whenReplayed() =
        runTest {
            val viewModel = viewModel(isReplay = true)

            viewModel.onFinish()

            assertThat(onboarding.completeAttempts).isEqualTo(0)
            assertThat(viewModel.state().value.isFinished).isTrue()
        }

    // region Error handling

    @Test
    fun onFinish_showsErrorAndStays_whenStoringTheFlagFails() =
        runTest {
            val failure = IllegalStateException("settings write failed")
            onboarding.writeFailure = failure
            val viewModel = viewModel()

            viewModel.onFinish()

            assertThat(viewModel.state().value).isEqualTo(OnboardingState(hasError = true))
            assertThat(onboarding.completed.value).isFalse()
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun onErrorShown_clearsTheError_soFinishingCanBeRetried() =
        runTest {
            onboarding.writeFailure = IllegalStateException("settings write failed")
            val viewModel = viewModel()
            viewModel.onFinish()

            viewModel.onErrorShown()
            onboarding.writeFailure = null
            viewModel.onFinish()

            assertThat(viewModel.state().value).isEqualTo(OnboardingState(isFinished = true))
            assertThat(onboarding.completed.value).isTrue()
        }

    // endregion
}

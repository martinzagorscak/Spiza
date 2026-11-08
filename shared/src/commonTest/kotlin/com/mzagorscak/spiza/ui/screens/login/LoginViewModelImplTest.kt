package com.mzagorscak.spiza.ui.screens.login

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.usecase.SignInUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
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
class LoginViewModelImplTest {
    private val userRepository = FakeUserRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = LoginViewModelImpl(signIn = SignInUseCaseImpl(userRepository), crashLogger = logger)

    @Test
    fun signIn_signsInWithGoogleAndKeepsLoading_whenSuccessful() =
        runTest {
            val viewModel = viewModel()

            viewModel.signInWithGoogle()

            // Navigation reacts to the auth state, so the screen keeps loading until it is replaced.
            assertThat(viewModel.state().value).isEqualTo(LoginState(isLoading = true))
            assertThat(userRepository.signInProviders).containsExactly(AuthProvider.Google)
        }

    // region Error handling

    @Test
    fun signIn_isNotStartedTwice_whenTappedAgainWhileLoading() =
        runTest {
            val viewModel = viewModel()
            viewModel.signInWithGoogle()

            viewModel.signInWithGoogle()

            assertThat(userRepository.signInProviders).hasSize(1)
        }

    @Test
    fun signIn_showsNothing_whenUserCancels() =
        runTest {
            userRepository.signInResult = AuthResult.Cancelled()
            val viewModel = viewModel()

            viewModel.signInWithGoogle()

            assertThat(viewModel.state().value).isEqualTo(LoginState())
        }

    @Test
    fun signIn_showsError_whenItFails_untilTheErrorIsShown() =
        runTest {
            userRepository.signInResult = AuthResult.Failure(IllegalStateException("token rejected"))
            val viewModel = viewModel()

            viewModel.signInWithGoogle()
            assertThat(viewModel.state().value).isEqualTo(LoginState(hasError = true))

            viewModel.onLoginErrorShown()
            assertThat(viewModel.state().value).isEqualTo(LoginState())
        }

    @Test
    fun signIn_showsErrorAndRecordsIt_whenSignInThrows() =
        runTest {
            val failure = IllegalStateException("credential manager crashed")
            userRepository.signInFailure = failure
            val viewModel = viewModel()

            viewModel.signInWithGoogle()

            assertThat(viewModel.state().value).isEqualTo(LoginState(hasError = true))
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    // endregion
}

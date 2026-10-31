package com.mzagorscak.spiza.ui.screens.login

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
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelImplTest {
    private val userRepository = FakeUserRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = LoginViewModelImpl(signIn = SignInUseCaseImpl(userRepository), crashLogger = FakeCrashLogger())

    @Test
    fun signIn_keepsLoading_whenSuccessful() =
        runTest {
            val viewModel = viewModel()

            viewModel.signInWithGoogle()

            assertEquals(LoginState(isLoading = true), viewModel.state().value)
        }

    @Test
    fun signIn_showsNothing_whenUserCancels() =
        runTest {
            userRepository.signInResult = AuthResult.Cancelled()
            val viewModel = viewModel()

            viewModel.signInWithGoogle()

            assertEquals(LoginState(), viewModel.state().value)
        }

    @Test
    fun signIn_showsError_whenItFails_untilTheErrorIsShown() =
        runTest {
            userRepository.signInResult = AuthResult.Failure(IllegalStateException("token rejected"))
            val viewModel = viewModel()

            viewModel.signInWithGoogle()
            assertEquals(LoginState(hasError = true), viewModel.state().value)

            viewModel.onLoginErrorShown()
            assertEquals(LoginState(), viewModel.state().value)
        }
}

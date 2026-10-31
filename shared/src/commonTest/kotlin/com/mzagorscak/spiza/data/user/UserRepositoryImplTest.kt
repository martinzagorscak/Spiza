package com.mzagorscak.spiza.data.user

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.fakes.FakeAuthService
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeUserDataSource
import com.mzagorscak.spiza.fakes.TEST_PROFILE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UserRepositoryImplTest {
    private val authService = FakeAuthService()
    private val userDataSource = FakeUserDataSource()
    private val logger = FakeCrashLogger()
    private val repository = UserRepositoryImpl(authService = authService, userDataSource = userDataSource, crashLogger = logger)

    @Test
    fun profile_isEmitted_whenSavingTheProfileDocumentFails() =
        runTest {
            userDataSource.saveFailure = DataException(DataException.Reason.Offline, "offline")

            assertEquals(TEST_PROFILE, repository.profile.first())
            assertEquals(1, logger.warnings.size)
            assertTrue(logger.exceptions.isEmpty())
        }

    @Test
    fun profile_savesTheSignedInUser() =
        runTest {
            repository.profile.first()

            assertEquals(listOf(TEST_PROFILE.id), userDataSource.savedProfiles.map { it.id })
        }

    @Test
    fun signIn_recordsFailure_withTheOriginalError() =
        runTest {
            val error = IllegalStateException("token rejected")
            authService.signInResult = AuthResult.Failure(error)

            repository.signIn(AuthProvider.Google)

            assertSame(error, logger.exceptions.single().cause)
        }
}

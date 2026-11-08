package com.mzagorscak.spiza.data.user

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.user.model.UserProfileDto
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.fakes.FakeAuthService
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeUserDataSource
import com.mzagorscak.spiza.fakes.TEST_PROFILE
import com.mzagorscak.spiza.fakes.TEST_USER
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UserRepositoryImplTest {
    private val authService = FakeAuthService()
    private val userDataSource = FakeUserDataSource()
    private val logger = FakeCrashLogger()
    private val repository = UserRepositoryImpl(authService = authService, userDataSource = userDataSource, crashLogger = logger)

    @Test
    fun profile_followsTheSignedInUser_andSavesOnlySignedInProfiles() =
        runTest {
            repository.profile.test {
                assertThat(awaitItem()).isEqualTo(TEST_PROFILE)

                authService.user.value = null

                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(userDataSource.savedProfiles).containsExactly(
                UserProfileDto(id = TEST_USER.uid, name = TEST_USER.displayName, email = TEST_USER.email, photoUrl = null),
            )
        }

    @Test
    fun currentProfile_isTheSignedInUser_orNullWhenSignedOut() {
        assertThat(repository.currentProfile).isEqualTo(TEST_PROFILE)

        authService.user.value = null

        assertThat(repository.currentProfile).isNull()
    }

    @Test
    fun getUsers_returnsProfilesOfTheRequestedUsers() =
        runTest {
            userDataSource.users =
                listOf(
                    UserProfileDto(id = "user-2", name = "Ivo", email = "ivo@example.com", photoUrl = "https://photo"),
                    UserProfileDto(id = "user-3", name = "Mia", email = null, photoUrl = null),
                )

            val users = repository.getUsers(listOf("user-2"))

            assertThat(users).containsExactly(
                UserProfile(id = "user-2", name = "Ivo", email = "ivo@example.com", photoUrl = "https://photo"),
            )
        }

    @Test
    fun signIn_returnsTheResult_withoutRecordingAnError_whenSuccessful() =
        runTest {
            val result = repository.signIn(AuthProvider.Google)

            assertThat(result).isEqualTo(AuthResult.Success(TEST_USER))
            assertThat(logger.exceptions).isEmpty()
        }

    @Test
    fun signOut_signsTheUserOut() =
        runTest {
            repository.signOut()

            repository.profile.test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // region Error handling

    @Test
    fun profile_isEmitted_whenSavingTheProfileDocumentFails() =
        runTest {
            userDataSource.saveFailure = DataException(DataException.Reason.Offline, "offline")

            repository.profile.test {
                assertThat(awaitItem()).isEqualTo(TEST_PROFILE)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(logger.warnings).hasSize(1)
            assertThat(logger.exceptions).isEmpty()
        }

    @Test
    fun signIn_recordsFailure_withTheOriginalError() =
        runTest {
            val error = IllegalStateException("token rejected")
            authService.signInResult = AuthResult.Failure(error)

            repository.signIn(AuthProvider.Google)

            val recorded = logger.exceptions.single()
            assertThat(recorded.cause?.message).isEqualTo(error.message)
        }

    // endregion
}

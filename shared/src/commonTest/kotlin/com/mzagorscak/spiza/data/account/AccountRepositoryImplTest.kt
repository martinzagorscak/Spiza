package com.mzagorscak.spiza.data.account

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.UserMismatchException
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.settings.SettingsDataSourceImpl
import com.mzagorscak.spiza.domain.model.DeleteAccountResult
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.fakes.FakeAccountDataSource
import com.mzagorscak.spiza.fakes.FakeAuthService
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeLocalDataCleaner
import com.mzagorscak.spiza.fakes.FakeSettings
import com.mzagorscak.spiza.fakes.TEST_USER
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccountRepositoryImplTest {
    private val authService = FakeAuthService()
    private val accountData = FakeAccountDataSource()
    private val storage = FakeSettings()
    private val settings = SettingsDataSourceImpl(settings = storage)
    private val localData = FakeLocalDataCleaner()
    private val logger = FakeCrashLogger()
    private val repository =
        AccountRepositoryImpl(
            authService = authService,
            accountDataSource = accountData,
            settingsDataSource = settings,
            localDataCleaner = localData,
            crashLogger = logger,
        )

    @BeforeTest
    fun setUp() {
        settings.setThemeMode(ThemeMode.Dark)
        settings.setOnboardingCompleted()
    }

    private fun assertAuthUserAndLocalDataKept() {
        assertThat(authService.deleteUserCalls).isEqualTo(0)
        assertThat(authService.currentUser).isNotNull()
        assertThat(localData.clearAllCalls).isEqualTo(0)
        assertThat(settings.onboardingCompleted.value).isTrue()
        assertThat(settings.themeMode.value).isEqualTo(ThemeMode.Dark)
    }

    private fun assertNothingDeleted() {
        assertThat(accountData.deleteAttempts).isEmpty()
        assertAuthUserAndLocalDataKept()
    }

    @Test
    fun deleteAccount_deletesRemoteDataAuthUserAndLocalData_whenConfirmed() =
        runTest {
            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.Deleted)
            assertThat(authService.reauthenticateCalls).isEqualTo(1)
            assertThat(accountData.deletedUserIds).containsExactly(TEST_USER.uid, TEST_USER.uid)
            assertThat(authService.deleteUserCalls).isEqualTo(1)
            assertThat(authService.currentUser).isNull()
            assertThat(localData.clearAllCalls).isEqualTo(1)
            assertThat(settings.onboardingCompleted.value).isFalse()
            assertThat(settings.themeMode.value).isEqualTo(ThemeMode.System)
            assertThat(logger.exceptions).isEmpty()
        }

    // The rules only let a signed-in user delete their data, and local data is the user's fallback while the account exists.
    @Test
    fun deleteAccount_deletesRemoteDataBeforeTheAuthUser_andTheAuthUserBeforeLocalData() =
        runTest {
            var reauthenticatedBeforeRemote = false
            var authUserDeletionsBeforeRemote = -1
            var localWipesBeforeRemote = -1
            accountData.onDelete = {
                reauthenticatedBeforeRemote = authService.reauthenticateCalls == 1
                authUserDeletionsBeforeRemote = authService.deleteUserCalls
                localWipesBeforeRemote = localData.clearAllCalls
            }
            authService.deleteUserFailure = DataException(reason = DataException.Reason.Offline, message = "network")

            assertFailure { repository.deleteAccount() }.isInstanceOf<DataException>()

            assertThat(accountData.deleteAttempts).containsExactly(TEST_USER.uid, TEST_USER.uid)
            assertThat(reauthenticatedBeforeRemote).isTrue()
            assertThat(authUserDeletionsBeforeRemote).isEqualTo(0)
            assertThat(localWipesBeforeRemote).isEqualTo(0)
            // The auth user deletion was attempted after the remote batch, and its failure kept the local data.
            assertThat(authService.deleteUserCalls).isEqualTo(1)
            assertThat(localData.clearAllCalls).isEqualTo(0)
        }

    @Test
    fun deleteAccount_deletesNothing_whenTheUserCancelsTheConfirmation() =
        runTest {
            authService.reauthenticateResult = AuthResult.Cancelled()

            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.Cancelled)
            assertNothingDeleted()
        }

    @Test
    fun deleteAccount_finishesTheDeletion_whenTheCallerIsCancelledAfterConfirming() =
        runTest {
            val acknowledgement = CompletableDeferred<Unit>()
            accountData.gate = acknowledgement
            val deletion = launch { repository.deleteAccount() }
            runCurrent()

            deletion.cancel()
            acknowledgement.complete(Unit)
            advanceUntilIdle()

            assertThat(accountData.deletedUserIds).containsExactly(TEST_USER.uid, TEST_USER.uid)
            assertThat(authService.currentUser).isNull()
            assertThat(localData.clearAllCalls).isEqualTo(1)
            assertThat(settings.onboardingCompleted.value).isFalse()
        }

    @Test
    fun deleteAccount_deletesNothing_andLogsAWarning_whenConfirmedWithAnotherAccount() =
        runTest {
            authService.reauthenticateResult = AuthResult.Failure(UserMismatchException(message = "user mismatch"))

            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.WrongAccount)
            assertNothingDeleted()
            assertThat(logger.warnings).hasSize(1)
            assertThat(logger.exceptions).isEmpty()
        }

    // region Error handling

    @Test
    fun deleteAccount_returnsNotSignedIn_andRecordsIt_whenSignedOut() =
        runTest {
            authService.user.value = null

            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.NotSignedIn)
            assertThat(authService.reauthenticateCalls).isEqualTo(0)
            assertThat(accountData.deleteAttempts).isEmpty()
            assertThat(logger.exceptions).hasSize(1)
        }

    @Test
    fun deleteAccount_throwsWithTheReason_andDeletesNothing_whenReauthenticationFails() =
        runTest {
            val failure = DataException(reason = DataException.Reason.Offline, message = "network")
            authService.reauthenticateResult = AuthResult.Failure(failure)

            assertFailure { repository.deleteAccount() }
                .isInstanceOf<DataException>()
                .prop(DataException::reason)
                .isEqualTo(DataException.Reason.Offline)
            assertNothingDeleted()
        }

    @Test
    fun deleteAccount_throwsWithTheReason_andKeepsAuthUserAndLocalData_whenTheBatchIsRejected() =
        runTest {
            accountData.deleteFailure = DataException(reason = DataException.Reason.PermissionDenied, message = "denied")

            assertFailure { repository.deleteAccount() }
                .isInstanceOf<DataException>()
                .prop(DataException::reason)
                .isEqualTo(DataException.Reason.PermissionDenied)

            assertThat(accountData.deleteAttempts).containsExactly(TEST_USER.uid)
            assertThat(accountData.deletedUserIds).isEmpty()
            assertAuthUserAndLocalDataKept()
        }

    // The second pass catches a household the user was added to while the first pass was prepared; until it succeeds the
    // account must stay complete enough to retry.
    @Test
    fun deleteAccount_throws_andKeepsAuthUserAndLocalData_whenTheSecondPassFails() =
        runTest {
            accountData.succeedingAttempts = 1
            accountData.deleteFailure = DataException(reason = DataException.Reason.PermissionDenied, message = "denied")

            assertFailure { repository.deleteAccount() }
                .isInstanceOf<DataException>()
                .prop(DataException::reason)
                .isEqualTo(DataException.Reason.PermissionDenied)

            assertThat(accountData.deleteAttempts).containsExactly(TEST_USER.uid, TEST_USER.uid)
            assertThat(accountData.deletedUserIds).containsExactly(TEST_USER.uid)
            assertAuthUserAndLocalDataKept()
        }

    @Test
    fun deleteAccount_failsAsOffline_andKeepsAuthUserAndLocalData_whenTheBatchIsNotAcknowledged() =
        runTest {
            accountData.neverAcknowledge = true

            assertFailure { repository.deleteAccount() }
                .isInstanceOf<DataException>()
                .prop(DataException::reason)
                .isEqualTo(DataException.Reason.Offline)

            assertThat(accountData.deletedUserIds).isEmpty()
            assertAuthUserAndLocalDataKept()
        }

    @Test
    fun deleteAccount_keepsLocalData_whenDeletingTheAuthUserFails() =
        runTest {
            authService.deleteUserFailure = DataException(reason = DataException.Reason.Offline, message = "network")

            assertFailure { repository.deleteAccount() }.isInstanceOf<DataException>()

            assertThat(authService.currentUser).isNotNull()
            assertThat(localData.clearAllCalls).isEqualTo(0)
            assertThat(settings.onboardingCompleted.value).isTrue()
        }

    @Test
    fun deleteAccount_returnsDeleted_andRecordsTheFailure_whenWipingTheDatabaseFails() =
        runTest {
            localData.clearFailure = IllegalStateException("database closed")

            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.Deleted)
            assertThat(authService.currentUser).isNull()
            assertThat(settings.onboardingCompleted.value).isFalse()
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo("database closed")
        }

    @Test
    fun deleteAccount_stillWipesTheDatabase_andReturnsDeleted_whenClearingSettingsFails() =
        runTest {
            storage.removeFailure = IllegalStateException("storage unavailable")

            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.Deleted)
            assertThat(localData.clearAllCalls).isEqualTo(1)
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo("storage unavailable")
        }

    // endregion
}

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
import com.mzagorscak.spiza.data.user.model.UserProfileDto
import com.mzagorscak.spiza.domain.model.DeleteAccountResult
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.fakes.FakeAuthService
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdDataSource
import com.mzagorscak.spiza.fakes.FakeLocalDataCleaner
import com.mzagorscak.spiza.fakes.FakeSettings
import com.mzagorscak.spiza.fakes.FakeUserDataSource
import com.mzagorscak.spiza.fakes.TEST_USER
import com.mzagorscak.spiza.fakes.householdDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val OTHER_USER_ID = "user-2"

class AccountRepositoryImplTest {
    private val authService = FakeAuthService()
    private val households = FakeHouseholdDataSource()
    private val users = FakeUserDataSource()
    private val storage = FakeSettings()
    private val settings = SettingsDataSourceImpl(settings = storage)
    private val localData = FakeLocalDataCleaner()
    private val logger = FakeCrashLogger()
    private val repository =
        AccountRepositoryImpl(
            authService = authService,
            householdDataSource = households,
            userDataSource = users,
            settingsDataSource = settings,
            localDataCleaner = localData,
            crashLogger = logger,
        )

    private val ownedShared = householdDto(id = "owned-shared", members = listOf(TEST_USER.uid, OTHER_USER_ID))
    private val joined = householdDto(id = "joined", ownerId = OTHER_USER_ID, members = listOf(OTHER_USER_ID, TEST_USER.uid))
    private val unrelated = householdDto(id = "unrelated", ownerId = OTHER_USER_ID, members = listOf(OTHER_USER_ID))

    @BeforeTest
    fun setUp() {
        households.households.value = listOf(ownedShared, joined, unrelated)
        households.joinKeys["join-key"] = TEST_USER.uid
        users.users =
            listOf(
                UserProfileDto(id = TEST_USER.uid, name = TEST_USER.displayName, email = TEST_USER.email, photoUrl = null),
                UserProfileDto(id = OTHER_USER_ID, name = "Ivo", email = null, photoUrl = null),
            )
        settings.setThemeMode(ThemeMode.Dark)
        settings.setOnboardingCompleted()
    }

    private fun assertNothingDeleted() {
        assertThat(households.households.value).containsExactly(ownedShared, joined, unrelated)
        assertThat(households.deletedJoinKeysOfUsers).isEmpty()
        assertThat(users.deletedProfileIds).isEmpty()
        assertThat(authService.deleteUserCalls).isEqualTo(0)
        assertThat(localData.clearAllCalls).isEqualTo(0)
        assertThat(settings.onboardingCompleted.value).isTrue()
    }

    @Test
    fun deleteAccount_deletesOwnedHouseholds_andLeavesJoinedOnes() =
        runTest {
            repository.deleteAccount()

            // The owned household is gone for its other member too; the joined one stays for its owner without the user.
            assertThat(households.households.value).containsExactly(
                joined.copy(joinedMembers = listOf(OTHER_USER_ID)),
                unrelated,
            )
        }

    @Test
    fun deleteAccount_deletesRemoteDataAuthUserAndLocalData_whenConfirmed() =
        runTest {
            val result = repository.deleteAccount()

            assertThat(result).isEqualTo(DeleteAccountResult.Deleted)
            assertThat(authService.reauthenticateCalls).isEqualTo(1)
            assertThat(households.deletedJoinKeysOfUsers).containsExactly(TEST_USER.uid)
            assertThat(households.joinKeys).isEmpty()
            assertThat(users.deletedProfileIds).containsExactly(TEST_USER.uid)
            assertThat(users.users.map { it.id }).containsExactly(OTHER_USER_ID)
            assertThat(authService.deleteUserCalls).isEqualTo(1)
            assertThat(authService.currentUser).isNull()
            assertThat(localData.clearAllCalls).isEqualTo(1)
            assertThat(settings.onboardingCompleted.value).isFalse()
            assertThat(settings.themeMode.value).isEqualTo(ThemeMode.System)
            assertThat(logger.exceptions).isEmpty()
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
            users.deleteGate = acknowledgement
            val deletion = launch { repository.deleteAccount() }
            runCurrent()

            deletion.cancel()
            acknowledgement.complete(Unit)
            advanceUntilIdle()

            assertThat(users.deletedProfileIds).containsExactly(TEST_USER.uid)
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
            assertThat(logger.exceptions).hasSize(1)
            assertThat(households.households.value).hasSize(3)
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

    // The join key goes before the memberships are read, so nobody can add the user to a household in between.
    @Test
    fun deleteAccount_hasDeletedTheJoinKey_andKeepsAuthUserAndLocalData_whenReadingHouseholdsFails() =
        runTest {
            households.getHouseholdsFailure = DataException(reason = DataException.Reason.Offline, message = "offline")

            assertFailure { repository.deleteAccount() }.isInstanceOf<DataException>()

            assertThat(households.deletedJoinKeysOfUsers).containsExactly(TEST_USER.uid)
            assertThat(households.households.value).containsExactly(ownedShared, joined, unrelated)
            assertThat(users.deletedProfileIds).isEmpty()
            assertThat(authService.deleteUserCalls).isEqualTo(0)
            assertThat(localData.clearAllCalls).isEqualTo(0)
            assertThat(settings.onboardingCompleted.value).isTrue()
        }

    @Test
    fun deleteAccount_keepsAuthUserAndLocalData_whenDeletingTheProfileFails() =
        runTest {
            users.deleteFailure = DataException(reason = DataException.Reason.PermissionDenied, message = "denied")

            assertFailure { repository.deleteAccount() }.isInstanceOf<DataException>()

            assertThat(authService.deleteUserCalls).isEqualTo(0)
            assertThat(authService.currentUser).isNotNull()
            assertThat(localData.clearAllCalls).isEqualTo(0)
            assertThat(settings.onboardingCompleted.value).isTrue()
        }

    @Test
    fun deleteAccount_failsAsOffline_andKeepsAuthUser_whenDeletionsAreNotAcknowledged() =
        runTest {
            households.neverAcknowledgeWrites = true

            assertFailure { repository.deleteAccount() }
                .isInstanceOf<DataException>()
                .prop(DataException::reason)
                .isEqualTo(DataException.Reason.Offline)

            assertThat(authService.deleteUserCalls).isEqualTo(0)
            assertThat(localData.clearAllCalls).isEqualTo(0)
            assertThat(settings.onboardingCompleted.value).isTrue()
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

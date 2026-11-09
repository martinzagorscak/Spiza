package com.mzagorscak.spiza.data.account

import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.UserMismatchException
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.crash.dataErrorReason
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.local.LocalDataCleaner
import com.mzagorscak.spiza.data.settings.SettingsDataSource
import com.mzagorscak.spiza.data.user.UserDataSource
import com.mzagorscak.spiza.domain.model.DeleteAccountResult
import com.mzagorscak.spiza.domain.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

// Unlike create/update, a timeout is a failure here: offline the deletes are only queued locally, and the auth user must not be
// deleted before its data is really gone (afterwards the rules would reject the queued writes of a user that no longer exists).
private const val DELETE_ACCOUNT_DATA_TIMEOUT_MS = 20_000L

internal class AccountRepositoryImpl(
    private val authService: AuthService,
    private val householdDataSource: HouseholdDataSource,
    private val userDataSource: UserDataSource,
    private val settingsDataSource: SettingsDataSource,
    private val localDataCleaner: LocalDataCleaner,
    private val crashLogger: CrashLogger,
) : AccountRepository {
    override suspend fun deleteAccount(): DeleteAccountResult {
        val userId = authService.currentUser?.uid
            ?: recordNonAuthenticatedError(message = "User tried to delete their account").let {
                return DeleteAccountResult.NotSignedIn
            }

        // First, so a cancelled or failed confirmation deletes nothing, and Firebase allows deleting the auth user afterwards
        // (it requires a recent login).
        when (val result = authService.reauthenticate()) {
            is AuthResult.Success -> Unit
            is AuthResult.Cancelled -> return DeleteAccountResult.Cancelled
            is AuthResult.Failure -> {
                if (result.error is UserMismatchException) {
                    crashLogger.logWarning(message = "User $userId confirmed account deletion with another account")
                    return DeleteAccountResult.WrongAccount
                }
                throw DataException(
                    reason = result.error.dataErrorReason(),
                    message = "Reauthentication before account deletion failed; ${result.error.message}",
                    cause = result.error,
                )
            }
        }

        // Once confirmed, leaving Settings (which cancels the ViewModel scope) must not stop the deletion halfway. The remote
        // phase is bounded by its timeout, so this can not hang.
        withContext(NonCancellable) {
            // The remote data goes while the user still exists: the rules only let a signed-in member or owner delete it.
            withTimeoutOrNull(DELETE_ACCOUNT_DATA_TIMEOUT_MS.milliseconds) { deleteRemoteData(userId) }
                ?: throw DataException(
                    reason = DataException.Reason.Offline,
                    message = "Deleting the data of user $userId timed out",
                )

            authService.deleteCurrentUser()
            // After the auth user: if that fails, the user is still signed in and keeps their local data to retry with.
            wipeLocalData()
        }
        return DeleteAccountResult.Deleted
    }

    private suspend fun deleteRemoteData(userId: String) {
        // The join key first, so nobody can add the user to a household after the memberships below were read.
        householdDataSource.deleteHouseholdJoinKey(userId)
        householdDataSource.getHouseholds(userId).forEach { household ->
            if (household.ownerId == userId) {
                // Also for the other members: the household can not exist without its owner.
                householdDataSource.deleteHousehold(household.id)
            } else {
                householdDataSource.removeMemberFromHousehold(householdId = household.id, memberId = userId)
            }
        }
        userDataSource.deleteUserProfile(userId)
    }

    // The account is already deleted, so a failed wipe is only recorded: nothing may surface as an error anymore. Each part is
    // guarded on its own so one failure does not leave the other behind.
    private suspend fun wipeLocalData() {
        recordingFailure(context = "Clearing settings after account deletion failed") { settingsDataSource.clear() }
        recordingFailure(context = "Clearing the local database after account deletion failed") { localDataCleaner.clearAll() }
    }

    private suspend fun recordingFailure(
        context: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            crashLogger.recordError(context = context, error = error)
        }
    }

    private fun recordNonAuthenticatedError(message: String) =
        crashLogger.recordException(Throwable(message = "Non-authenticated user error: $message"))
}

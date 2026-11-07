package com.mzagorscak.spiza.ui.screens.householdManagement

import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.usecase.AddMemberToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanAddMemberToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.RemoveMemberFromHouseholdUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.FakeUserRepository
import com.mzagorscak.spiza.fakes.TEST_USER
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdManagementViewModelImplTest {
    private val households = FakeHouseholdRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        HouseholdManagementViewModelImpl(
            getCurrentUserProfileUseCase = GetCurrentUserProfileUseCaseImpl(FakeUserRepository()),
            observeHouseholds = ObserveHouseholdsUseCaseImpl(households),
            addMemberToHousehold = AddMemberToHouseholdUseCaseImpl(households),
            removeMemberFromHouseholdUseCase = RemoveMemberFromHouseholdUseCaseImpl(households),
            deleteHouseholdUseCase = DeleteHouseholdUseCaseImpl(households),
            canUserCreateHousehold = CanUserCreateHouseholdUseCaseImpl(ConstraintsManagerImpl),
            canAddMemberToHousehold = CanAddMemberToHouseholdUseCaseImpl(ConstraintsManagerImpl),
            crashLogger = logger,
        )

    @Test
    fun failedStream_setsOnlyQuotaFallback_whenQuotaIsExceeded() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")

            val state = viewModel().state().value

            assertTrue(state.isQuotaExceeded)
            assertEquals(HouseholdManagementScreenState.ErrorState.None, state.errorState, "The fallback replaces the snackbar")
            assertEquals(1, logger.exceptions.size)
            assertTrue(logger.warnings.isEmpty())
        }

    @Test
    fun retry_restoresHouseholds_afterQuotaStreamFailure() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            households.householdsFailure = null
            viewModel.retry()

            val state = viewModel.state().value
            assertFalse(state.isQuotaExceeded)
            assertEquals(listOf("household-1"), state.households.map { it.id })
        }

    @Test
    fun dialogState_keepsUpdating_afterStreamRetry() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()
            households.householdsFailure = null
            viewModel.retry()

            viewModel.showAddMemberToHouseholdDialog(householdId = "household-1")

            assertEquals(
                HouseholdManagementScreenState.DialogState.AddMemberToHousehold(householdId = "household-1"),
                viewModel.state().value.dialogState,
            )
        }

    @Test
    fun failedStream_setsGenericErrorWithoutQuotaFlag_whenFailureIsNotQuota() =
        runTest {
            households.householdsFailure = IllegalStateException("listener failed")

            val state = viewModel().state().value

            assertFalse(state.isQuotaExceeded)
            assertEquals(HouseholdManagementScreenState.ErrorState.Generic, state.errorState)
        }

    @Test
    fun loadedStream_hasNoQuotaFlag_whenHouseholdsLoad() =
        runTest {
            val state = viewModel().state().value

            assertFalse(state.isQuotaExceeded)
            assertEquals(listOf("household-1"), state.households.map { it.id })
        }

    @Test
    fun onErrorShown_keepsQuotaFallback_afterQuotaStreamFailure() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.onErrorShown()

            assertTrue(viewModel.state().value.isQuotaExceeded, "The fallback must stay until the stream works again")
        }

    @Test
    fun deleteHousehold_setsQuotaExceededError_whenQuotaIsExceeded() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = TEST_USER.uid)

            assertEquals(HouseholdManagementScreenState.ErrorState.QuotaExceeded, viewModel.state().value.errorState)
            assertFalse(viewModel.state().value.isQuotaExceeded, "A failed write must not replace the household list")
        }

    @Test
    fun deleteHousehold_setsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            val viewModel = viewModel()

            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = TEST_USER.uid)

            assertEquals(HouseholdManagementScreenState.ErrorState.Generic, viewModel.state().value.errorState)
        }

    @Test
    fun removeMember_setsQuotaExceededError_whenQuotaIsExceeded() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.removeMemberFromHousehold(householdId = "household-1", memberId = "user-2", householdOwnerId = TEST_USER.uid)

            assertEquals(HouseholdManagementScreenState.ErrorState.QuotaExceeded, viewModel.state().value.errorState)
        }

    @Test
    fun onErrorShown_resetsError_afterQuotaWriteFailure() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()
            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = TEST_USER.uid)

            viewModel.onErrorShown()

            assertEquals(HouseholdManagementScreenState.ErrorState.None, viewModel.state().value.errorState)
        }
}

package com.mzagorscak.spiza.ui.screens.householdManagement

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.AddMemberResult
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.model.UserProfile
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
import com.mzagorscak.spiza.fakes.TEST_PROFILE
import com.mzagorscak.spiza.fakes.TEST_USER
import com.mzagorscak.spiza.fakes.household
import com.mzagorscak.spiza.ui.model.PresentableHousehold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val OTHER_PROFILE = UserProfile(id = "user-2", name = "Ivo", email = null, photoUrl = null)

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

    private fun sharedHousehold(ownerId: String) =
        Household(id = "household-1", name = "Home", description = "", ownerId = ownerId, members = listOf(TEST_PROFILE, OTHER_PROFILE))

    private fun HouseholdManagementViewModelImpl.presentedHousehold(id: String = "household-1"): PresentableHousehold =
        state().value.households.single { it.id == id }

    @Test
    fun state_showsObservedHouseholds_withOwnershipOfTheCurrentUser() =
        runTest {
            households.households.value = listOf(household(id = "own"), household(id = "joined", ownerId = OTHER_PROFILE.id))
            val viewModel = viewModel()

            val state = viewModel.state().value
            assertThat(state.households.map { it.id to it.isCurrentUserOwner }).containsExactly("own" to true, "joined" to false)
            assertThat(state.households.map { it.canOwnerAddNewMember }).containsExactly(true, true)
            assertThat(state.canUserCreateHousehold).isTrue()
        }

    @Test
    fun state_showsHouseholdUpdates_whenTheStreamEmitsAgain() =
        runTest {
            households.households.value = listOf(household(id = "own"), household(id = "joined", ownerId = OTHER_PROFILE.id))
            val viewModel = viewModel()

            households.households.value = listOf(household(id = "own"))

            assertThat(
                viewModel
                    .state()
                    .value.households
                    .map { it.id },
            ).containsExactly("own")
        }

    @Test
    fun addMemberToHousehold_addsTypedKeyAndClosesDialog_whenUserIsFound() =
        runTest {
            val viewModel = viewModel()

            viewModel.showAddMemberToHouseholdDialog(householdId = "household-1")
            assertThat(viewModel.state().value.dialogState)
                .isEqualTo(HouseholdManagementScreenState.DialogState.AddMemberToHousehold(householdId = "household-1"))
            viewModel.onDialogJoinKeyInputChanged(" ABC-123 ")
            assertThat(viewModel.dialogInput().value).isEqualTo(" ABC-123 ")

            viewModel.addMemberToHousehold(householdId = "household-1")

            // The key is normalized by the repository, so the ViewModel forwards it unchanged.
            assertThat(households.addedMembers).containsExactly("household-1" to " ABC-123 ")
            val state = viewModel.state().value
            assertThat(state.dialogState).isEqualTo(HouseholdManagementScreenState.DialogState.Hidden)
            assertThat(state.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
            assertThat(viewModel.dialogInput().value).isEqualTo("")
        }

    @Test
    fun removeMemberFromHousehold_removesTheMemberAndClosesDialog_whenOwnerRemovesThem() =
        runTest {
            households.households.value = listOf(sharedHousehold(ownerId = TEST_USER.uid))
            val viewModel = viewModel()
            val household = viewModel.presentedHousehold()

            viewModel.showRemoveMemberFromHouseholdDialog(household = household, member = household.members.single { it.id == "user-2" })
            assertThat(viewModel.state().value.dialogState).isEqualTo(
                HouseholdManagementScreenState.DialogState.RemoveMemberFromHousehold(
                    householdId = "household-1",
                    memberId = "user-2",
                    householdOwnerId = TEST_USER.uid,
                    householdName = "Home",
                    memberName = "Ivo",
                ),
            )

            viewModel.removeMemberFromHousehold(householdId = "household-1", memberId = "user-2", householdOwnerId = TEST_USER.uid)

            assertThat(households.removedMembers).containsExactly("user-2")
            assertThat(viewModel.presentedHousehold().members.map { it.id }).containsExactly(TEST_USER.uid)
            assertThat(viewModel.state().value.dialogState).isEqualTo(HouseholdManagementScreenState.DialogState.Hidden)
            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
        }

    @Test
    fun removeMemberFromHousehold_removesCurrentUserAndHidesTheHousehold_whenMemberLeavesIt() =
        runTest {
            households.households.value = listOf(sharedHousehold(ownerId = OTHER_PROFILE.id))
            val viewModel = viewModel()

            viewModel.removeMemberFromHousehold(householdId = "household-1", memberId = TEST_USER.uid, householdOwnerId = OTHER_PROFILE.id)

            assertThat(households.removedMembers).containsExactly(TEST_USER.uid)
            assertThat(viewModel.state().value.households).isEmpty()
            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
        }

    @Test
    fun deleteHousehold_deletesTheHouseholdAndClosesDialog_whenOwnerDeletesIt() =
        runTest {
            val viewModel = viewModel()

            viewModel.showDeleteHouseholdDialog(household = viewModel.presentedHousehold())
            assertThat(viewModel.state().value.dialogState).isEqualTo(
                HouseholdManagementScreenState.DialogState.DeleteHousehold(
                    householdId = "household-1",
                    householdOwnerId = TEST_USER.uid,
                    householdName = "Home",
                ),
            )

            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = TEST_USER.uid)

            assertThat(households.deletedHouseholds).containsExactly("household-1")
            assertThat(viewModel.state().value.households).isEmpty()
            assertThat(viewModel.state().value.dialogState).isEqualTo(HouseholdManagementScreenState.DialogState.Hidden)
            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
        }

    @Test
    fun dismissDialog_hidesDialogAndClearsTypedKey() =
        runTest {
            val viewModel = viewModel()
            viewModel.showAddMemberToHouseholdDialog(householdId = "household-1")
            viewModel.onDialogJoinKeyInputChanged("abc-123")

            viewModel.dismissDialog()

            assertThat(viewModel.state().value.dialogState).isEqualTo(HouseholdManagementScreenState.DialogState.Hidden)
            assertThat(viewModel.dialogInput().value).isEqualTo("")
            assertThat(households.addedMembers).isEmpty()
        }

    // region Error handling

    @Test
    fun failedStream_setsOnlyQuotaFallback_whenQuotaIsExceeded() =
        runTest {
            val failure = DataException(DataException.Reason.QuotaExceeded, "quota")
            households.householdsFailure = failure

            val state = viewModel().state().value

            assertThat(state.isQuotaExceeded).isTrue()
            // The fallback replaces the snackbar.
            assertThat(state.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
            assertThat(logger.warnings).isEmpty()
        }

    @Test
    fun failedStream_setsGenericErrorWithoutQuotaFlag_whenFailureIsNotQuota() =
        runTest {
            households.householdsFailure = IllegalStateException("listener failed")

            val state = viewModel().state().value

            assertThat(state.isQuotaExceeded).isFalse()
            assertThat(state.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.Generic)
        }

    @Test
    fun retry_restoresHouseholdsAndDialogs_afterQuotaStreamFailure() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            households.householdsFailure = null
            viewModel.retry()
            // The dialog state is part of the restarted stream, so it must keep working after the retry.
            viewModel.showAddMemberToHouseholdDialog(householdId = "household-1")

            val state = viewModel.state().value
            assertThat(state.isQuotaExceeded).isFalse()
            assertThat(state.households.map { it.id }).containsExactly("household-1")
            assertThat(state.dialogState)
                .isEqualTo(HouseholdManagementScreenState.DialogState.AddMemberToHousehold(householdId = "household-1"))
        }

    @Test
    fun addMemberToHousehold_setsMatchingErrorAndDismissesDialog_whenMemberIsNotAdded() =
        runTest {
            val expectedErrors =
                mapOf(
                    AddMemberResult.KeyNotFound to HouseholdManagementScreenState.ErrorState.UserNotFound,
                    AddMemberResult.OwnKey to HouseholdManagementScreenState.ErrorState.OwnJoinKey,
                    AddMemberResult.AlreadyMember to HouseholdManagementScreenState.ErrorState.AlreadyMember,
                    AddMemberResult.HouseholdFull to HouseholdManagementScreenState.ErrorState.HouseholdFull,
                )

            val states =
                expectedErrors.keys.associateWith { result ->
                    households.addMemberResult = result
                    val viewModel = viewModel()
                    viewModel.showAddMemberToHouseholdDialog(householdId = "household-1")
                    viewModel.onDialogJoinKeyInputChanged("abc-123")

                    viewModel.addMemberToHousehold(householdId = "household-1")

                    Triple(viewModel.state().value.errorState, viewModel.state().value.dialogState, viewModel.dialogInput().value)
                }

            assertThat(states).isEqualTo(
                expectedErrors.mapValues { (_, error) -> Triple(error, HouseholdManagementScreenState.DialogState.Hidden, "") },
            )
        }

    @Test
    fun addMemberToHousehold_doesNotLookUpAUser_whenNoKeyWasTyped() =
        runTest {
            val viewModel = viewModel()
            viewModel.showAddMemberToHouseholdDialog(householdId = "household-1")

            viewModel.addMemberToHousehold(householdId = "household-1")

            assertThat(households.addedMembers).isEmpty()
            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
            assertThat(viewModel.state().value.dialogState).isEqualTo(HouseholdManagementScreenState.DialogState.Hidden)
        }

    @Test
    fun removeMemberFromHousehold_setsOwnerOnlyError_whenMemberRemovesSomeoneElse() =
        runTest {
            val viewModel = viewModel()

            viewModel.removeMemberFromHousehold(householdId = "household-1", memberId = "user-3", householdOwnerId = OTHER_PROFILE.id)

            assertThat(households.removedMembers).isEmpty()
            assertThat(viewModel.state().value.errorState)
                .isEqualTo(HouseholdManagementScreenState.ErrorState.OnlyOwnerCanRemoveOtherMembers)
        }

    @Test
    fun deleteHousehold_setsOwnerOnlyError_whenUserIsNotTheOwner() =
        runTest {
            val viewModel = viewModel()

            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = OTHER_PROFILE.id)

            assertThat(households.deletedHouseholds).isEmpty()
            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.OnlyOwnerCanDeleteHousehold)
        }

    @Test
    fun deleteHousehold_setsQuotaExceededError_untilItIsShown() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = TEST_USER.uid)

            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.QuotaExceeded)
            // A failed write must not replace the household list.
            assertThat(viewModel.state().value.isQuotaExceeded).isFalse()

            viewModel.onErrorShown()

            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.None)
        }

    @Test
    fun deleteHousehold_setsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            val viewModel = viewModel()

            viewModel.deleteHousehold(householdId = "household-1", householdOwnerId = TEST_USER.uid)

            assertThat(viewModel.state().value.errorState).isEqualTo(HouseholdManagementScreenState.ErrorState.Generic)
        }

    // endregion
}

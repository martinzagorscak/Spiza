package com.mzagorscak.spiza.ui.screens.householdManagement

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.crash.isQuotaExceeded
import com.mzagorscak.spiza.domain.usecase.AddMemberToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanAddMemberToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCase
import com.mzagorscak.spiza.domain.usecase.RemoveMemberFromHouseholdUseCase
import com.mzagorscak.spiza.ui.model.PresentableHousehold
import com.mzagorscak.spiza.ui.model.PresentableHouseholdMember
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

private const val EMPTY = ""
private const val INITIAL_COUNT = 0

internal class HouseholdManagementViewModelImpl(
    getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    observeHouseholds: ObserveHouseholdsUseCase,
    private val addMemberToHousehold: AddMemberToHouseholdUseCase,
    private val removeMemberFromHouseholdUseCase: RemoveMemberFromHouseholdUseCase,
    private val deleteHouseholdUseCase: DeleteHouseholdUseCase,
    private val canUserCreateHousehold: CanUserCreateHouseholdUseCase,
    private val canAddMemberToHousehold: CanAddMemberToHouseholdUseCase,
    private val crashLogger: CrashLogger,
) : HouseholdManagementViewModel() {
    private val state = MutableStateFlow(HouseholdManagementScreenState())
    private val currentUserId = getCurrentUserProfileUseCase()?.id
    private val dialogStatePublisher =
        MutableStateFlow<HouseholdManagementScreenState.DialogState>(HouseholdManagementScreenState.DialogState.Hidden)
    private val joinKeyInputPublisher = MutableStateFlow(EMPTY)
    private val retryTrigger = MutableStateFlow(INITIAL_COUNT)

    init {
        observeState(observeHouseholds).launchIn(viewModelScope)
    }

    override fun state(): StateFlow<HouseholdManagementScreenState> = state.asStateFlow()

    override fun retry() {
        retryTrigger.update { it + 1 }
    }

    override fun dialogInput(): StateFlow<String> = joinKeyInputPublisher.asStateFlow()

    override fun showAddMemberToHouseholdDialog(householdId: String) =
        dialogStatePublisher.update {
            HouseholdManagementScreenState.DialogState.AddMemberToHousehold(householdId = householdId)
        }

    override fun addMemberToHousehold(householdId: String) {
        launchAction(context = "Adding household member failed") {
            if (joinKeyInputPublisher.value.isEmpty()) return@launchAction
            val isSuccess = addMemberToHousehold(
                householdId = householdId,
                userJoinKey = joinKeyInputPublisher.value,
            )
            if (isSuccess.not()) {
                updateErrorState(HouseholdManagementScreenState.ErrorState.UserNotFound)
            }
        }
        dismissDialog()
    }

    override fun showRemoveMemberFromHouseholdDialog(
        household: PresentableHousehold,
        member: PresentableHouseholdMember,
    ) {
        dialogStatePublisher.update {
            HouseholdManagementScreenState.DialogState.RemoveMemberFromHousehold(
                householdId = household.id,
                memberId = member.id,
                householdOwnerId = household.ownerId,
                householdName = household.name,
                memberName = member.name.toString(),
            )
        }
    }

    override fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
        householdOwnerId: String,
    ) {
        launchAction(context = "Removing household member failed") {
            val isSuccess = removeMemberFromHouseholdUseCase(
                householdId = householdId,
                memberId = memberId,
                isCurrentUserOwner = currentUserId == householdOwnerId,
                isCurrentUserRemovingHimself = currentUserId == memberId,
            )
            if (isSuccess.not()) {
                updateErrorState(HouseholdManagementScreenState.ErrorState.OnlyOwnerCanRemoveOtherMembers)
            }
        }
        dismissDialog()
    }

    override fun showDeleteHouseholdDialog(household: PresentableHousehold) {
        dialogStatePublisher.update {
            HouseholdManagementScreenState.DialogState.DeleteHousehold(
                householdId = household.id,
                householdOwnerId = household.ownerId,
                householdName = household.name,
            )
        }
    }

    override fun deleteHousehold(
        householdId: String,
        householdOwnerId: String,
    ) {
        launchAction(context = "Deleting household failed") {
            val isSuccess = deleteHouseholdUseCase(householdId, currentUserId == householdOwnerId)
            if (isSuccess.not()) {
                updateErrorState(HouseholdManagementScreenState.ErrorState.OnlyOwnerCanDeleteHousehold)
            }
        }
        dismissDialog()
    }

    override fun onDialogJoinKeyInputChanged(input: String) {
        joinKeyInputPublisher.update { input }
    }

    override fun dismissDialog() {
        dialogStatePublisher.update { HouseholdManagementScreenState.DialogState.Hidden }
        joinKeyInputPublisher.update { EMPTY }
    }

    private fun updateErrorState(errorState: HouseholdManagementScreenState.ErrorState) {
        state.update { it.copy(errorState = errorState) }
    }

    override fun onErrorShown() {
        state.update { it.copy(errorState = HouseholdManagementScreenState.ErrorState.None) }
    }

    // A failed stream completes, so it is restarted by retry(); the dialog state is part of it and keeps working afterwards.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeState(observeHouseholds: ObserveHouseholdsUseCase): Flow<Unit> =
        retryTrigger.flatMapLatest {
            combine(
                observeHouseholds(),
                dialogStatePublisher,
            ) { households, dialogState ->
                state.update {
                    it.copy(
                        households = households.map { household ->
                            household.toPresentable(
                                currentUserId = currentUserId,
                                canAddMemberToHousehold = canAddMemberToHousehold(memberCount = household.members.count()),
                            )
                        },
                        canUserCreateHousehold = canUserCreateHousehold(householdCount = households.count()),
                        dialogState = dialogState,
                        isQuotaExceeded = false,
                    )
                }
            }.catchAndRecord(crashLogger = crashLogger, context = "Household management stream failed") { error ->
                // The quota fallback already explains it, so a snackbar on top would repeat the message.
                if (error.isQuotaExceeded()) {
                    state.update { it.copy(isQuotaExceeded = true) }
                } else {
                    updateErrorState(HouseholdManagementScreenState.ErrorState.Generic)
                }
            }
        }

    private fun launchAction(
        context: String,
        block: suspend () -> Unit,
    ) {
        launchSafely(
            crashLogger = crashLogger,
            context = context,
            onError = { error -> updateErrorState(error.toErrorState()) },
        ) { block() }
    }
}

private fun Throwable.toErrorState(): HouseholdManagementScreenState.ErrorState =
    if (isQuotaExceeded()) {
        HouseholdManagementScreenState.ErrorState.QuotaExceeded
    } else {
        HouseholdManagementScreenState.ErrorState.Generic
    }

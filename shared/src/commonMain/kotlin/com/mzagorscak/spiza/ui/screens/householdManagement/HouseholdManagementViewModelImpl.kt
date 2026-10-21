package com.mzagorscak.spiza.ui.screens.householdManagement

import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val EMPTY = ""

internal class HouseholdManagementViewModelImpl(
    getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    observeHouseholds: ObserveHouseholdsUseCase,
    private val addMemberToHousehold: AddMemberToHouseholdUseCase,
    private val removeMemberFromHouseholdUseCase: RemoveMemberFromHouseholdUseCase,
    private val deleteHouseholdUseCase: DeleteHouseholdUseCase,
    private val canUserCreateHousehold: CanUserCreateHouseholdUseCase,
    private val canAddMemberToHousehold: CanAddMemberToHouseholdUseCase,
) : HouseholdManagementViewModel() {
    private val state = MutableStateFlow(HouseholdManagementScreenState())
    private val currentUserId = getCurrentUserProfileUseCase()?.id
    private val dialogStatePublisher =
        MutableStateFlow<HouseholdManagementScreenState.DialogState>(HouseholdManagementScreenState.DialogState.Hidden)
    private val joinKeyInputPublisher = MutableStateFlow(EMPTY)

    init {
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
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun state(): StateFlow<HouseholdManagementScreenState> = state.asStateFlow()

    override fun dialogInput(): StateFlow<String> = joinKeyInputPublisher.asStateFlow()

    override fun showAddMemberToHouseholdDialog(householdId: String) =
        dialogStatePublisher.update {
            HouseholdManagementScreenState.DialogState.AddMemberToHousehold(householdId = householdId)
        }

    override fun addMemberToHousehold(householdId: String) {
        viewModelScope.launch {
            if (joinKeyInputPublisher.value.isEmpty()) return@launch
            addMemberToHousehold(
                householdId = householdId,
                userJoinKey = joinKeyInputPublisher.value,
            )
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
        viewModelScope.launch {
            removeMemberFromHouseholdUseCase(
                householdId = householdId,
                memberId = memberId,
                isCurrentUserOwner = currentUserId == householdOwnerId,
                isCurrentUserRemovingHimself = currentUserId == memberId,
            )
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
        viewModelScope.launch {
            deleteHouseholdUseCase(householdId, currentUserId == householdOwnerId)
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
}

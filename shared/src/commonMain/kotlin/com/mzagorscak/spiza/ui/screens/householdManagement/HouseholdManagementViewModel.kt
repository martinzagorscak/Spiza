package com.mzagorscak.spiza.ui.screens.householdManagement

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableHousehold
import com.mzagorscak.spiza.ui.model.PresentableHouseholdMember
import kotlinx.coroutines.flow.StateFlow

data class HouseholdManagementScreenState(
    val households: List<PresentableHousehold> = emptyList(),
    val canUserCreateHousehold: Boolean = false,
    val dialogState: DialogState = DialogState.Hidden,
    val errorState: ErrorState = ErrorState.None,
) {
    sealed class DialogState {
        object Hidden : DialogState()

        data class DeleteHousehold(
            val householdId: String,
            val householdOwnerId: String,
            val householdName: String,
        ) : DialogState()

        data class AddMemberToHousehold(
            val householdId: String,
        ) : DialogState()

        data class RemoveMemberFromHousehold(
            val householdId: String,
            val memberId: String,
            val householdOwnerId: String,
            val householdName: String,
            val memberName: String,
        ) : DialogState()
    }

    enum class ErrorState {
        None,
        UserNotFound,
        OnlyOwnerCanRemoveOtherMembers,
        OnlyOwnerCanDeleteHousehold,
        Generic,
    }
}

abstract class HouseholdManagementViewModel : ViewModel() {
    abstract fun state(): StateFlow<HouseholdManagementScreenState>

    abstract fun dialogInput(): StateFlow<String>

    abstract fun showAddMemberToHouseholdDialog(householdId: String)

    /**
     * Owner can add members to the household, but members cannot add other members to the household.
     */
    abstract fun addMemberToHousehold(householdId: String)

    abstract fun showRemoveMemberFromHouseholdDialog(
        household: PresentableHousehold,
        member: PresentableHouseholdMember,
    )

    /**
     * Owner can remove any member from the household, but members can only remove themselves from the household.
     */
    abstract fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
        householdOwnerId: String,
    )

    abstract fun showDeleteHouseholdDialog(household: PresentableHousehold)

    /**
     * Owner can delete the household, but members can only leave the household.
     */
    abstract fun deleteHousehold(
        householdId: String,
        householdOwnerId: String,
    )

    abstract fun onDialogJoinKeyInputChanged(input: String)

    abstract fun dismissDialog()

    abstract fun onErrorShown()
}

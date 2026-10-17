package com.mzagorscak.spiza.ui.screens.householdManagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.HouseholdCard
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.model.PresentableHousehold
import com.mzagorscak.spiza.ui.model.PresentableHouseholdMember
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.cancel
import spiza.shared.generated.resources.delete
import spiza.shared.generated.resources.household_management_add_member_dialog_confirm_label
import spiza.shared.generated.resources.household_management_add_member_dialog_description
import spiza.shared.generated.resources.household_management_add_member_dialog_title
import spiza.shared.generated.resources.household_management_delete_household_dialog_description
import spiza.shared.generated.resources.household_management_delete_household_dialog_title_template
import spiza.shared.generated.resources.household_management_remove_member_dialog_description
import spiza.shared.generated.resources.household_management_remove_member_dialog_title_template
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.ic_trash
import spiza.shared.generated.resources.join_key
import spiza.shared.generated.resources.remove
import spiza.shared.generated.resources.settings_household_management
import spiza.shared.generated.resources.shopping_list_no_household_description
import spiza.shared.generated.resources.shopping_list_no_household_title

private val MinHouseholdCardHeight = 100.dp
private val MaxHouseholdCardHeight = 500.dp

@Composable
fun HouseholdManagementScreen(
    state: HouseholdManagementScreenState,
    dialogInput: String,
    callbacks: HouseholdManagementScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.settings_household_management),
                leadingContent = {
                    IconButton(
                        resource = Res.drawable.ic_back,
                        onClick = callbacks.onBackClick,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            IconButton(
                resource = Res.drawable.ic_plus,
                onClick = callbacks.onCreateHousehold,
                appearance = IconButtonAppearance(
                    variant = IconButtonVariant.IconBased,
                    size = IconButtonSize.Large,
                    tintColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.households.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = padding400, vertical = padding400),
                    verticalArrangement = Arrangement.spacedBy(padding200),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.households) { household ->
                        HouseholdCard(
                            household = household,
                            nameTrailingContent = {
                                if (household.isCurrentUserOwner) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            resource = Res.drawable.ic_plus,
                                            onClick = { callbacks.showAddMemberToHouseholdsDialog(household.id) },
                                            appearance = IconButtonAppearance(
                                                variant = IconButtonVariant.IconBased,
                                                size = IconButtonSize.Medium,
                                                tintColor = MaterialTheme.colorScheme.background,
                                            ),
                                        )

                                        Spacer(modifier = Modifier.width(padding100))

                                        IconButton(
                                            resource = Res.drawable.ic_trash,
                                            onClick = {
                                                callbacks.showDeleteHouseholdDialog(
                                                    household.id,
                                                    household.ownerId,
                                                    household.name,
                                                )
                                            },
                                            appearance = IconButtonAppearance(
                                                variant = IconButtonVariant.IconBased,
                                                size = IconButtonSize.Medium,
                                                tintColor = MaterialTheme.colorScheme.background,
                                            ),
                                        )
                                    }
                                }
                            },
                            onRemoveUserClick = callbacks.showRemoveMemberFromHouseholdDialog,
                            modifier = Modifier.heightIn(
                                min = MinHouseholdCardHeight,
                                max = MaxHouseholdCardHeight,
                            ),
                        )
                    }
                }
            } else {
                FallbackState(
                    title = stringResource(Res.string.shopping_list_no_household_title),
                    description = stringResource(Res.string.shopping_list_no_household_description),
                )
            }
        }

        when (state.dialogState) {
            is HouseholdManagementScreenState.DialogState.AddMemberToHousehold -> {
                Dialog(
                    title = stringResource(Res.string.household_management_add_member_dialog_title),
                    description = stringResource(Res.string.household_management_add_member_dialog_description),
                    textInput = dialogInput,
                    onTextInputChange = callbacks.onDialogJoinKeyInputChanged,
                    label = stringResource(Res.string.join_key),
                    trailingIcon = {
                        IconButton(
                            resource = Res.drawable.ic_close,
                            onClick = { callbacks.onDialogJoinKeyInputChanged("") },
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                                tintColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    dismissCTA = CTA(
                        label = stringResource(Res.string.cancel),
                        onClick = callbacks.onDismissDialog,
                    ),
                    confirmCTA = CTA(
                        label = stringResource(Res.string.household_management_add_member_dialog_confirm_label),
                        onClick = { callbacks.onAddMember(state.dialogState.householdId) },
                        isEnabled = dialogInput.isNotBlank(),
                    ),
                )
            }

            is HouseholdManagementScreenState.DialogState.DeleteHousehold -> {
                Dialog(
                    title = stringResource(
                        Res.string.household_management_delete_household_dialog_title_template,
                        state.dialogState.householdName,
                    ),
                    description = stringResource(Res.string.household_management_delete_household_dialog_description),
                    dismissCTA = CTA(
                        label = stringResource(Res.string.cancel),
                        onClick = callbacks.onDismissDialog,
                    ),
                    confirmCTA = CTA(
                        label = stringResource(Res.string.delete),
                        onClick = { callbacks.onDeleteHousehold(state.dialogState.householdId, state.dialogState.householdOwnerId) },
                    ),
                )
            }

            is HouseholdManagementScreenState.DialogState.RemoveMemberFromHousehold -> {
                Dialog(
                    title = stringResource(
                        Res.string.household_management_remove_member_dialog_title_template,
                        state.dialogState.memberName,
                        state.dialogState.householdName,
                    ),
                    description = stringResource(Res.string.household_management_remove_member_dialog_description),
                    dismissCTA = CTA(
                        label = stringResource(Res.string.cancel),
                        onClick = callbacks.onDismissDialog,
                    ),
                    confirmCTA = CTA(
                        label = stringResource(Res.string.remove),
                        onClick = {
                            callbacks.onRemoveMember(
                                state.dialogState.householdId,
                                state.dialogState.memberId,
                                state.dialogState.householdOwnerId,
                            )
                        },
                    ),
                )
            }

            HouseholdManagementScreenState.DialogState.Hidden -> Unit
        }
    }
}

data class HouseholdManagementScreenCallbacks(
    val onBackClick: () -> Unit,
    val onCreateHousehold: () -> Unit,
    val onAddMember: (joinKey: String) -> Unit,
    val onRemoveMember: (householdId: String, memberId: String, householdOwnerId: String) -> Unit,
    val onDeleteHousehold: (String, String) -> Unit,
    val showAddMemberToHouseholdsDialog: (String) -> Unit,
    val showRemoveMemberFromHouseholdDialog: (household: PresentableHousehold, member: PresentableHouseholdMember) -> Unit,
    val showDeleteHouseholdDialog: (householdId: String, householdOwnerId: String, householdName: String) -> Unit,
    val onDialogJoinKeyInputChanged: (String) -> Unit,
    val onDismissDialog: () -> Unit,
)

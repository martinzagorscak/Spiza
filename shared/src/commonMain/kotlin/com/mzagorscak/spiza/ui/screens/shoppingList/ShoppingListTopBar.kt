package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.animatable.SettingsIconButton
import com.mzagorscak.spiza.ui.components.animatable.animatedicons.rememberAnimatedChevronPainter
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.shopping_list_create_household
import spiza.shared.generated.resources.shopping_list_default_title

@Composable
fun ShoppingListTopBar(
    headerState: ShoppingListState.Header,
    callbacks: ShoppingListScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    TopBar(
        title = headerState.currentHouseholdName ?: stringResource(Res.string.shopping_list_default_title),
        leadingContent = {
            if (headerState.otherHouseholds.isNotEmpty() || headerState.canUserCreateHousehold) {
                var isDropdownExpanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        painter = rememberAnimatedChevronPainter(isExpanded = isDropdownExpanded.not()),
                        onClick = { isDropdownExpanded = true },
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        headerState.otherHouseholds.forEach { household ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = household.name,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                onClick = {
                                    isDropdownExpanded = false
                                    callbacks.onOtherHouseholdClick(household.id)
                                },
                            )
                        }
                        if (headerState.canUserCreateHousehold) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(Res.string.shopping_list_create_household),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                onClick = {
                                    isDropdownExpanded = false
                                    callbacks.onCreateHouseholdClick()
                                },
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            SettingsIconButton(
                onClick = callbacks.onSettingsClick,
                tintColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun ShoppingListTopBarLayoutPreview() {
    ShoppingListTopBar(
        headerState = ShoppingListState.Header(
            currentHouseholdName = "My Household",
            otherHouseholds = emptyList(),
        ),
        callbacks = ShoppingListScreenCallbacks(
            onSettingsClick = {},
            onTabClick = {},
            onOtherHouseholdClick = {},
            onCreateHouseholdClick = {},
            onRetryClick = {},
            onAddItem = {},
            onItemTextChange = { _, _ -> },
            onItemCheckedChange = { _, _ -> },
            onItemDelete = {},
            onItemMoved = { _, _ -> },
            onCheckedItemsCollapse = {},
            showClearCheckedItemsConfirmationDialog = {},
            onDismissDialog = {},
            clearCheckedItems = {},
            onWriteErrorShown = {},
            onLockToggle = {},
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ShoppingListTopBarPreviewLightMode() {
    TheAppTheme {
        ShoppingListTopBarLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShoppingListTopBarPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        ShoppingListTopBarLayoutPreview()
    }
}

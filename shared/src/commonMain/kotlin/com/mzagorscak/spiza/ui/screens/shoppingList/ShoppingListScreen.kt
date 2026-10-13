package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_dropdown
import spiza.shared.generated.resources.ic_settings
import spiza.shared.generated.resources.shopping_list_default_title

@Composable
fun ShoppingListScreen(
    headerState: ShoppingListState.Header,
    callbacks: ShoppingListScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = headerState.currentHouseholdName ?: stringResource(Res.string.shopping_list_default_title),
                leadingContent = {
                    var isDropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            resource = Res.drawable.ic_dropdown,
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
                        }
                    }
                },
                trailingContent = {
                    IconButton(
                        resource = Res.drawable.ic_settings,
                        onClick = callbacks.onSettingsClick,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues))
    }
}

data class ShoppingListScreenCallbacks(
    val onSettingsClick: () -> Unit = {},
    val onOtherHouseholdClick: (householdId: String) -> Unit = { _ -> },
)

@Preview
@Composable
private fun ShoppingListScreenPreview() {
    TheAppTheme(darkTheme = true) {
        ShoppingListScreen(
            headerState = ShoppingListState.Header(
                currentHouseholdName = "My Household",
                otherHouseholds = listOf(
                    PresentableSimpleHousehold(id = "1", name = "Household 1"),
                    PresentableSimpleHousehold(id = "2", name = "Household 2"),
                ),
            ),
            callbacks = ShoppingListScreenCallbacks(),
        )
    }
}

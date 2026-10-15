package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.LoadingIndicator
import com.mzagorscak.spiza.ui.components.LoadingIndicatorSize
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.model.PresentableArticleItem
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_filled_chevron_down
import spiza.shared.generated.resources.ic_settings
import spiza.shared.generated.resources.shopping_list_create_household
import spiza.shared.generated.resources.shopping_list_default_title
import spiza.shared.generated.resources.shopping_list_loading_error_description
import spiza.shared.generated.resources.shopping_list_loading_error_title
import spiza.shared.generated.resources.shopping_list_no_household_description
import spiza.shared.generated.resources.shopping_list_no_household_title
import spiza.shared.generated.resources.shopping_list_retry
import spiza.shared.generated.resources.shopping_list_write_error

private const val ERROR_LOTTIE_PATH = "files/lottie_error.json"

@Composable
fun ShoppingListScreen(
    headerState: ShoppingListState.Header,
    bodyState: ShoppingListState.Body,
    callbacks: ShoppingListScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val writeErrorMessage = stringResource(Res.string.shopping_list_write_error)
    val hasWriteError = (bodyState as? ShoppingListState.Body.Content)?.hasWriteError == true
    LaunchedEffect(hasWriteError) {
        if (hasWriteError) {
            callbacks.onWriteErrorShown()
            snackbarHostState.showSnackbar(writeErrorMessage)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = headerState.currentHouseholdName ?: stringResource(Res.string.shopping_list_default_title),
                leadingContent = {
                    var isDropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            resource = Res.drawable.ic_filled_chevron_down,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (bodyState) {
                ShoppingListState.Body.Loading -> {
                    LoadingIndicator(
                        size = LoadingIndicatorSize.Large,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                ShoppingListState.Body.Error -> {
                    FallbackState(
                        title = stringResource(Res.string.shopping_list_loading_error_title),
                        description = stringResource(Res.string.shopping_list_loading_error_description),
                        lottiePath = ERROR_LOTTIE_PATH,
                        cta = CTA(
                            label = stringResource(Res.string.shopping_list_retry),
                            onClick = callbacks.onRetryClick,
                        ),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                ShoppingListState.Body.NoHousehold -> {
                    FallbackState(
                        title = stringResource(Res.string.shopping_list_no_household_title),
                        description = stringResource(Res.string.shopping_list_no_household_description),
                        cta = CTA(
                            label = stringResource(Res.string.shopping_list_create_household),
                            onClick = callbacks.onCreateHouseholdClick,
                        ),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ShoppingListState.Body.Content -> {
                    ShoppingListContent(
                        state = bodyState,
                        callbacks = callbacks,
                    )
                }
            }
        }
    }
}

data class ShoppingListScreenCallbacks(
    val onSettingsClick: () -> Unit = {},
    val onOtherHouseholdClick: (householdId: String) -> Unit = { _ -> },
    val onCreateHouseholdClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
    val onAddItem: (text: String) -> Unit = { _ -> },
    val onItemTextChange: (itemId: String, text: String) -> Unit = { _, _ -> },
    val onItemCheckedChange: (itemId: String, isChecked: Boolean) -> Unit = { _, _ -> },
    val onItemMoved: (itemId: String, toIndex: Int) -> Unit = { _, _ -> },
    val onCheckedItemsCollapse: () -> Unit = {},
    val clearCheckedItems: () -> Unit = {},
    val onWriteErrorShown: () -> Unit = {},
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
            bodyState = ShoppingListState.Body.Content(
                pendingItems = listOf(
                    PresentableArticleItem(id = "1", text = "Milk", isChecked = false),
                    PresentableArticleItem(id = "2", text = "Eggs", isChecked = false),
                ),
                doneItems = listOf(PresentableArticleItem(id = "3", text = "Bread", isChecked = true)),
                areCheckedItemsCollapsed = false,
            ),
            callbacks = ShoppingListScreenCallbacks(),
        )
    }
}

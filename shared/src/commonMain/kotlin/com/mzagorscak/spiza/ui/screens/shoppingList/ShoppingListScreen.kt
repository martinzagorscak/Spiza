package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.LoadingIndicator
import com.mzagorscak.spiza.ui.components.LoadingIndicatorSize
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.model.PresentableArticleItem
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.cancel
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.shopping_list_clear_checked_items_dialog_confirm
import spiza.shared.generated.resources.shopping_list_clear_checked_items_dialog_description
import spiza.shared.generated.resources.shopping_list_clear_checked_items_dialog_title
import spiza.shared.generated.resources.shopping_list_create_household
import spiza.shared.generated.resources.shopping_list_loading_error_description
import spiza.shared.generated.resources.shopping_list_loading_error_title
import spiza.shared.generated.resources.shopping_list_no_household_description
import spiza.shared.generated.resources.shopping_list_no_household_title
import spiza.shared.generated.resources.shopping_list_retry
import spiza.shared.generated.resources.shopping_list_write_error

private const val ERROR_LOTTIE_PATH = "files/lottie_error.json"
private const val OPEN_DRAWER_FRACTION = 0.75f

@Composable
fun ShoppingListScreen(
    headerState: ShoppingListState.Header,
    bodyState: ShoppingListState.Body,
    callbacks: ShoppingListScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val suggestionsDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val writeErrorMessage = stringResource(Res.string.shopping_list_write_error)
    val hasWriteError = (bodyState as? ShoppingListState.Body.Content)?.hasWriteError == true
    LaunchedEffect(hasWriteError) {
        if (hasWriteError) {
            callbacks.onWriteErrorShown()
            snackbarHostState.showSnackbar(writeErrorMessage)
        }
    }

    ModalNavigationDrawer(
        drawerState = suggestionsDrawerState,
        drawerContent = {
            ShoppingListSuggestionsDrawer(
                suggestions = (bodyState as? ShoppingListState.Body.Content)?.suggestions ?: emptyList(),
                onAddItem = { text -> callbacks.onAddItem(text) },
                onClose = { scope.launch { suggestionsDrawerState.close() } },
                modifier = Modifier.fillMaxWidth(fraction = OPEN_DRAWER_FRACTION),
            )
        },
    ) {
        Scaffold(
            topBar = {
                ShoppingListTopBar(
                    headerState = headerState,
                    callbacks = callbacks,
                )
            },
            floatingActionButton = {
                if (bodyState as? ShoppingListState.Body.Content != null && bodyState.suggestions.isNotEmpty()) {
                    IconButton(
                        resource = Res.drawable.ic_plus,
                        onClick = {
                            scope.launch {
                                suggestionsDrawerState.open()
                            }
                        },
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Large,
                            tintColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    )
                }
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

                        when (bodyState.dialogState) {
                            ShoppingListState.DialogState.ClearCheckedItemsConfirmation -> {
                                Dialog(
                                    title = stringResource(Res.string.shopping_list_clear_checked_items_dialog_title),
                                    description = stringResource(Res.string.shopping_list_clear_checked_items_dialog_description),
                                    dismissCTA = CTA(
                                        label = stringResource(Res.string.cancel),
                                        onClick = callbacks.onDismissDialog,
                                    ),
                                    confirmCTA = CTA(
                                        label = stringResource(Res.string.shopping_list_clear_checked_items_dialog_confirm),
                                        onClick = {
                                            callbacks.clearCheckedItems()
                                            callbacks.onDismissDialog()
                                        },
                                    ),
                                )
                            }

                            ShoppingListState.DialogState.Hidden -> Unit
                        }
                    }
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
    // shows a confirmation dialog, which when confirmed calls clearCheckedItems
    val showClearCheckedItemsConfirmationDialog: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
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

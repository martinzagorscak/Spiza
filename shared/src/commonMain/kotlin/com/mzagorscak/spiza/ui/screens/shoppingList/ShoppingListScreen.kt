package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.LoadingIndicator
import com.mzagorscak.spiza.ui.components.LoadingIndicatorSize
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.components.common.performHapticFeedback
import com.mzagorscak.spiza.ui.model.PresentableArticleItem
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import com.mzagorscak.spiza.ui.screens.common.MainBottomBar
import com.mzagorscak.spiza.ui.screens.common.MainTab
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.cancel
import spiza.shared.generated.resources.error_quota_exceeded
import spiza.shared.generated.resources.error_quota_exceeded_title
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.retry
import spiza.shared.generated.resources.shopping_list_clear_checked_items_dialog_confirm
import spiza.shared.generated.resources.shopping_list_clear_checked_items_dialog_description
import spiza.shared.generated.resources.shopping_list_clear_checked_items_dialog_title
import spiza.shared.generated.resources.shopping_list_create_household
import spiza.shared.generated.resources.shopping_list_loading_error_description
import spiza.shared.generated.resources.shopping_list_loading_error_title
import spiza.shared.generated.resources.shopping_list_no_household_description
import spiza.shared.generated.resources.shopping_list_no_household_title
import spiza.shared.generated.resources.shopping_list_quota_exceeded_description
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
    val hapticFeedback = LocalHapticFeedback.current
    val suggestionsDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val areSuggestionsVisible =
        (bodyState as? ShoppingListState.Body.Content)?.canAddItemToHousehold == true && bodyState.suggestions.isNotEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val writeErrorMessage = if (bodyState is ShoppingListState.Body.Content) {
        when (bodyState.contentErrorState) {
            ShoppingListState.ContentErrorState.WriteError -> stringResource(Res.string.shopping_list_write_error)
            ShoppingListState.ContentErrorState.QuotaExceeded -> stringResource(Res.string.error_quota_exceeded)
            ShoppingListState.ContentErrorState.None -> null
        }
    } else {
        null
    }
    // Keyed on the enum so a different error replacing the current one is shown too.
    val contentErrorState = (bodyState as? ShoppingListState.Body.Content)?.contentErrorState
    LaunchedEffect(contentErrorState) {
        writeErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            callbacks.onWriteErrorShown()
        }
    }

    ModalNavigationDrawer(
        drawerState = suggestionsDrawerState,
        drawerContent = {
            ShoppingListSuggestionsDrawer(
                areSuggestionsEnabled = areSuggestionsVisible,
                suggestions = (bodyState as? ShoppingListState.Body.Content)?.suggestions ?: emptyList(),
                onAddItem = { text ->
                    performHapticFeedback(hapticFeedback)
                    callbacks.onAddItem(text)
                },
                onClose = {
                    performHapticFeedback(hapticFeedback)
                    scope.launch { suggestionsDrawerState.close() }
                },
                modifier = Modifier.fillMaxWidth(fraction = OPEN_DRAWER_FRACTION),
            )
        },
        gesturesEnabled = areSuggestionsVisible,
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
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                performHapticFeedback(hapticFeedback)
                                suggestionsDrawerState.open()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_plus),
                            contentDescription = null,
                        )
                    }
                }
            },
            bottomBar = {
                MainBottomBar(
                    selectedTab = MainTab.ShoppingList,
                    onTabClick = callbacks.onTabClick,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            modifier = modifier.fillMaxSize(),
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding(),
            ) {
                when (bodyState) {
                    ShoppingListState.Body.Loading -> {
                        LoadingIndicator(
                            size = LoadingIndicatorSize.Large,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    is ShoppingListState.Body.Error -> {
                        val isQuotaExceeded = bodyState.errorType == ShoppingListState.ErrorType.QuotaExceeded
                        FallbackState(
                            title = stringResource(
                                if (isQuotaExceeded) {
                                    Res.string.error_quota_exceeded_title
                                } else {
                                    Res.string.shopping_list_loading_error_title
                                },
                            ),
                            description = stringResource(
                                if (isQuotaExceeded) {
                                    Res.string.shopping_list_quota_exceeded_description
                                } else {
                                    Res.string.shopping_list_loading_error_description
                                },
                            ),
                            lottiePath = ERROR_LOTTIE_PATH,
                            cta = CTA(
                                label = stringResource(Res.string.retry),
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
                                        onClick = callbacks.clearCheckedItems,
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
    val onSettingsClick: () -> Unit,
    val onTabClick: (MainTab) -> Unit,
    val onOtherHouseholdClick: (householdId: String) -> Unit,
    val onCreateHouseholdClick: () -> Unit,
    val onRetryClick: () -> Unit,
    val onAddItem: (text: String) -> Unit,
    val onItemTextChange: (itemId: String, text: String) -> Unit,
    val onItemCheckedChange: (itemId: String, isChecked: Boolean) -> Unit,
    val onItemMoved: (itemId: String, toIndex: Int) -> Unit,
    val onItemDelete: (String) -> Unit,
    val onCheckedItemsCollapse: () -> Unit,
    // shows a confirmation dialog, which when confirmed calls clearCheckedItems
    val showClearCheckedItemsConfirmationDialog: () -> Unit,
    val onDismissDialog: () -> Unit,
    val clearCheckedItems: () -> Unit,
    val onWriteErrorShown: () -> Unit,
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
                currentHouseholdId = "1",
                pendingItems = listOf(
                    PresentableArticleItem(id = "1", text = "Milk", isChecked = false),
                    PresentableArticleItem(id = "2", text = "Eggs", isChecked = false),
                ),
                doneItems = listOf(PresentableArticleItem(id = "3", text = "Bread", isChecked = true)),
                areCheckedItemsCollapsed = false,
            ),
            callbacks = PreviewCallbacks,
        )
    }
}

@Preview
@Composable
private fun ShoppingListScreenQuotaExceededPreview() {
    TheAppTheme {
        ShoppingListScreen(
            headerState = ShoppingListState.Header(),
            bodyState = ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.QuotaExceeded),
            callbacks = PreviewCallbacks,
        )
    }
}

private val PreviewCallbacks =
    ShoppingListScreenCallbacks(
        onSettingsClick = {},
        onTabClick = {},
        onOtherHouseholdClick = {},
        onCreateHouseholdClick = {},
        onRetryClick = {},
        onAddItem = {},
        onItemTextChange = { _, _ -> },
        onItemCheckedChange = { _, _ -> },
        onItemMoved = { _, _ -> },
        onItemDelete = {},
        onCheckedItemsCollapse = {},
        showClearCheckedItemsConfirmationDialog = {},
        onDismissDialog = {},
        clearCheckedItems = {},
        onWriteErrorShown = {},
    )

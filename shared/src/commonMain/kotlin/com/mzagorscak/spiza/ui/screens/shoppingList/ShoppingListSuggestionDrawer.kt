package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.TextChip
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding600
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.shopping_list_suggestions_title

@Composable
fun ShoppingListSuggestionsDrawer(
    suggestions: List<PresentableShoppingItemSuggestion>,
    onAddItem: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding600),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = padding200),
            ) {
                IconButton(
                    resource = Res.drawable.ic_close,
                    onClick = onClose,
                    appearance = IconButtonAppearance(
                        variant = IconButtonVariant.IconBased,
                        size = IconButtonSize.Medium,
                        tintColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Spacer(modifier = Modifier.width(padding600))
                Text(
                    text = stringResource(Res.string.shopping_list_suggestions_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f).padding(padding200),
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(padding100),
                horizontalAlignment = Alignment.Start,
                modifier = modifier.fillMaxWidth(),
            ) {
                items(suggestions) { suggestion ->
                    TextChip(
                        text = suggestion.name,
                        onClick = { onAddItem(suggestion.name) },
                        enabled = suggestion.isEnabled,
                        trailingContent = {
                            IconButton(
                                resource = Res.drawable.ic_plus,
                                onClick = { onAddItem(suggestion.name) },
                                isEnabled = suggestion.isEnabled,
                                appearance = IconButtonAppearance(
                                    variant = IconButtonVariant.IconBased,
                                    size = IconButtonSize.Small,
                                    tintColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingListSuggestionDrawerLayoutPreview() {
    ShoppingListSuggestionsDrawer(
        suggestions = listOf(
            PresentableShoppingItemSuggestion(
                id = 0,
                name = "Apples",
                isEnabled = true,
            ),
            PresentableShoppingItemSuggestion(
                id = 1,
                name = "Bananas",
                isEnabled = true,
            ),
            PresentableShoppingItemSuggestion(
                id = 2,
                name = "Carrots",
                isEnabled = false,
            ),
        ),
        onAddItem = {},
        onClose = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ShoppingListSuggestionDrawerPreviewLightMode() {
    TheAppTheme {
        ShoppingListSuggestionDrawerLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShoppingListSuggestionDrawerPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        ShoppingListSuggestionDrawerLayoutPreview()
    }
}

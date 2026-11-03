package com.mzagorscak.spiza.ui.screens.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.mzagorscak.spiza.ui.components.BottomNavigationBar
import com.mzagorscak.spiza.ui.components.BottomNavigationBarItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.bottom_navigation_loyalty_cards
import spiza.shared.generated.resources.bottom_navigation_shopping_list
import spiza.shared.generated.resources.ic_loyalty_card
import spiza.shared.generated.resources.ic_shopping_list

enum class MainTab { ShoppingList, LoyaltyCards }

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Hidden while the keyboard is open: Scaffold then pads the content with the IME insets instead of the bar height,
    // so the shopping list input is not covered by the keyboard.
    // derivedStateOf recomposes only when visibility flips, not on every frame of the keyboard animation.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isImeVisible by remember(density, imeInsets) { derivedStateOf { imeInsets.getBottom(density) > 0 } }

    AnimatedVisibility(
        visible = isImeVisible.not(),
        // No clipping, otherwise the shadow drawn above the bar's top edge is cut off.
        enter = expandVertically(clip = false),
        exit = shrinkVertically(clip = false),
        modifier = modifier,
    ) {
        BottomNavigationBar(
            items = MainTab.entries.map { tab ->
                BottomNavigationBarItem(
                    icon = tab.icon,
                    label = stringResource(tab.label),
                    isSelected = tab == selectedTab,
                    onClick = { if (tab != selectedTab) onTabClick(tab) },
                )
            },
        )
    }
}

private val MainTab.icon: DrawableResource
    get() = when (this) {
        MainTab.ShoppingList -> Res.drawable.ic_shopping_list
        MainTab.LoyaltyCards -> Res.drawable.ic_loyalty_card
    }

private val MainTab.label: StringResource
    get() = when (this) {
        MainTab.ShoppingList -> Res.string.bottom_navigation_shopping_list
        MainTab.LoyaltyCards -> Res.string.bottom_navigation_loyalty_cards
    }

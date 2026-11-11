package com.mzagorscak.spiza.ui.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.mzagorscak.spiza.ui.components.BottomNavigationBar
import com.mzagorscak.spiza.ui.components.BottomNavigationBarItem
import com.mzagorscak.spiza.ui.components.animatable.animatedicons.rememberAnimatedLoyaltyCardPainter
import com.mzagorscak.spiza.ui.components.animatable.animatedicons.rememberAnimatedShoppingListPainter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.bottom_navigation_loyalty_cards
import spiza.shared.generated.resources.bottom_navigation_shopping_list

enum class MainTab { ShoppingList, LoyaltyCards }

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomNavigationBar(
        items = MainTab.entries.map { tab ->
            val isSelected = tab == selectedTab
            BottomNavigationBarItem(
                icon = tab.icon(isSelected = isSelected),
                label = stringResource(tab.label),
                isSelected = isSelected,
                onClick = { if (tab != selectedTab) onTabClick(tab) },
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun MainTab.icon(isSelected: Boolean): Painter =
    when (this) {
        MainTab.ShoppingList -> rememberAnimatedShoppingListPainter(isSelected = isSelected)
        MainTab.LoyaltyCards -> rememberAnimatedLoyaltyCardPainter(isSelected = isSelected)
    }

private val MainTab.label: StringResource
    get() = when (this) {
        MainTab.ShoppingList -> Res.string.bottom_navigation_shopping_list
        MainTab.LoyaltyCards -> Res.string.bottom_navigation_loyalty_cards
    }

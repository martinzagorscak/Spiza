package com.mzagorscak.spiza.ui.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    BottomNavigationBar(
        items = MainTab.entries.map { tab ->
            BottomNavigationBarItem(
                icon = tab.icon,
                label = stringResource(tab.label),
                isSelected = tab == selectedTab,
                onClick = { if (tab != selectedTab) onTabClick(tab) },
            )
        },
        modifier = modifier,
    )
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

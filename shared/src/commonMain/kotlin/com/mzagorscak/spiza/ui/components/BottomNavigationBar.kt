package com.mzagorscak.spiza.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.bottom_navigation_loyalty_cards
import spiza.shared.generated.resources.bottom_navigation_shopping_list
import spiza.shared.generated.resources.ic_loyalty_card
import spiza.shared.generated.resources.ic_shopping_list

private const val TOP_SHADOW_ALPHA = 0.08f
private val TopShadowHeight = 8.dp

data class BottomNavigationBarItem(
    val icon: DrawableResource,
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun BottomNavigationBar(
    items: List<BottomNavigationBarItem>,
    modifier: Modifier = Modifier,
) {
    val shadowColor = MaterialTheme.colorScheme.onBackground.copy(alpha = TOP_SHADOW_ALPHA)
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = modifier.drawWithContent {
            val shadowHeight = TopShadowHeight.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, shadowColor),
                    startY = -shadowHeight,
                    endY = 0f,
                ),
                topLeft = Offset(x = 0f, y = -shadowHeight),
                size = Size(width = size.width, height = shadowHeight),
            )
            drawContent()
        },
    ) {
        items.forEach { item ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                selected = item.isSelected,
                onClick = item.onClick,
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}

@Composable
private fun BottomNavigationBarLayoutPreview() {
    BottomNavigationBar(
        items = listOf(
            BottomNavigationBarItem(
                icon = Res.drawable.ic_shopping_list,
                label = stringResource(Res.string.bottom_navigation_shopping_list),
                isSelected = true,
                onClick = {},
            ),
            BottomNavigationBarItem(
                icon = Res.drawable.ic_loyalty_card,
                label = stringResource(Res.string.bottom_navigation_loyalty_cards),
                isSelected = false,
                onClick = {},
            ),
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BottomNavigationBarPreviewLightMode() {
    TheAppTheme {
        BottomNavigationBarLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BottomNavigationBarPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        BottomNavigationBarLayoutPreview()
    }
}

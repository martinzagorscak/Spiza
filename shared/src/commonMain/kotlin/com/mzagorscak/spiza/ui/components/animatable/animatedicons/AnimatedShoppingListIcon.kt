package com.mzagorscak.spiza.ui.components.animatable.animatedicons

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding050
import com.mzagorscak.spiza.ui.theme.padding200
import kotlin.math.PI
import kotlin.math.sin

private const val ANIMATION_DURATION_MS = 500
private const val POP_SCALE = 0.2f
private const val WIGGLE_DEGREES = 12f
private const val CENTER = 12f

private val ShoppingListPath =
    addPathNodes(
        "M7,2.5H17A3,3 0,0 1,20 5.5V18.5A3,3 0,0 1,17 21.5H7A3,3 0,0 1,4 18.5V5.5A3,3 0,0 1,7 2.5Z" +
            "M7,7.25A1,1 0,1 0,9 7.25A1,1 0,1 0,7 7.25ZM10.75,6.5H16.25A0.75,0.75 0,0 1,16.25 8H10.75A0.75,0.75 0,0 1,10.75 6.5Z" +
            "M7,12A1,1 0,1 0,9 12A1,1 0,1 0,7 12ZM10.75,11.25H16.25A0.75,0.75 0,0 1,16.25 12.75H10.75A0.75,0.75 0,0 1,10.75 11.25Z" +
            "M7,16.75A1,1 0,1 0,9 16.75A1,1 0,1 0,7 16.75ZM10.75,16H16.25A0.75,0.75 0,0 1,16.25 17.5H10.75A0.75,0.75 0,0 1,10.75 16Z",
    )

/**
 * The shopping list tab icon: pops up and wiggles once when it becomes selected ([isSelected]).
 * It also plays when the tab's screen appears; losing the selection resets it at once, so only the selected tab moves.
 */
@Composable
fun rememberAnimatedShoppingListPainter(isSelected: Boolean): Painter =
    rememberAnimatedIconPainter(
        targetProgress = if (isSelected) 1f else 0f,
        label = "shoppingListSelection",
        animationSpec = if (isSelected) tween(durationMillis = ANIMATION_DURATION_MS) else snap(),
        // Each tab screen draws its own bar, so a selected icon is composed already selected: start from rest to still play.
        initialProgress = 0f,
    ) { progress ->
        val scale = 1f + POP_SCALE * sin(PI * progress).toFloat()
        val rotation = WIGGLE_DEGREES * sin(2 * PI * progress).toFloat() * (1f - progress)
        Group(rotation = rotation, scaleX = scale, scaleY = scale, pivotX = CENTER, pivotY = CENTER) {
            Path(pathData = ShoppingListPath, pathFillType = PathFillType.EvenOdd, fill = AnimatedIconFill)
        }
    }

@Composable
private fun ShoppingListIconLayoutPreview() {
    var isSelected by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedShoppingListPainter(isSelected = isSelected),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { isSelected = isSelected.not() },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedShoppingListIconPreviewLightMode() {
    TheAppTheme {
        ShoppingListIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedShoppingListIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        ShoppingListIconLayoutPreview()
    }
}

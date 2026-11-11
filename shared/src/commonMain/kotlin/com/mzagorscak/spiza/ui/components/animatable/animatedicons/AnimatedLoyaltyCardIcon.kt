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
import kotlin.math.cos
import kotlin.math.sin

private const val ANIMATION_DURATION_MS = 600
private const val POP_SCALE = 0.1f
private const val CENTER = 12f

private val LoyaltyCardPath =
    addPathNodes(
        "M5,4.5H19A3,3 0,0 1,22 7.5V16.5A3,3 0,0 1,19 19.5H5A3,3 0,0 1,2 16.5V7.5A3,3 0,0 1,5 4.5Z" +
            "M5,8H6.25V16H5ZM7.25,8H8V16H7.25ZM9,8H10.5V16H9ZM11.5,8H12.25V16H11.5ZM13.25,8H14V16H13.25Z" +
            "M15,8H16.5V16H15ZM17.5,8H19V16H17.5Z",
    )

/**
 * The loyalty cards tab icon: the card turns over once, like flipping it in the hand, when it becomes selected
 * ([isSelected]), also when the tab's screen appears. Losing the selection resets it at once.
 */
@Composable
fun rememberAnimatedLoyaltyCardPainter(isSelected: Boolean): Painter =
    rememberAnimatedIconPainter(
        targetProgress = if (isSelected) 1f else 0f,
        label = "loyaltyCardSelection",
        animationSpec = if (isSelected) tween(durationMillis = ANIMATION_DURATION_MS) else snap(),
        // Each tab screen draws its own bar, so a selected icon is composed already selected: start from rest to still play.
        initialProgress = 0f,
    ) { progress ->
        val scale = 1f + POP_SCALE * sin(PI * progress).toFloat()
        // A full turn: the card narrows to its edge, shows its back and comes round to the front again.
        val flip = cos(2 * PI * progress).toFloat()
        Group(scaleX = scale * flip, scaleY = scale, pivotX = CENTER, pivotY = CENTER) {
            Path(pathData = LoyaltyCardPath, pathFillType = PathFillType.EvenOdd, fill = AnimatedIconFill)
        }
    }

@Composable
private fun LoyaltyCardIconLayoutPreview() {
    var isSelected by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedLoyaltyCardPainter(isSelected = isSelected),
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
private fun AnimatedLoyaltyCardIconPreviewLightMode() {
    TheAppTheme {
        LoyaltyCardIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedLoyaltyCardIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        LoyaltyCardIconLayoutPreview()
    }
}

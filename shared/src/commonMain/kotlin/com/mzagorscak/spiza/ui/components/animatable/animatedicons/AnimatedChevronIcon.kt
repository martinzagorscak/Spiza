package com.mzagorscak.spiza.ui.components.animatable.animatedicons

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

// Same shape and viewport as ic_filled_chevron_down, so it matches the static chevrons elsewhere in the app.
private const val VIEWPORT_SIZE = 32f
private const val PIVOT = VIEWPORT_SIZE / 2
private const val COLLAPSED_ROTATION_DEGREES = 180f

private val ChevronDownPath =
    addPathNodes(
        "M25.313,11.406l-7.906,9.906c-0.75,0.906 -1.906,0.906 -2.625,0l-7.906,-9.906" +
            "c-0.75,-0.938 -0.375,-1.656 0.781,-1.656h16.875c1.188,0 1.531,0.719 0.781,1.656z",
    )

/** A filled chevron that points down when [isExpanded] and up otherwise, turning over between the two. */
@Composable
fun rememberAnimatedChevronPainter(isExpanded: Boolean): Painter =
    rememberAnimatedIconPainter(
        atEnd = isExpanded,
        label = "chevronProgress",
        viewportSize = VIEWPORT_SIZE,
    ) { progress ->
        Group(rotation = COLLAPSED_ROTATION_DEGREES * (1f - progress), pivotX = PIVOT, pivotY = PIVOT) {
            Path(pathData = ChevronDownPath, fill = AnimatedIconFill)
        }
    }

@Composable
private fun AnimatedChevronIconLayoutPreview() {
    var isExpanded by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedChevronPainter(isExpanded = isExpanded),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { isExpanded = isExpanded.not() },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedChevronIconPreviewLightMode() {
    TheAppTheme {
        AnimatedChevronIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedChevronIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedChevronIconLayoutPreview()
    }
}

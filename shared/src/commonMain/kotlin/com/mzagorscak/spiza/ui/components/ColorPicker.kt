package com.mzagorscak.spiza.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200

private const val SELECTED_SCALE_GAIN = 0.1f
private const val LIGHT_COLOR_LUMINANCE = 0.5f

// Check mark in a 24 x 24 box: down to the corner, then up to the right.
private const val CHECK_VIEWPORT_SIZE = 24f
private const val CHECK_START_X = 6f
private const val CHECK_START_Y = 12.5f
private const val CHECK_CORNER_X = 10f
private const val CHECK_CORNER_Y = 16.5f
private const val CHECK_END_X = 18f
private const val CHECK_END_Y = 8f
private const val CHECK_MIN_PROGRESS = 0.02f
private const val CHECK_FADE_END = 0.3f

private val ColorItemSize = 40.dp
private val SelectedColorItemBorderWidth = 3.dp
private val CheckSize = 20.dp
private val CheckStrokeWidth = 2.5.dp

data class ColorPickerItem(
    val id: String,
    val color: Color,
    val isSelected: Boolean,
)

@Composable
fun ColorPicker(
    colors: List<ColorPickerItem>,
    onSelect: (ColorPickerItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(padding200),
        contentPadding = PaddingValues(horizontal = padding200, vertical = padding100),
        modifier = modifier,
    ) {
        items(colors) {
            ColorItem(
                colorItem = it,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun ColorItem(
    colorItem: ColorPickerItem,
    onSelect: (ColorPickerItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A bouncy spring overshoots past 1 on selection: that is the "pop". Width, alpha and the check are clamped to 0..1.
    val selection by animateFloatAsState(
        targetValue = if (colorItem.isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "colorSelection",
    )
    val clampedSelection = selection.coerceIn(0f, 1f)
    val checkColor = if (colorItem.color.luminance() > LIGHT_COLOR_LUMINANCE) Color.Black else Color.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ColorItemSize)
            .graphicsLayer {
                scaleX = 1f + SELECTED_SCALE_GAIN * selection
                scaleY = 1f + SELECTED_SCALE_GAIN * selection
            }.clip(CircleShape)
            .background(colorItem.color)
            .border(
                width = SelectedColorItemBorderWidth * clampedSelection,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = clampedSelection),
                shape = CircleShape,
            ).clickable { onSelect(colorItem) },
    ) {
        Canvas(modifier = Modifier.size(CheckSize)) {
            drawCheck(progress = clampedSelection, color = checkColor)
        }
    }
}

/** Draws the check mark stroke from its start up to [progress], so it looks drawn by hand when selecting. */
private fun DrawScope.drawCheck(
    progress: Float,
    color: Color,
) {
    // A round cap draws a dot even for a zero-length stroke, and the spring creeps towards 0 for a while when unselecting:
    // the check fades out over the last part of the stroke and is not drawn at all below the threshold.
    if (progress < CHECK_MIN_PROGRESS) return
    val alpha = (progress / CHECK_FADE_END).coerceIn(0f, 1f)
    val scale = size.width / CHECK_VIEWPORT_SIZE
    val check = Path().apply {
        moveTo(CHECK_START_X * scale, CHECK_START_Y * scale)
        lineTo(CHECK_CORNER_X * scale, CHECK_CORNER_Y * scale)
        lineTo(CHECK_END_X * scale, CHECK_END_Y * scale)
    }
    val measure = PathMeasure().apply { setPath(check, forceClosed = false) }
    val drawn = Path()
    measure.getSegment(startDistance = 0f, stopDistance = measure.length * progress, destination = drawn, startWithMoveTo = true)
    drawPath(
        path = drawn,
        color = color.copy(alpha = alpha),
        style = Stroke(width = CheckStrokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Composable
private fun ColorPickerLayoutPreview() {
    val colors = listOf(
        ColorPickerItem(id = "1", color = Color.Red, isSelected = false),
        ColorPickerItem(id = "2", color = Color.Green, isSelected = true),
        ColorPickerItem(id = "3", color = Color.Blue, isSelected = false),
        ColorPickerItem(id = "4", color = Color.Yellow, isSelected = false),
        ColorPickerItem(id = "5", color = Color.Cyan, isSelected = false),
    )

    ColorPicker(
        colors = colors,
        onSelect = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ColorPickerPreviewLightMode() {
    TheAppTheme {
        ColorPickerLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ColorPickerPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        ColorPickerLayoutPreview()
    }
}

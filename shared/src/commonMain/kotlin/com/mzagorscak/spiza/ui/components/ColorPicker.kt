package com.mzagorscak.spiza.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200

private val ColorItemSize = 40.dp
private val SelectedColorItemBorderWidth = 3.dp
private val UnselectedColorItemBorderWidth = 1.dp

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
    Box(
        modifier = modifier
            .size(ColorItemSize)
            .clip(CircleShape)
            .background(colorItem.color)
            .border(
                width = if (colorItem.isSelected) SelectedColorItemBorderWidth else UnselectedColorItemBorderWidth,
                color = if (colorItem.isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape,
            ).clickable { onSelect(colorItem) },
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

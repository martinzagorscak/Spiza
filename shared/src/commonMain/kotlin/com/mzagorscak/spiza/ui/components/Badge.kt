package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding150
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400

enum class BadgeSize { Small, Medium, Large }

data class BadgeAppearance(
    val textColor: Color,
    val backgroundColor: Color,
    val size: BadgeSize = BadgeSize.Medium,
) {
    @Composable
    fun getPadding(): PaddingValues =
        when (size) {
            BadgeSize.Small -> PaddingValues(horizontal = padding150, vertical = padding100)
            BadgeSize.Medium -> PaddingValues(horizontal = padding200, vertical = padding100)
            BadgeSize.Large -> PaddingValues(horizontal = padding400, vertical = padding200)
        }

    @Composable
    fun getTextStyle() =
        when (size) {
            BadgeSize.Small -> MaterialTheme.typography.labelSmall
            BadgeSize.Medium -> MaterialTheme.typography.labelMedium
            BadgeSize.Large -> MaterialTheme.typography.labelLarge
        }
}

@Composable
fun Badge(
    label: String,
    appearance: BadgeAppearance,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = appearance.getTextStyle(),
        color = appearance.textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(appearance.backgroundColor)
            .padding(appearance.getPadding()),
    )
}

@Composable
private fun BadgeLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.padding(padding200),
    ) {
        Badge(
            label = "Small Badge",
            appearance = BadgeAppearance(
                textColor = MaterialTheme.colorScheme.background,
                backgroundColor = MaterialTheme.colorScheme.onBackground,
                size = BadgeSize.Small,
            ),
        )
        Badge(
            label = "Medium Badge",
            appearance = BadgeAppearance(
                textColor = MaterialTheme.colorScheme.background,
                backgroundColor = MaterialTheme.colorScheme.onBackground,
                size = BadgeSize.Medium,
            ),
        )
        Badge(
            label = "Large Badge",
            appearance = BadgeAppearance(
                textColor = MaterialTheme.colorScheme.background,
                backgroundColor = MaterialTheme.colorScheme.onBackground,
                size = BadgeSize.Large,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BadgePreviewLightMode() {
    TheAppTheme {
        BadgeLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BadgePreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        BadgeLayoutPreview()
    }
}

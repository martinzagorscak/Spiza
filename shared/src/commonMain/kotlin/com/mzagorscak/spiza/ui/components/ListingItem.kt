package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_right_chevron

private val MenuListingItemHeight = 36.dp

@Composable
fun ListingItem(
    label: String,
    onClick: (() -> Unit)?,
    leadingContent: (@Composable (() -> Unit))? = null,
    trailingContent: (@Composable (() -> Unit))? = null,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                enabled = onClick != null,
                onClick = onClick ?: {},
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = padding200, vertical = padding200)
                .height(MenuListingItemHeight),
        ) {
            leadingContent?.let {
                it()
                Spacer(modifier = Modifier.width(padding200))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            trailingContent?.let {
                Spacer(modifier = Modifier.weight(1f))
                it()
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun MenuListingItemLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(padding200),
    ) {
        var isToggleOn by remember { mutableStateOf(false) }

        ListingItem(
            label = "Menu Item 1",
            onClick = {},
            showDivider = true,
        )
        ListingItem(
            label = "Menu Item 2",
            trailingContent = @Composable {
                IconButton(
                    resource = Res.drawable.ic_right_chevron,
                    appearance = IconButtonAppearance(
                        variant = IconButtonVariant.IconBased,
                        size = IconButtonSize.Small,
                    ),
                    onClick = {},
                )
            },
            onClick = null,
            showDivider = true,
        )
        ListingItem(
            label = "Menu Item 2",
            trailingContent = @Composable {
                ToggleButton(
                    checked = isToggleOn,
                    onCheckedChange = { isToggleOn = it },
                )
            },
            onClick = { isToggleOn = !isToggleOn },
            showDivider = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MenuListingItemPreviewLightMode() {
    TheAppTheme {
        MenuListingItemLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MenuListingItemPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        MenuListingItemLayoutPreview()
    }
}

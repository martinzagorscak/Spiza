package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.roundedShape400
import org.jetbrains.compose.resources.painterResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_plus

private const val SINGLE_LINE_COUNT = 1
private const val ENABLED_ALPHA = 1f
private const val DISABLED_ALPHA = 0.5f

@Composable
fun TextChip(
    text: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(roundedShape400)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                enabled = onClick != null && enabled,
                onClick = onClick ?: {},
            ).padding(padding100)
            .alpha(if (enabled.not()) DISABLED_ALPHA else ENABLED_ALPHA),
    ) {
        leadingContent?.let {
            it()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = SINGLE_LINE_COUNT,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(
                horizontal = padding100,
                vertical = padding100,
            ),
        )
        trailingContent?.let {
            it()
        }
    }
}

@Composable
private fun TextChipLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.padding(padding200),
    ) {
        TextChip(
            text = "Text Chip",
            onClick = {},
        )
        TextChip(
            text = "Chip",
            onClick = {},
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = null,
                )
            },
        )
        TextChip(
            text = "Text Chip",
            onClick = {},
            trailingContent = {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = null,
                )
            },
        )
        TextChip(
            text = "Text Chip",
            onClick = {},
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = null,
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = null,
                )
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TextChipPreviewLightMode() {
    TheAppTheme {
        TextChipLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TextChipPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        TextChipLayoutPreview()
    }
}

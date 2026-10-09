package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding300
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_back

private const val SINGLE_LINE_COUNT = 1

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .windowInsetsPadding(insets = WindowInsets.statusBars)
            .fillMaxWidth()
            .padding(horizontal = padding200),
    ) {
        leadingContent?.invoke()

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = SINGLE_LINE_COUNT,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = padding200,
                    vertical = padding300,
                ),
        )

        trailingContent?.invoke()
    }
}

@Composable
private fun TopBarLayoutPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(padding200)) {
        val leadingContent: @Composable () -> Unit = {
            IconButton(
                resource = Res.drawable.ic_back,
                onClick = {},
            )
        }
        TopBar(title = "Header1")
        TopBar(
            title = "Header2",
            leadingContent = leadingContent,
        )
        TopBar(
            title = "Header3",
            leadingContent = leadingContent,
            trailingContent = leadingContent,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TopBarPreviewLightMode() {
    TheAppTheme {
        TopBarLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TopBarPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        TopBarLayoutPreview()
    }
}

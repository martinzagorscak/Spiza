package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200

@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    )
}

@Composable
private fun ToggleButtonLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.padding(padding200),
    ) {
        ToggleButton(
            checked = true,
            onCheckedChange = {},
        )

        ToggleButton(
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ToggleButtonPreviewLightMode() {
    TheAppTheme {
        ToggleButtonLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ToggleButtonPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        ToggleButtonLayoutPreview()
    }
}

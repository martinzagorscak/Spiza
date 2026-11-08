package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.padding600
import com.mzagorscak.spiza.ui.theme.roundedShape400

@Composable
fun Dialog(
    title: String,
    description: String,
    dismissCTA: CTA,
    confirmCTA: CTA,
    onDismissRequest: () -> Unit = dismissCTA.onClick,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        BasicDialogContent(
            title = title,
            description = description,
            dismissCTA = dismissCTA,
            confirmCTA = confirmCTA,
        )
    }
}

@Composable
fun Dialog(
    title: String,
    description: String,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    dismissCTA: CTA,
    confirmCTA: CTA,
) {
    Dialog(onDismissRequest = dismissCTA.onClick) {
        UserInputDialogContent(
            title = title,
            description = description,
            textInput = textInput,
            onTextInputChange = onTextInputChange,
            label = label,
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            dismissCTA = dismissCTA,
            confirmCTA = confirmCTA,
        )
    }
}

@Composable
private fun UserInputDialogContent(
    title: String,
    description: String,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    dismissCTA: CTA,
    confirmCTA: CTA,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding400),
        modifier = modifier
            .fillMaxWidth()
            .clip(roundedShape400)
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding600),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextField(
            text = textInput,
            onTextChange = onTextInputChange,
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Outlined,
                singleLine = true,
                label = label,
                placeholder = placeholder,
                trailingIcon = trailingIcon,
            ),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                label = dismissCTA.label,
                onClick = dismissCTA.onClick,
                isEnabled = dismissCTA.isEnabled,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Medium,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )

            Button(
                label = confirmCTA.label,
                onClick = confirmCTA.onClick,
                isEnabled = confirmCTA.isEnabled,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Medium,
                ),
            )
        }
    }
}

@Composable
private fun BasicDialogContent(
    title: String,
    description: String,
    dismissCTA: CTA,
    confirmCTA: CTA,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding400),
        modifier = modifier
            .fillMaxWidth()
            .clip(roundedShape400)
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding600),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                label = dismissCTA.label,
                onClick = dismissCTA.onClick,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Medium,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )

            Button(
                label = confirmCTA.label,
                onClick = confirmCTA.onClick,
                isEnabled = confirmCTA.isEnabled,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Tertiary,
                    size = ButtonSize.Medium,
                ),
            )
        }
    }
}

@Composable
private fun DialogLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.padding(padding200),
    ) {
        BasicDialogContent(
            title = "Dialog Title",
            description = "This is a sample dialog description.",
            dismissCTA = CTA(label = "Cancel", onClick = {}),
            confirmCTA = CTA(label = "Confirm", onClick = {}),
        )

        UserInputDialogContent(
            title = "Dialog Title",
            description = "This is a sample dialog description.",
            textInput = "",
            onTextInputChange = {},
            dismissCTA = CTA(label = "Cancel", onClick = {}),
            confirmCTA = CTA(label = "Confirm", onClick = {}),
            label = "Input Label",
            placeholder = "Enter text here",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DialogPreviewLightMode() {
    TheAppTheme {
        DialogLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DialogPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        DialogLayoutPreview()
    }
}

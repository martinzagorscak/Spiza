package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.text_field_optional_template

private const val SINGLE_LINE_COUNT = 1
private const val MAX_LINE_COUNT = 5

enum class TextFieldVariant {
    Filled,
    Outlined,
    Borderless,
}

data class TextFieldAppearance(
    val variant: TextFieldVariant,
    val singleLine: Boolean,
    val readOnly: Boolean = false,
    val isOptional: Boolean = false,
    val label: String? = null,
    val placeholder: String? = null,
    val supportingText: String? = null,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val trailingIcon: @Composable (() -> Unit)? = null,
) {
    companion object {
        val Default = TextFieldAppearance(
            variant = TextFieldVariant.Filled,
            singleLine = true,
        )
    }
}

@Composable
fun TextField(
    text: String,
    onTextChange: (String) -> Unit,
    isError: Boolean = false,
    appearance: TextFieldAppearance = TextFieldAppearance.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    when (appearance.variant) {
        TextFieldVariant.Filled, TextFieldVariant.Borderless -> {
            val colors =
                if (appearance.variant == TextFieldVariant.Filled) {
                    TextFieldDefaults.colors()
                } else {
                    TextFieldDefaults.colors().copy(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                }

            androidx.compose.material3.TextField(
                value = text,
                onValueChange = onTextChange,
                isError = isError,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = colors,
                placeholder = appearance.placeholder?.let { placeholderText ->
                    @Composable {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                label = appearance.label?.let { labelText ->
                    @Composable {
                        Text(
                            text = labelText.let {
                                if (appearance.isOptional) {
                                    stringResource(
                                        Res.string.text_field_optional_template,
                                        it,
                                    )
                                } else {
                                    it
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                supportingText = appearance.supportingText?.let { supportingText ->
                    @Composable {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                readOnly = appearance.readOnly,
                leadingIcon = appearance.leadingIcon,
                trailingIcon = appearance.trailingIcon,
                keyboardOptions = keyboardOptions,
                keyboardActions = onKeyboardAction,
                singleLine = appearance.singleLine,
                maxLines = if (appearance.singleLine) SINGLE_LINE_COUNT else MAX_LINE_COUNT,
                modifier = modifier,
            )
        }

        TextFieldVariant.Outlined -> {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                isError = isError,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = appearance.placeholder?.let { placeholderText ->
                    @Composable {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                label = appearance.label?.let { labelText ->
                    @Composable {
                        Text(
                            text = labelText.let {
                                if (appearance.isOptional) {
                                    stringResource(
                                        Res.string.text_field_optional_template,
                                        it,
                                    )
                                } else {
                                    it
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                readOnly = appearance.readOnly,
                leadingIcon = appearance.leadingIcon,
                trailingIcon = appearance.trailingIcon,
                keyboardOptions = keyboardOptions,
                keyboardActions = onKeyboardAction,
                singleLine = appearance.singleLine,
                maxLines = if (appearance.singleLine) SINGLE_LINE_COUNT else MAX_LINE_COUNT,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TextFieldLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.padding(padding200),
    ) {
        var txt by remember { mutableStateOf("") }
        val onTextChange = { newText: String -> txt = newText }
        TextField(
            text = txt,
            onTextChange = onTextChange,
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Filled,
                singleLine = true,
                placeholder = "Filled TextField",
                label = "Label",
            ),
        )
        TextField(
            text = "Typing...",
            onTextChange = {},
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Filled,
                singleLine = true,
                placeholder = "Filled TextField",
                label = "Label",
            ),
        )
        TextField(
            text = "",
            onTextChange = {},
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Outlined,
                singleLine = true,
                placeholder = "Outlined TextField",
                label = "Label",
            ),
        )
        TextField(
            text = "Typing...",
            onTextChange = {},
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Outlined,
                singleLine = true,
                placeholder = "Outlined TextField",
                label = "Label",
            ),
        )
        TextField(
            text = "",
            onTextChange = {},
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Borderless,
                singleLine = true,
                placeholder = "Outlined TextField",
                label = "Label",
            ),
        )
        TextField(
            text = "Typing...",
            onTextChange = {},
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Borderless,
                singleLine = true,
                placeholder = "Outlined TextField",
                label = "Label",
                supportingText = "9/20",
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TextFieldPreviewLightMode() {
    TheAppTheme {
        TextFieldLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TextFieldPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        TextFieldLayoutPreview()
    }
}

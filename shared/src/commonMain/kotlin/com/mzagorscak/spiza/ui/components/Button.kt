package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding050
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.padding800

enum class ButtonSize { Small, Medium, Large }

enum class ButtonVariant { Primary, Secondary, Tertiary }

data class ButtonAppearance(
    val variant: ButtonVariant,
    val size: ButtonSize,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
) {
    companion object {
        val Default = ButtonAppearance(
            variant = ButtonVariant.Primary,
            size = ButtonSize.Medium,
        )
    }

    @Composable
    fun buttonColors(): ButtonColors =
        when (variant) {
            ButtonVariant.Primary, ButtonVariant.Tertiary -> {
                ButtonDefaults.buttonColors().copy(
                    containerColor = when (variant) {
                        ButtonVariant.Primary -> contentColor ?: MaterialTheme.colorScheme.primary
                        ButtonVariant.Tertiary -> Color.Transparent
                    },
                    contentColor = when (variant) {
                        ButtonVariant.Primary -> contentColor ?: MaterialTheme.colorScheme.onPrimary
                        ButtonVariant.Tertiary -> contentColor ?: MaterialTheme.colorScheme.primary
                    },
                    disabledContainerColor = when (variant) {
                        ButtonVariant.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ButtonVariant.Tertiary -> Color.Transparent
                    },
                    disabledContentColor = when (variant) {
                        ButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        ButtonVariant.Tertiary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    },
                )
            }

            ButtonVariant.Secondary -> {
                ButtonDefaults.outlinedButtonColors().copy(
                    containerColor = Color.Transparent,
                    contentColor = contentColor ?: MaterialTheme.colorScheme.secondary,
                )
            }
        }

    @Composable
    fun getFontStyle(): TextStyle =
        with(MaterialTheme.typography) {
            when (size) {
                ButtonSize.Small -> labelSmall
                ButtonSize.Medium -> labelMedium
                ButtonSize.Large -> labelLarge
            }
        }

    fun getContentPadding(): PaddingValues =
        when (size) {
            ButtonSize.Small -> PaddingValues(horizontal = padding200, vertical = padding050)
            ButtonSize.Medium -> PaddingValues(horizontal = padding400, vertical = padding200)
            ButtonSize.Large -> PaddingValues(horizontal = padding800, vertical = padding200)
        }

    fun getMinHeight(): Dp =
        when (size) {
            ButtonSize.Small -> 32.dp
            ButtonSize.Medium -> 40.dp
            ButtonSize.Large -> 48.dp
        }
}

@Composable
fun Button(
    label: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    appearance: ButtonAppearance = ButtonAppearance.Default,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val contentPadding = appearance.getContentPadding()
    val sizedModifier = modifier.defaultMinSize(minHeight = appearance.getMinHeight())

    when (appearance.variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                enabled = isEnabled,
                contentPadding = contentPadding,
                colors = appearance.buttonColors(),
                modifier = sizedModifier,
            ) {
                this.ButtonContent(
                    label = label,
                    appearance = appearance,
                    leadingContent = leadingContent,
                    trailingContent = trailingContent,
                )
            }
        }

        ButtonVariant.Secondary -> {
            val border = BorderStroke(
                width = 1.dp,
                color = appearance.contentColor
                    ?: with(MaterialTheme.colorScheme) { if (isEnabled) secondary else onSurface.copy(alpha = 0.1f) },
            )

            OutlinedButton(
                onClick = onClick,
                enabled = isEnabled,
                contentPadding = contentPadding,
                colors = appearance.buttonColors(),
                border = border,
                modifier = sizedModifier,
            ) {
                this.ButtonContent(
                    label = label,
                    appearance = appearance,
                    leadingContent = leadingContent,
                    trailingContent = trailingContent,
                )
            }
        }

        ButtonVariant.Tertiary -> {
            TextButton(
                onClick = onClick,
                enabled = isEnabled,
                contentPadding = contentPadding,
                colors = appearance.buttonColors(),
                modifier = sizedModifier,
            ) {
                this.ButtonContent(
                    label = label,
                    appearance = appearance,
                    leadingContent = leadingContent,
                    trailingContent = trailingContent,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ButtonContent(
    label: String,
    appearance: ButtonAppearance,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    leadingContent?.let { content ->
        content()
        Spacer(modifier = Modifier.width(padding200))
    }

    Text(
        text = label,
        style = appearance.getFontStyle(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    trailingContent?.let { content ->
        Spacer(modifier = Modifier.width(padding200))
        content()
    }
}

@Composable
private fun ButtonsLayoutPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding050),
        modifier = Modifier.padding(padding200),
    ) {
        Button(
            label = "Primary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Primary,
                size = ButtonSize.Small,
            ),
        )
        Button(
            label = "Primary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Primary,
                size = ButtonSize.Medium,
            ),
        )
        Button(
            label = "Primary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
            ),
        )
        Button(
            label = "Primary Button",
            onClick = {},
            isEnabled = false,
            appearance = ButtonAppearance(
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
            ),
        )
        Button(
            label = "Secondary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
            ),
        )
        Button(
            label = "Secondary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
            ),
        )
        Button(
            label = "Secondary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Large,
            ),
        )
        Button(
            label = "Secondary Button",
            onClick = {},
            isEnabled = false,
            appearance = ButtonAppearance(
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Large,
            ),
        )
        Button(
            label = "Tertiary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Tertiary,
                size = ButtonSize.Small,
            ),
        )
        Button(
            label = "Tertiary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Tertiary,
                size = ButtonSize.Medium,
            ),
        )
        Button(
            label = "Tertiary Button",
            onClick = {},
            appearance = ButtonAppearance(
                variant = ButtonVariant.Tertiary,
                size = ButtonSize.Large,
            ),
        )
        Button(
            label = "Tertiary Button",
            onClick = {},
            isEnabled = false,
            appearance = ButtonAppearance(
                variant = ButtonVariant.Tertiary,
                size = ButtonSize.Large,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ButtonPreviewLightMode() {
    TheAppTheme {
        ButtonsLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ButtonPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        ButtonsLayoutPreview()
    }
}

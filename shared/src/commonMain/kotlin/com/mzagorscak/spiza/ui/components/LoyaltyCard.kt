package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.ui.components.barcode.Barcode
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.theme.CardColorRed
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding300
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.roundedShape300
import com.mzagorscak.spiza.ui.theme.roundedShape400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_pin
import spiza.shared.generated.resources.ic_pin_filled
import spiza.shared.generated.resources.loyalty_cards_add_card_name_placeholder
import spiza.shared.generated.resources.loyalty_cards_shortcut_add
import spiza.shared.generated.resources.loyalty_cards_shortcut_remove

private const val LUMINANCE_THRESHOLD = 0.5f
private const val SINGLE_LINE = 1
private const val DISPLAY_CODE_MAX_LINES = 2
private val SquareCodeSize = 160.dp
private val BarcodeHeight = 90.dp

@Composable
fun LoyaltyCard(
    card: PresentableLoyaltyCard,
    modifier: Modifier = Modifier,
    onShortcutClick: (() -> Unit)? = null,
) {
    val textColor = if (card.color.luminance() > LUMINANCE_THRESHOLD) Color.Black else Color.White

    Card(
        shape = roundedShape400,
        colors = CardDefaults.cardColors(containerColor = card.color),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(padding400),
            verticalArrangement = Arrangement.spacedBy(padding300),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = card.name.ifBlank { stringResource(Res.string.loyalty_cards_add_card_name_placeholder) },
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    maxLines = SINGLE_LINE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (onShortcutClick != null) {
                    ShortcutPin(
                        isShortcut = card.isShortcut,
                        tintColor = textColor,
                        onClick = onShortcutClick,
                    )
                }
            }

            if (card.codeText.isNotBlank()) {
                Surface(
                    color = Color.White,
                    shape = roundedShape300,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(padding400),
                    ) {
                        Barcode(
                            content = card.codeText,
                            format = card.format,
                            modifier = if (card.format.isSquare) {
                                Modifier.size(SquareCodeSize)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .height(BarcodeHeight)
                            },
                        )
                    }
                }

                Text(
                    text = card.displayCode,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = DISPLAY_CODE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ShortcutPin(
    isShortcut: Boolean,
    tintColor: Color,
    onClick: () -> Unit,
) {
    val description = stringResource(
        if (isShortcut) Res.string.loyalty_cards_shortcut_remove else Res.string.loyalty_cards_shortcut_add,
    )
    IconButton(
        resource = if (isShortcut) Res.drawable.ic_pin_filled else Res.drawable.ic_pin,
        onClick = onClick,
        appearance = IconButtonAppearance(
            variant = IconButtonVariant.IconBased,
            size = IconButtonSize.Medium,
            tintColor = tintColor,
        ),
        modifier = Modifier.semantics {
            contentDescription = description
            role = Role.Button
        },
    )
}

// PDF417 is two-dimensional but wide, so it is laid out like a linear barcode.
private val LoyaltyCardFormat.isSquare: Boolean
    get() = this == LoyaltyCardFormat.QrCode || this == LoyaltyCardFormat.Aztec

@Composable
private fun LayoutPreview() {
    val card = PresentableLoyaltyCard(
        id = 1L,
        name = "My Loyalty Card",
        color = CardColorRed,
        codeText = "1234567890",
        format = LoyaltyCardFormat.QrCode,
    )

    Column(verticalArrangement = Arrangement.spacedBy(padding400)) {
        LoyaltyCard(card = card)
        LoyaltyCard(card = card.copy(isShortcut = true), onShortcutClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLightMode() {
    TheAppTheme {
        LayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        LayoutPreview()
    }
}

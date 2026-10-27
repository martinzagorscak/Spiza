package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.theme.CardColorRed
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding300
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.roundedShape300
import com.mzagorscak.spiza.ui.theme.roundedShape400
import org.jetbrains.compose.resources.stringResource
import qrgenerator.barcodepainter.BarcodeFormat
import qrgenerator.barcodepainter.rememberBarcodePainter
import qrgenerator.qrkitpainter.rememberQrKitPainter
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.loyalty_cards_add_card_name_placeholder

private const val LUMINANCE_THRESHOLD = 0.5f
private const val SINGLE_LINE = 1
private val QRCodeSize = 160.dp
private val BarcodeHeight = 90.dp

@Composable
fun LoyaltyCard(
    card: PresentableLoyaltyCard,
    modifier: Modifier = Modifier,
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
            Text(
                text = card.name.ifBlank { stringResource(Res.string.loyalty_cards_add_card_name_placeholder) },
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                maxLines = SINGLE_LINE,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Start),
            )

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
                        if (card.isQrFormat) {
                            val painter = rememberQrKitPainter(data = card.codeText)
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.size(QRCodeSize),
                            )
                        } else {
                            val painter = rememberBarcodePainter(
                                content = card.codeText,
                                format = BarcodeFormat.Code128,
                                onError = { ColorPainter(Color.LightGray) },
                            )
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(BarcodeHeight),
                            )
                        }
                    }
                }

                Text(
                    text = card.codeText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun LayoutPreview() {
    val card = PresentableLoyaltyCard(
        id = 1L,
        name = "My Loyalty Card",
        color = CardColorRed,
        codeText = "1234567890",
        isQrFormat = true,
    )

    LoyaltyCard(card = card)
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

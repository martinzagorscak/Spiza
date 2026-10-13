package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding800
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import spiza.shared.generated.resources.Res

private val LottieImageSize = 160.dp

@Composable
fun FallbackState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    lottiePath: String? = null,
    cta: CTA? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(padding800),
    ) {
        lottiePath?.let {
            val composition by rememberLottieComposition {
                LottieCompositionSpec.JsonString(
                    Res.readBytes(it).decodeToString(),
                )
            }

            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = Compottie.IterateForever,
            )

            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    progress = { progress },
                ),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(LottieImageSize),
            )
            Spacer(modifier = Modifier.height(padding200))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        description?.let {
            Spacer(modifier = Modifier.height(padding200))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }

        cta?.let {
            Spacer(modifier = Modifier.height(padding200))
            Button(
                label = it.label,
                onClick = it.onClick,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Medium,
                ),
            )
        }
    }
}

@Composable
private fun FallbackStateLayoutPreview() {
    FallbackState(
        title = "No data available",
        description = "Please check your internet connection and try again.",
        cta = CTA(
            label = "Retry",
            onClick = {},
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FallbackStatePreviewLightMode() {
    TheAppTheme {
        FallbackStateLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FallbackStatePreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        FallbackStateLayoutPreview()
    }
}

package com.mzagorscak.spiza.ui.components.barcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import kotlin.math.floor
import kotlin.math.min

// Quiet zones (in modules) required around the symbol, on top of the white surface the barcode sits on.
private const val LINEAR_QUIET_ZONE_MODULES = 10
private const val TWO_DIMENSIONAL_QUIET_ZONE_MODULES = 2

private val BarColor = Color.Black
private val PlaceholderColor = Color.LightGray

/**
 * Draws [content] as a [format] barcode, black on transparent: place it on a white surface.
 * Modules are snapped to whole pixels because blurred edges are what makes on-screen barcodes fail at checkout.
 * Shows a grey placeholder when the content cannot be encoded.
 */
@Composable
fun Barcode(
    content: String,
    format: LoyaltyCardFormat,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(content, format) { encodeBarcode(content = content, format = format) }
    if (matrix == null) {
        Box(modifier = modifier.background(PlaceholderColor))
    } else {
        Canvas(modifier = modifier) {
            if (matrix.isLinear) drawLinear(matrix) else drawTwoDimensional(matrix)
        }
    }
}

private fun DrawScope.drawLinear(matrix: BarcodeMatrix) {
    val moduleWidth = (size.width / (matrix.width + 2 * LINEAR_QUIET_ZONE_MODULES)).snapToPixels()
    val left = floor((size.width - matrix.width * moduleWidth) / 2)
    drawRuns(matrix = matrix, left = left, top = 0f, moduleWidth = moduleWidth, moduleHeight = size.height)
}

private fun DrawScope.drawTwoDimensional(matrix: BarcodeMatrix) {
    val quietZone = 2 * TWO_DIMENSIONAL_QUIET_ZONE_MODULES
    val moduleSize = min(size.width / (matrix.width + quietZone), size.height / (matrix.height + quietZone)).snapToPixels()
    val left = floor((size.width - matrix.width * moduleSize) / 2)
    val top = floor((size.height - matrix.height * moduleSize) / 2)
    drawRuns(matrix = matrix, left = left, top = top, moduleWidth = moduleSize, moduleHeight = moduleSize)
}

/** Draws each horizontal run of dark modules as one rectangle. */
private fun DrawScope.drawRuns(
    matrix: BarcodeMatrix,
    left: Float,
    top: Float,
    moduleWidth: Float,
    moduleHeight: Float,
) {
    for (y in 0 until matrix.height) {
        var x = 0
        while (x < matrix.width) {
            if (matrix.isDark(x = x, y = y)) {
                val start = x
                while (x < matrix.width && matrix.isDark(x = x, y = y)) x++
                drawRect(
                    color = BarColor,
                    topLeft = Offset(x = left + start * moduleWidth, y = top + y * moduleHeight),
                    size = Size(width = (x - start) * moduleWidth, height = moduleHeight),
                )
            } else {
                x++
            }
        }
    }
}

private fun Float.snapToPixels(): Float = if (this >= 1f) floor(this) else this

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLinear() {
    TheAppTheme {
        Barcode(
            content = "3850123456782",
            format = LoyaltyCardFormat.Ean13,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewQrCode() {
    TheAppTheme(darkTheme = true) {
        Barcode(
            content = "https://example.com/card/123456",
            format = LoyaltyCardFormat.QrCode,
            modifier = Modifier.size(160.dp),
        )
    }
}

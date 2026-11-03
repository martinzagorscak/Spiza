package com.mzagorscak.spiza.ui.components.barcode

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

/** Dark/light modules of an encoded barcode, row by row. Linear barcodes have a single row. */
class BarcodeMatrix(
    val width: Int,
    val height: Int,
    private val modules: BooleanArray,
) {
    init {
        require(modules.size == width * height) { "Expected ${width * height} modules, got ${modules.size}" }
    }

    val isLinear: Boolean get() = height == 1

    fun isDark(
        x: Int,
        y: Int,
    ): Boolean = modules[y * width + x]

    companion object {
        fun linear(modules: BooleanArray): BarcodeMatrix = BarcodeMatrix(width = modules.size, height = 1, modules = modules)
    }
}

/** Returns `null` when [content] cannot be encoded in [format]. */
fun encodeBarcode(
    content: String,
    format: LoyaltyCardFormat,
): BarcodeMatrix? =
    if (format.isTwoDimensional) {
        encodeTwoDimensionalBarcode(content = content, format = format)
    } else {
        LinearBarcodeEncoder.encode(content = content, format = format)?.let(BarcodeMatrix::linear)
    }

/** QR, Aztec and PDF417 are encoded by the platform (ZXing on Android, Core Image on iOS). */
internal expect fun encodeTwoDimensionalBarcode(
    content: String,
    format: LoyaltyCardFormat,
): BarcodeMatrix?

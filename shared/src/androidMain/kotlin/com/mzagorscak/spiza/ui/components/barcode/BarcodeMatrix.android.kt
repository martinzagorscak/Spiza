package com.mzagorscak.spiza.ui.components.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

private const val MAX_LATIN_1 = 0xFF

internal actual fun encodeTwoDimensionalBarcode(
    content: String,
    format: LoyaltyCardFormat,
): BarcodeMatrix? {
    val zxingFormat = when (format) {
        LoyaltyCardFormat.QrCode -> BarcodeFormat.QR_CODE
        LoyaltyCardFormat.Aztec -> BarcodeFormat.AZTEC
        LoyaltyCardFormat.Pdf417 -> BarcodeFormat.PDF_417
        else -> return null
    }
    // Scanners decode these codes as ISO-8859-1 by default, so only switch to UTF-8 when the content needs it.
    val charset = if (content.all { it.code <= MAX_LATIN_1 }) "ISO-8859-1" else "UTF-8"
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to charset,
        EncodeHintType.MARGIN to 0,
    )
    val bitMatrix = try {
        MultiFormatWriter().encode(content, zxingFormat, 0, 0, hints)
    } catch (_: WriterException) {
        return null
    } catch (_: IllegalArgumentException) {
        // Thrown for content that does not fit the symbol
        return null
    }
    val modules = BooleanArray(bitMatrix.width * bitMatrix.height) { index ->
        bitMatrix.get(index % bitMatrix.width, index / bitMatrix.width)
    }
    return BarcodeMatrix(width = bitMatrix.width, height = bitMatrix.height, modules = modules)
}

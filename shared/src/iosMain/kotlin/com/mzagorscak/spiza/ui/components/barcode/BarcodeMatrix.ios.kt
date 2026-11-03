package com.mzagorscak.spiza.ui.components.barcode

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.NSISOLatin1StringEncoding
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue

private const val MAX_LATIN_1 = 0xFF
private const val DARK_THRESHOLD = 128

// Creating a Core Image context is expensive, so all codes share one.
@OptIn(ExperimentalForeignApi::class)
private val ciContext: CIContext? by lazy { CIContext.contextWithOptions(null) }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun encodeTwoDimensionalBarcode(
    content: String,
    format: LoyaltyCardFormat,
): BarcodeMatrix? {
    val filterName = when (format) {
        LoyaltyCardFormat.QrCode -> "CIQRCodeGenerator"
        LoyaltyCardFormat.Aztec -> "CIAztecCodeGenerator"
        LoyaltyCardFormat.Pdf417 -> "CIPDF417BarcodeGenerator"
        else -> return null
    }
    // Scanners decode these codes as ISO-8859-1 by default, so only switch to UTF-8 when the content needs it.
    val encoding = if (content.all { it.code <= MAX_LATIN_1 }) NSISOLatin1StringEncoding else NSUTF8StringEncoding
    val message = NSString.create(string = content).dataUsingEncoding(encoding) ?: return null
    val filter = CIFilter.filterWithName(filterName) ?: return null
    filter.setValue(message, forKey = "inputMessage")
    if (format == LoyaltyCardFormat.QrCode) filter.setValue("M", forKey = "inputCorrectionLevel")
    // Core Image returns no image when the content does not fit the symbol.
    val image = filter.outputImage ?: return null
    val cgImage = ciContext?.createCGImage(image, fromRect = image.extent) ?: return null
    return try {
        cgImage.toBarcodeMatrix()
    } finally {
        CGImageRelease(cgImage)
    }
}

/** Reads [this] as dark/light modules, one per pixel, top row first. */
@OptIn(ExperimentalForeignApi::class)
internal fun CGImageRef.toBarcodeMatrix(): BarcodeMatrix {
    val width = CGImageGetWidth(this).toInt()
    val height = CGImageGetHeight(this).toInt()
    val pixels = ByteArray(width * height)
    pixels.usePinned { pinned ->
        val colorSpace = CGColorSpaceCreateDeviceGray()
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = width.toULong(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNone.value,
        )
        // Bitmap memory is top row first, while Quartz draws with a bottom-left origin; drawing into the full rect keeps it upright.
        CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), this)
        CGContextRelease(context)
        CGColorSpaceRelease(colorSpace)
    }
    val modules = BooleanArray(pixels.size) { index -> (pixels[index].toInt() and 0xFF) < DARK_THRESHOLD }
    return BarcodeMatrix(width = width, height = height, modules = modules)
}

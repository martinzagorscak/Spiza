package com.mzagorscak.spiza.ui.components.barcode

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetGrayFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Core Image cannot create a context in the headless simulator test process, so rendering the symbols themselves is verified in the app.
@OptIn(ExperimentalForeignApi::class)
class TwoDimensionalBarcodeEncoderIosTest {
    @Test
    fun toBarcodeMatrix_keepsTheImageUpright() {
        val width = 4
        val height = 3
        val colorSpace = CGColorSpaceCreateDeviceGray()
        val context =
            CGBitmapContextCreate(null, width.toULong(), height.toULong(), 8u, 0u, colorSpace, CGImageAlphaInfo.kCGImageAlphaNone.value)
        CGContextSetGrayFillColor(context, 1.0, 1.0)
        CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
        // Quartz has a bottom-left origin: y = height - 1 is the top row, x = 0 the left column.
        CGContextSetGrayFillColor(context, 0.0, 1.0)
        CGContextFillRect(context, CGRectMake(0.0, height - 1.0, width.toDouble(), 1.0))
        CGContextFillRect(context, CGRectMake(0.0, 0.0, 1.0, height.toDouble()))
        val image = assertNotNull(CGBitmapContextCreateImage(context))

        val matrix = image.toBarcodeMatrix()

        CGImageRelease(image)
        CGContextRelease(context)
        CGColorSpaceRelease(colorSpace)
        assertEquals(width, matrix.width)
        assertEquals(height, matrix.height)
        assertTrue((0 until width).all { matrix.isDark(x = it, y = 0) })
        assertTrue(matrix.isDark(x = 0, y = height - 1))
        assertFalse(matrix.isDark(x = width - 1, y = height - 1))
    }

    @Test
    fun encode_returnsNull_forLinearFormats() {
        assertNull(encodeTwoDimensionalBarcode(content = "4006381333931", format = LoyaltyCardFormat.Ean13))
    }
}

package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosScannerTypesTest {
    @Test
    fun everyFormatExceptUpcA_isRecognisedByTheScanner() {
        // UPC-A arrives as EAN-13 with a leading zero and is turned back into UPC-A by normalization.
        assertEquals(
            LoyaltyCardFormat.entries - LoyaltyCardFormat.UpcA,
            SupportedTypes.values
                .filterNotNull()
                .distinct()
                .sorted(),
        )
    }

    @Test
    fun dataMatrix_isDetectedButUnsupported() {
        assertTrue(AVMetadataObjectTypeDataMatrixCode in SupportedTypes.keys)
        assertNull(SupportedTypes[AVMetadataObjectTypeDataMatrixCode])
    }
}

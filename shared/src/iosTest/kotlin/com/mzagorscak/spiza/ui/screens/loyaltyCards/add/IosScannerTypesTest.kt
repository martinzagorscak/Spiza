package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import kotlin.test.Test

class IosScannerTypesTest {
    @Test
    fun everyFormatExceptUpcA_isRecognisedByTheScanner() {
        // UPC-A arrives as EAN-13 with a leading zero and is turned back into UPC-A by normalization.
        assertThat(
            SupportedTypes.values
                .filterNotNull()
                .distinct()
                .sorted(),
        ).isEqualTo(
            LoyaltyCardFormat.entries - LoyaltyCardFormat.UpcA,
        )
    }

    @Test
    fun dataMatrix_isDetectedButUnsupported() {
        assertThat(AVMetadataObjectTypeDataMatrixCode in SupportedTypes.keys).isTrue()
        assertThat(SupportedTypes[AVMetadataObjectTypeDataMatrixCode]).isNull()
    }
}

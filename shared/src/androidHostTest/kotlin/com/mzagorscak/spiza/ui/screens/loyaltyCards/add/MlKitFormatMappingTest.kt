package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.google.mlkit.vision.barcode.common.Barcode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test

class MlKitFormatMappingTest {
    @Test
    fun everyFormat_isRecognisedByTheScanner() {
        val scannable = listOf(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_PDF417,
        ).map(::mlKitFormatToLoyaltyCardFormat)

        assertThat(scannable).isEqualTo(LoyaltyCardFormat.entries)
    }

    @Test
    fun unsupportedSymbologies_mapToNull() {
        assertThat(mlKitFormatToLoyaltyCardFormat(Barcode.FORMAT_DATA_MATRIX)).isNull()
        assertThat(mlKitFormatToLoyaltyCardFormat(Barcode.FORMAT_UNKNOWN)).isNull()
    }
}

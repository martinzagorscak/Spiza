package com.mzagorscak.spiza.domain.loyaltyCard

import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoyaltyCardCodesTest {
    private fun isValid(
        value: String,
        format: LoyaltyCardFormat,
    ) = LoyaltyCardCodes.isValid(LoyaltyCardCode(value = value, format = format))

    @Test
    fun isValid_acceptsEan13_whenCheckDigitIsCorrect() {
        assertTrue(isValid("4006381333931", LoyaltyCardFormat.Ean13))
        assertTrue(isValid("3850123456782", LoyaltyCardFormat.Ean13))
    }

    @Test
    fun isValid_rejectsEan13_whenCheckDigitIsWrong() {
        assertFalse(isValid("4006381333932", LoyaltyCardFormat.Ean13))
    }

    @Test
    fun isValid_rejectsEan13_whenLengthOrCharactersAreWrong() {
        assertFalse(isValid("400638133393", LoyaltyCardFormat.Ean13))
        assertFalse(isValid("40063813339A1", LoyaltyCardFormat.Ean13))
    }

    @Test
    fun isValid_checksEan8AndUpcA() {
        assertTrue(isValid("96385074", LoyaltyCardFormat.Ean8))
        assertFalse(isValid("96385075", LoyaltyCardFormat.Ean8))
        assertTrue(isValid("036000291452", LoyaltyCardFormat.UpcA))
        assertFalse(isValid("036000291453", LoyaltyCardFormat.UpcA))
    }

    @Test
    fun isValid_checksUpcE_againstTheExpandedUpcA() {
        assertTrue(isValid("01234565", LoyaltyCardFormat.UpcE))
        assertFalse(isValid("01234566", LoyaltyCardFormat.UpcE))
        assertFalse(isValid("21234565", LoyaltyCardFormat.UpcE))
    }

    @Test
    fun isValid_acceptsCode128_withControlCharacters() {
        assertTrue(isValid("ABC\u001D123", LoyaltyCardFormat.Code128))
    }

    @Test
    fun isValid_rejectsCode128_withNonAsciiOrTooLongContent() {
        assertFalse(isValid("Čevapi", LoyaltyCardFormat.Code128))
        assertFalse(isValid("1".repeat(81), LoyaltyCardFormat.Code128))
    }

    @Test
    fun isValid_rejectsCode39_withLowercaseLetters() {
        assertTrue(isValid("SPIZA-123", LoyaltyCardFormat.Code39))
        assertFalse(isValid("spiza", LoyaltyCardFormat.Code39))
    }

    @Test
    fun isValid_requiresEvenDigitCount_forItf() {
        assertTrue(isValid("12345678901231", LoyaltyCardFormat.Itf))
        assertFalse(isValid("123", LoyaltyCardFormat.Itf))
    }

    @Test
    fun isValid_acceptsCodabar_withOrWithoutGuards() {
        assertTrue(isValid("A40156B", LoyaltyCardFormat.Codabar))
        assertTrue(isValid("40156", LoyaltyCardFormat.Codabar))
        assertFalse(isValid("AB", LoyaltyCardFormat.Codabar))
        assertFalse(isValid("40X56", LoyaltyCardFormat.Codabar))
    }

    @Test
    fun isValid_acceptsAnyText_forTwoDimensionalFormats() {
        assertTrue(isValid("https://example.com/card?id=Čevapi", LoyaltyCardFormat.QrCode))
        assertFalse(isValid("   ", LoyaltyCardFormat.QrCode))
    }

    @Test
    fun detectFormat_recognisesRetailBarcodes_byLengthAndCheckDigit() {
        assertEquals(LoyaltyCardFormat.Ean13, LoyaltyCardCodes.detectFormat("4006381333931"))
        assertEquals(LoyaltyCardFormat.UpcA, LoyaltyCardCodes.detectFormat("036000291452"))
        assertEquals(LoyaltyCardFormat.Ean8, LoyaltyCardCodes.detectFormat("96385074"))
    }

    @Test
    fun detectFormat_fallsBackToCode128_whenCheckDigitDoesNotMatch() {
        assertEquals(LoyaltyCardFormat.Code128, LoyaltyCardCodes.detectFormat("4006381333932"))
        assertEquals(LoyaltyCardFormat.Code128, LoyaltyCardCodes.detectFormat("CARD-0042"))
    }

    @Test
    fun detectFormat_fallsBackToQrCode_whenContentDoesNotFitALinearBarcode() {
        assertEquals(LoyaltyCardFormat.QrCode, LoyaltyCardCodes.detectFormat("Čevapi"))
        assertEquals(LoyaltyCardFormat.QrCode, LoyaltyCardCodes.detectFormat("1".repeat(81)))
    }

    @Test
    fun normalizeScanned_turnsEan13WithLeadingZero_intoUpcA() {
        val normalized = LoyaltyCardCodes.normalizeScanned(LoyaltyCardCode(value = "0036000291452", format = LoyaltyCardFormat.Ean13))

        assertEquals(LoyaltyCardCode(value = "036000291452", format = LoyaltyCardFormat.UpcA), normalized)
    }

    @Test
    fun normalizeScanned_completesShortUpcE() {
        assertEquals(
            LoyaltyCardCode(value = "01234565", format = LoyaltyCardFormat.UpcE),
            LoyaltyCardCodes.normalizeScanned(LoyaltyCardCode(value = "123456", format = LoyaltyCardFormat.UpcE)),
        )
        assertEquals(
            LoyaltyCardCode(value = "01234565", format = LoyaltyCardFormat.UpcE),
            LoyaltyCardCodes.normalizeScanned(LoyaltyCardCode(value = "0123456", format = LoyaltyCardFormat.UpcE)),
        )
    }

    @Test
    fun normalizeScanned_keepsContentUntouched_otherwise() {
        val code = LoyaltyCardCode(value = " 12\u001D34 ", format = LoyaltyCardFormat.Code128)

        assertEquals(code, LoyaltyCardCodes.normalizeScanned(code))
    }
}

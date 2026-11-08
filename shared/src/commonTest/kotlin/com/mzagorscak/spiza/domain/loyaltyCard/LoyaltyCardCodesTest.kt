package com.mzagorscak.spiza.domain.loyaltyCard

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test

class LoyaltyCardCodesTest {
    private fun isValid(
        value: String,
        format: LoyaltyCardFormat,
    ) = LoyaltyCardCodes.isValid(LoyaltyCardCode(value = value, format = format))

    @Test
    fun isValid_checksEan13_lengthDigitsAndCheckDigit() {
        assertThat(isValid("4006381333931", LoyaltyCardFormat.Ean13)).isTrue()
        assertThat(isValid("3850123456782", LoyaltyCardFormat.Ean13)).isTrue()
        assertThat(isValid("4006381333932", LoyaltyCardFormat.Ean13)).isFalse()
        assertThat(isValid("400638133393", LoyaltyCardFormat.Ean13)).isFalse()
        assertThat(isValid("40063813339A1", LoyaltyCardFormat.Ean13)).isFalse()
    }

    @Test
    fun isValid_checksEan8AndUpcA() {
        assertThat(isValid("96385074", LoyaltyCardFormat.Ean8)).isTrue()
        assertThat(isValid("96385075", LoyaltyCardFormat.Ean8)).isFalse()
        assertThat(isValid("036000291452", LoyaltyCardFormat.UpcA)).isTrue()
        assertThat(isValid("036000291453", LoyaltyCardFormat.UpcA)).isFalse()
    }

    @Test
    fun isValid_checksUpcE_againstTheExpandedUpcA() {
        assertThat(isValid("01234565", LoyaltyCardFormat.UpcE)).isTrue()
        assertThat(isValid("01234566", LoyaltyCardFormat.UpcE)).isFalse()
        assertThat(isValid("21234565", LoyaltyCardFormat.UpcE)).isFalse()
    }

    @Test
    fun isValid_checksCode128_isAsciiWithinLinearLength() {
        assertThat(isValid("ABC\u001D123", LoyaltyCardFormat.Code128)).isTrue()
        assertThat(isValid("Čevapi", LoyaltyCardFormat.Code128)).isFalse()
        assertThat(isValid("1".repeat(81), LoyaltyCardFormat.Code128)).isFalse()
    }

    @Test
    fun isValid_rejectsCode39_withLowercaseLetters() {
        assertThat(isValid("SPIZA-123", LoyaltyCardFormat.Code39)).isTrue()
        assertThat(isValid("spiza", LoyaltyCardFormat.Code39)).isFalse()
    }

    @Test
    fun isValid_requiresEvenDigitCount_forItf() {
        assertThat(isValid("12345678901231", LoyaltyCardFormat.Itf)).isTrue()
        assertThat(isValid("123", LoyaltyCardFormat.Itf)).isFalse()
    }

    @Test
    fun isValid_acceptsCodabar_withOrWithoutGuards() {
        assertThat(isValid("A40156B", LoyaltyCardFormat.Codabar)).isTrue()
        assertThat(isValid("40156", LoyaltyCardFormat.Codabar)).isTrue()
        assertThat(isValid("AB", LoyaltyCardFormat.Codabar)).isFalse()
        assertThat(isValid("40X56", LoyaltyCardFormat.Codabar)).isFalse()
    }

    @Test
    fun isValid_acceptsAnyText_forTwoDimensionalFormats() {
        assertThat(isValid("https://example.com/card?id=Čevapi", LoyaltyCardFormat.QrCode)).isTrue()
        assertThat(isValid("   ", LoyaltyCardFormat.QrCode)).isFalse()
    }

    @Test
    fun canEnter_allowsInput_upToTheLongestAcceptedCode() {
        assertThat(LoyaltyCardCodes.canEnter("1".repeat(LoyaltyCardCodes.MAX_CODE_LENGTH))).isTrue()
        assertThat(LoyaltyCardCodes.canEnter("1".repeat(LoyaltyCardCodes.MAX_CODE_LENGTH + 1))).isFalse()
    }

    @Test
    fun detectFormat_recognisesRetailBarcodes_byLengthAndCheckDigit() {
        assertThat(LoyaltyCardCodes.detectFormat("4006381333931")).isEqualTo(LoyaltyCardFormat.Ean13)
        assertThat(LoyaltyCardCodes.detectFormat("036000291452")).isEqualTo(LoyaltyCardFormat.UpcA)
        assertThat(LoyaltyCardCodes.detectFormat("96385074")).isEqualTo(LoyaltyCardFormat.Ean8)
    }

    @Test
    fun detectFormat_fallsBackToCode128_whenCheckDigitDoesNotMatch() {
        assertThat(LoyaltyCardCodes.detectFormat("4006381333932")).isEqualTo(LoyaltyCardFormat.Code128)
        assertThat(LoyaltyCardCodes.detectFormat("CARD-0042")).isEqualTo(LoyaltyCardFormat.Code128)
    }

    @Test
    fun detectFormat_fallsBackToQrCode_whenContentDoesNotFitALinearBarcode() {
        assertThat(LoyaltyCardCodes.detectFormat("Čevapi")).isEqualTo(LoyaltyCardFormat.QrCode)
        assertThat(LoyaltyCardCodes.detectFormat("1".repeat(81))).isEqualTo(LoyaltyCardFormat.QrCode)
    }

    @Test
    fun normalizeScanned_turnsEan13WithLeadingZero_intoUpcA() {
        val normalized = LoyaltyCardCodes.normalizeScanned(LoyaltyCardCode(value = "0036000291452", format = LoyaltyCardFormat.Ean13))

        assertThat(normalized).isEqualTo(LoyaltyCardCode(value = "036000291452", format = LoyaltyCardFormat.UpcA))
    }

    @Test
    fun normalizeScanned_completesShortUpcE() {
        val expected = LoyaltyCardCode(value = "01234565", format = LoyaltyCardFormat.UpcE)

        val sixDigits = LoyaltyCardCodes.normalizeScanned(LoyaltyCardCode(value = "123456", format = LoyaltyCardFormat.UpcE))
        val sevenDigits = LoyaltyCardCodes.normalizeScanned(LoyaltyCardCode(value = "0123456", format = LoyaltyCardFormat.UpcE))

        assertThat(sixDigits).isEqualTo(expected)
        assertThat(sevenDigits).isEqualTo(expected)
    }

    @Test
    fun normalizeScanned_keepsContentUntouched_otherwise() {
        val code = LoyaltyCardCode(value = " 12\u001D34 ", format = LoyaltyCardFormat.Code128)

        assertThat(LoyaltyCardCodes.normalizeScanned(code)).isEqualTo(code)
    }
}

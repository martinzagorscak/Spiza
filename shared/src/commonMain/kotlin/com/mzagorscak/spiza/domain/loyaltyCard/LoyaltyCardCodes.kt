package com.mzagorscak.spiza.domain.loyaltyCard

import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

private const val MAX_LINEAR_CODE_LENGTH = 80
private const val MAX_TWO_DIMENSIONAL_CODE_LENGTH = 1000
private const val MAX_ASCII = 127
private const val EAN_13_LENGTH = 13
private const val EAN_8_LENGTH = 8
private const val UPC_A_LENGTH = 12
private const val UPC_E_LENGTH = 8
private const val UPC_E_LENGTH_WITHOUT_CHECK_DIGIT = 7
private const val UPC_E_LENGTH_WITHOUT_NUMBER_SYSTEM = 6
private const val CODE_39_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
private const val CODABAR_CHARACTERS = "0123456789-$:/.+"
private const val CODABAR_GUARDS = "ABCD"

/** Validation, detection and normalization rules for loyalty card codes. */
internal object LoyaltyCardCodes {
    /** The longest content any format accepts; longer input can never become a valid card. */
    const val MAX_CODE_LENGTH = MAX_TWO_DIMENSIONAL_CODE_LENGTH

    fun canEnter(value: String): Boolean = value.length <= MAX_CODE_LENGTH

    fun isValid(code: LoyaltyCardCode): Boolean {
        val value = code.value
        if (value.isBlank()) return false
        return when (code.format) {
            LoyaltyCardFormat.Ean13 -> value.isGtin(length = EAN_13_LENGTH)
            LoyaltyCardFormat.Ean8 -> value.isGtin(length = EAN_8_LENGTH)
            LoyaltyCardFormat.UpcA -> value.isGtin(length = UPC_A_LENGTH)
            LoyaltyCardFormat.UpcE -> value.isValidUpcE()
            LoyaltyCardFormat.Code128, LoyaltyCardFormat.Code93 -> value.isLinearLength() && value.isAscii()
            LoyaltyCardFormat.Code39 -> value.isLinearLength() && value.all { it in CODE_39_CHARACTERS }
            LoyaltyCardFormat.Itf -> value.isLinearLength() && value.length % 2 == 0 && value.isDigits()
            LoyaltyCardFormat.Codabar -> value.isLinearLength() && value.isValidCodabar()
            LoyaltyCardFormat.QrCode, LoyaltyCardFormat.Aztec, LoyaltyCardFormat.Pdf417 ->
                value.length <= MAX_TWO_DIMENSIONAL_CODE_LENGTH
        }
    }

    /** Best guess for a typed-in code: retail barcodes are recognised by length and check digit, anything else is Code 128. */
    fun detectFormat(value: String): LoyaltyCardFormat =
        when {
            value.isGtin(length = EAN_13_LENGTH) -> LoyaltyCardFormat.Ean13
            value.isGtin(length = UPC_A_LENGTH) -> LoyaltyCardFormat.UpcA
            value.isGtin(length = EAN_8_LENGTH) -> LoyaltyCardFormat.Ean8
            value.isLinearLength() && value.isAscii() -> LoyaltyCardFormat.Code128
            else -> LoyaltyCardFormat.QrCode
        }

    /**
     * Scanners differ in how they report the same barcode: iOS reports UPC-A as EAN-13 with a leading zero
     * (the bars are identical) and some report UPC-E without the number system or check digit.
     * The content is otherwise kept exactly as scanned.
     */
    fun normalizeScanned(code: LoyaltyCardCode): LoyaltyCardCode {
        val value = code.value
        return when {
            code.format == LoyaltyCardFormat.Ean13 && value.length == EAN_13_LENGTH && value.startsWith('0') && value.isDigits() ->
                LoyaltyCardCode(value = value.drop(1), format = LoyaltyCardFormat.UpcA)

            code.format == LoyaltyCardFormat.UpcE && value.isDigits() && value.length == UPC_E_LENGTH_WITHOUT_NUMBER_SYSTEM ->
                code.copy(value = "0$value".withUpcECheckDigit())

            code.format == LoyaltyCardFormat.UpcE && value.isDigits() && value.length == UPC_E_LENGTH_WITHOUT_CHECK_DIGIT ->
                code.copy(value = value.withUpcECheckDigit())

            else -> code
        }
    }

    /** GS1 check digit for [digits] that do not yet contain one. */
    fun gtinCheckDigit(digits: String): Int {
        val sum = digits.reversed().foldIndexed(0) { index, acc, char ->
            acc + char.digitToInt() * if (index % 2 == 0) 3 else 1
        }
        return (10 - sum % 10) % 10
    }

    /** Expands an 8-digit UPC-E (number system + 6 digits + check digit) to the 11 UPC-A digits without the check digit. */
    fun expandUpcE(value: String): String {
        val numberSystem = value[0]
        val digits = value.substring(1, 7)
        val body = when (val last = digits[5]) {
            '0', '1', '2' -> digits.substring(0, 2) + last + "0000" + digits.substring(2, 5)
            '3' -> digits.substring(0, 3) + "00000" + digits.substring(3, 5)
            '4' -> digits.substring(0, 4) + "00000" + digits[4]
            else -> digits.substring(0, 5) + "0000" + last
        }
        return numberSystem + body
    }

    private fun String.withUpcECheckDigit(): String = this + gtinCheckDigit(expandUpcE(this + "0"))

    private fun String.isValidUpcE(): Boolean =
        length == UPC_E_LENGTH &&
            isDigits() &&
            (this[0] == '0' || this[0] == '1') &&
            gtinCheckDigit(expandUpcE(this)) == last().digitToInt()

    private fun String.isGtin(length: Int): Boolean =
        this.length == length && isDigits() && gtinCheckDigit(dropLast(1)) == last().digitToInt()

    private fun String.isValidCodabar(): Boolean {
        val isGuarded = length >= 2 && first() in CODABAR_GUARDS && last() in CODABAR_GUARDS
        val body = if (isGuarded) substring(1, length - 1) else this
        return body.isNotEmpty() && body.all { it in CODABAR_CHARACTERS }
    }

    private fun String.isLinearLength(): Boolean = length in 1..MAX_LINEAR_CODE_LENGTH

    private fun String.isDigits(): Boolean = isNotEmpty() && all { it in '0'..'9' }

    private fun String.isAscii(): Boolean = all { it.code <= MAX_ASCII }
}

package com.mzagorscak.spiza.ui.components.barcode

import com.mzagorscak.spiza.domain.loyaltyCard.LoyaltyCardCodes
import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

// Wide elements are 3x narrow: the spec allows 2-3x, and phone screens have tiny modules where 3x scans more reliably.
private const val WIDE = 3
private const val NARROW = 1

private const val EAN_GUARD = "101"
private const val EAN_CENTER_GUARD = "01010"
private const val UPC_E_END_GUARD = "010101"
private val EAN_L_CODES =
    listOf("0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011")
private val EAN_R_CODES = EAN_L_CODES.map { code -> code.map { if (it == '0') '1' else '0' }.joinToString("") }
private val EAN_G_CODES = EAN_R_CODES.map { it.reversed() }
private val EAN_13_PARITY = listOf("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")

// Number system 0, indexed by check digit; number system 1 uses the inverse. E = even parity (G code), O = odd (L code).
private val UPC_E_PARITY = listOf("EEEOOO", "EEOEOO", "EEOOEO", "EEOOOE", "EOEEOO", "EOOEEO", "EOOOEE", "EOEOEO", "EOEOOE", "EOOEOE")

// Internal so the host test can compare it with ZXing's reference table.
internal val CODE_128_PATTERNS = listOf(
    "212222",
    "222122",
    "222221",
    "121223",
    "121322",
    "131222",
    "122213",
    "122312",
    "132212",
    "221213",
    "221312",
    "231212",
    "112232",
    "122132",
    "122231",
    "113222",
    "123122",
    "123221",
    "223211",
    "221132",
    "221231",
    "213212",
    "223112",
    "312131",
    "311222",
    "321122",
    "321221",
    "312212",
    "322112",
    "322211",
    "212123",
    "212321",
    "232121",
    "111323",
    "131123",
    "131321",
    "112313",
    "132113",
    "132311",
    "211313",
    "231113",
    "231311",
    "112133",
    "112331",
    "132131",
    "113123",
    "113321",
    "133121",
    "313121",
    "211331",
    "231131",
    "213113",
    "213311",
    "213131",
    "311123",
    "311321",
    "331121",
    "312113",
    "312311",
    "332111",
    "314111",
    "221411",
    "431111",
    "111224",
    "111422",
    "121124",
    "121421",
    "141122",
    "141221",
    "112214",
    "112412",
    "122114",
    "122411",
    "142112",
    "142211",
    "241211",
    "221114",
    "413111",
    "241112",
    "134111",
    "111242",
    "121142",
    "121241",
    "114212",
    "124112",
    "124211",
    "411212",
    "421112",
    "421211",
    "212141",
    "214121",
    "412121",
    "111143",
    "111341",
    "131141",
    "114113",
    "114311",
    "411113",
    "411311",
    "113141",
    "114131",
    "311141",
    "411131",
    "211412",
    "211214",
    "211232",
    "2331112",
)
private const val CODE_128_CODE_C = 99
private const val CODE_128_CODE_B = 100
private const val CODE_128_CODE_A = 101
private const val CODE_128_START_A = 103
private const val CODE_128_START_B = 104
private const val CODE_128_START_C = 105
private const val CODE_128_STOP = 106
private const val CODE_128_CHECKSUM_MODULO = 103
private const val CODE_128_MIN_DIGITS_FOR_CODE_C = 4

private const val CODE_39_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*"
private val CODE_39_ENCODINGS = intArrayOf(
    0x034,
    0x121,
    0x061,
    0x160,
    0x031,
    0x130,
    0x070,
    0x025,
    0x124,
    0x064,
    0x109,
    0x049,
    0x148,
    0x019,
    0x118,
    0x058,
    0x00D,
    0x10C,
    0x04C,
    0x01C,
    0x103,
    0x043,
    0x142,
    0x013,
    0x112,
    0x052,
    0x007,
    0x106,
    0x046,
    0x016,
    0x181,
    0x0C1,
    0x1C0,
    0x091,
    0x190,
    0x0D0,
    0x085,
    0x184,
    0x0C4,
    0x0A8,
    0x0A2,
    0x08A,
    0x02A,
    0x094,
)
private const val CODE_39_ELEMENTS = 9

// a-d are the Code 93 shift characters ($), (%), (/) and (+) used for full ASCII.
private const val CODE_93_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*"
private val CODE_93_ENCODINGS = intArrayOf(
    0x114,
    0x148,
    0x144,
    0x142,
    0x128,
    0x124,
    0x122,
    0x150,
    0x112,
    0x10A,
    0x1A8,
    0x1A4,
    0x1A2,
    0x194,
    0x192,
    0x18A,
    0x168,
    0x164,
    0x162,
    0x134,
    0x11A,
    0x158,
    0x14C,
    0x146,
    0x12C,
    0x116,
    0x1B4,
    0x1B2,
    0x1AC,
    0x1A6,
    0x196,
    0x19A,
    0x16C,
    0x166,
    0x136,
    0x13A,
    0x12E,
    0x1D4,
    0x1D2,
    0x1CA,
    0x16E,
    0x176,
    0x1AE,
    0x126,
    0x1DA,
    0x1D6,
    0x132,
    0x15E,
)
private const val CODE_93_MODULES_PER_CHARACTER = 9
private const val CODE_93_C_MAX_WEIGHT = 20
private const val CODE_93_K_MAX_WEIGHT = 15
private const val CODE_93_MODULO = 47

private val ITF_PATTERNS = listOf("nnwwn", "wnnnw", "nwnnw", "wwnnn", "nnwnw", "wnwnn", "nwwnn", "nnnww", "wnnwn", "nwnwn")
private val ITF_START = listOf(NARROW, NARROW, NARROW, NARROW)
private val ITF_END = listOf(WIDE, NARROW, NARROW)

private const val CODABAR_ALPHABET = "0123456789-$:/.+ABCD"
private val CODABAR_ENCODINGS = intArrayOf(
    0x003,
    0x006,
    0x009,
    0x060,
    0x012,
    0x042,
    0x021,
    0x024,
    0x030,
    0x048,
    0x00C,
    0x018,
    0x045,
    0x051,
    0x054,
    0x015,
    0x01A,
    0x029,
    0x00B,
    0x00E,
)
private const val CODABAR_ELEMENTS = 7
private const val CODABAR_GUARDS = "ABCD"
private const val CODABAR_DEFAULT_GUARD = 'A'

/** Encodes linear (1D) barcodes into modules (`true` = bar), without quiet zones. */
internal object LinearBarcodeEncoder {
    fun encode(
        content: String,
        format: LoyaltyCardFormat,
    ): BooleanArray? {
        if (format.isTwoDimensional || LoyaltyCardCodes.isValid(LoyaltyCardCode(value = content, format = format)).not()) return null
        return when (format) {
            LoyaltyCardFormat.Ean13 -> encodeEan13(content)
            LoyaltyCardFormat.Ean8 -> encodeEan8(content)
            // UPC-A is an EAN-13 with a leading zero: the bars are identical.
            LoyaltyCardFormat.UpcA -> encodeEan13("0$content")
            LoyaltyCardFormat.UpcE -> encodeUpcE(content)
            LoyaltyCardFormat.Code128 -> encodeCode128(content)
            LoyaltyCardFormat.Code39 -> encodeCode39(content)
            LoyaltyCardFormat.Code93 -> encodeCode93(content)
            LoyaltyCardFormat.Itf -> encodeItf(content)
            LoyaltyCardFormat.Codabar -> encodeCodabar(content)
            LoyaltyCardFormat.QrCode, LoyaltyCardFormat.Aztec, LoyaltyCardFormat.Pdf417 -> null
        }
    }

    private fun encodeEan13(digits: String): BooleanArray =
        modules {
            val parity = EAN_13_PARITY[digits[0].digitToInt()]
            pattern(EAN_GUARD)
            for (i in 1..6) {
                val digit = digits[i].digitToInt()
                pattern(if (parity[i - 1] == 'L') EAN_L_CODES[digit] else EAN_G_CODES[digit])
            }
            pattern(EAN_CENTER_GUARD)
            for (i in 7..12) pattern(EAN_R_CODES[digits[i].digitToInt()])
            pattern(EAN_GUARD)
        }

    private fun encodeEan8(digits: String): BooleanArray =
        modules {
            pattern(EAN_GUARD)
            for (i in 0..3) pattern(EAN_L_CODES[digits[i].digitToInt()])
            pattern(EAN_CENTER_GUARD)
            for (i in 4..7) pattern(EAN_R_CODES[digits[i].digitToInt()])
            pattern(EAN_GUARD)
        }

    private fun encodeUpcE(digits: String): BooleanArray =
        modules {
            val isNumberSystemOne = digits[0] == '1'
            val parity = UPC_E_PARITY[digits[7].digitToInt()]
            pattern(EAN_GUARD)
            for (i in 1..6) {
                val digit = digits[i].digitToInt()
                val isEven = (parity[i - 1] == 'E') != isNumberSystemOne
                pattern(if (isEven) EAN_G_CODES[digit] else EAN_L_CODES[digit])
            }
            pattern(UPC_E_END_GUARD)
        }

    private enum class CodeSet { A, B, C }

    private fun encodeCode128(content: String): BooleanArray {
        val values = mutableListOf<Int>()
        var codeSet: CodeSet? = null
        var index = 0
        while (index < content.length) {
            val digitRun = content.countDigitsFrom(index)
            val useCodeC = when (codeSet) {
                CodeSet.C -> digitRun >= 2
                null -> digitRun >= CODE_128_MIN_DIGITS_FOR_CODE_C || (digitRun == 2 && content.length == 2)
                else -> digitRun >= CODE_128_MIN_DIGITS_FOR_CODE_C
            }
            if (useCodeC) {
                if (codeSet != CodeSet.C) {
                    values += if (codeSet == null) CODE_128_START_C else CODE_128_CODE_C
                    codeSet = CodeSet.C
                }
                values += content.substring(index, index + 2).toInt()
                index += 2
            } else {
                val char = content[index].code
                val target = when {
                    char < ' '.code -> CodeSet.A
                    char >= '`'.code -> CodeSet.B
                    codeSet == CodeSet.A || codeSet == CodeSet.B -> codeSet
                    else -> CodeSet.B
                }
                if (codeSet != target) {
                    values += when {
                        codeSet == null && target == CodeSet.A -> CODE_128_START_A
                        codeSet == null -> CODE_128_START_B
                        target == CodeSet.A -> CODE_128_CODE_A
                        else -> CODE_128_CODE_B
                    }
                    codeSet = target
                }
                // Code set A maps control characters to 64-95; printable characters share the same values in A and B.
                values += if (char < ' '.code) char + 64 else char - ' '.code
                index++
            }
        }
        val checksum = values.foldIndexed(0) { position, acc, value -> acc + value * maxOf(position, 1) } % CODE_128_CHECKSUM_MODULO
        values += checksum
        values += CODE_128_STOP
        return modules {
            values.forEach { value -> widths(CODE_128_PATTERNS[value].map { it.digitToInt() }) }
        }
    }

    private fun encodeCode39(content: String): BooleanArray =
        modules {
            val characters = "*$content*"
            characters.forEachIndexed { index, char ->
                val encoding = CODE_39_ENCODINGS[CODE_39_ALPHABET.indexOf(char)]
                widths(narrowWideWidths(encoding = encoding, elements = CODE_39_ELEMENTS))
                if (index < characters.lastIndex) space(NARROW)
            }
        }

    private fun encodeCode93(content: String): BooleanArray {
        val data = content.toCode93Extended().map { CODE_93_ALPHABET.indexOf(it) }
        val checkC = code93Checksum(values = data, maxWeight = CODE_93_C_MAX_WEIGHT)
        val checkK = code93Checksum(values = data + checkC, maxWeight = CODE_93_K_MAX_WEIGHT)
        val asterisk = CODE_93_ALPHABET.indexOf('*')
        return modules {
            (listOf(asterisk) + data + checkC + checkK + asterisk).forEach { value ->
                val encoding = CODE_93_ENCODINGS[value]
                for (bit in CODE_93_MODULES_PER_CHARACTER - 1 downTo 0) module(encoding shr bit and 1 == 1)
            }
            // Termination bar
            module(true)
        }
    }

    private fun encodeItf(digits: String): BooleanArray =
        modules {
            widths(ITF_START)
            for (pair in digits.indices step 2) {
                val bars = ITF_PATTERNS[digits[pair].digitToInt()]
                val spaces = ITF_PATTERNS[digits[pair + 1].digitToInt()]
                for (element in 0 until 5) {
                    bar(if (bars[element] == 'w') WIDE else NARROW)
                    space(if (spaces[element] == 'w') WIDE else NARROW)
                }
            }
            widths(ITF_END)
        }

    private fun encodeCodabar(content: String): BooleanArray =
        modules {
            val isGuarded = content.length >= 2 && content.first() in CODABAR_GUARDS && content.last() in CODABAR_GUARDS
            val characters = if (isGuarded) content else "$CODABAR_DEFAULT_GUARD$content$CODABAR_DEFAULT_GUARD"
            characters.forEachIndexed { index, char ->
                val encoding = CODABAR_ENCODINGS[CODABAR_ALPHABET.indexOf(char)]
                widths(narrowWideWidths(encoding = encoding, elements = CODABAR_ELEMENTS))
                if (index < characters.lastIndex) space(NARROW)
            }
        }

    private fun narrowWideWidths(
        encoding: Int,
        elements: Int,
    ): List<Int> = (elements - 1 downTo 0).map { bit -> if (encoding shr bit and 1 == 1) WIDE else NARROW }

    private fun code93Checksum(
        values: List<Int>,
        maxWeight: Int,
    ): Int {
        var weight = 1
        var total = 0
        for (index in values.indices.reversed()) {
            total += values[index] * weight
            weight = if (weight == maxWeight) 1 else weight + 1
        }
        return total % CODE_93_MODULO
    }

    /** Full ASCII Code 93: characters outside the basic alphabet become a shift character plus a letter. */
    private fun String.toCode93Extended(): String =
        buildString {
            for (char in this@toCode93Extended) {
                val code = char.code
                when {
                    code == 0 -> append("bU")
                    code <= 26 -> append('a').append('A' + (code - 1))
                    code <= 31 -> append('b').append('A' + (code - 27))
                    char == ' ' || char == '$' || char == '%' || char == '+' -> append(char)
                    char <= ',' -> append('c').append('A' + (char - '!'))
                    char <= '9' -> append(char)
                    char == ':' -> append("cZ")
                    char <= '?' -> append('b').append('F' + (char - ';'))
                    char == '@' -> append("bV")
                    char <= 'Z' -> append(char)
                    char <= '_' -> append('b').append('K' + (char - '['))
                    char == '`' -> append("bW")
                    char <= 'z' -> append('d').append('A' + (char - 'a'))
                    else -> append('b').append('P' + (char - '{'))
                }
            }
        }

    private fun String.countDigitsFrom(start: Int): Int {
        var end = start
        while (end < length && this[end] in '0'..'9') end++
        return end - start
    }
}

private class ModuleBuilder {
    private val modules = mutableListOf<Boolean>()

    fun module(isDark: Boolean) {
        modules += isDark
    }

    fun pattern(bits: String) = bits.forEach { module(it == '1') }

    fun bar(width: Int) = repeat(width) { module(true) }

    fun space(width: Int) = repeat(width) { module(false) }

    /** Alternating bar/space widths, starting with a bar. */
    fun widths(widths: List<Int>) =
        widths.forEachIndexed { index, width ->
            if (index % 2 == 0) bar(width) else space(width)
        }

    fun build(): BooleanArray = modules.toBooleanArray()
}

private inline fun modules(block: ModuleBuilder.() -> Unit): BooleanArray = ModuleBuilder().apply(block).build()

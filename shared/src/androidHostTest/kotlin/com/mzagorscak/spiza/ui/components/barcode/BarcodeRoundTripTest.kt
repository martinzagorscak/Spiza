package com.mzagorscak.spiza.ui.components.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val PIXELS_PER_MODULE = 4
private const val LINEAR_HEIGHT_PX = 80
private const val QUIET_ZONE_MODULES = 10
private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

/**
 * Encodes with the app's encoders and decodes with ZXing, the closest thing to a checkout scanner in a unit test:
 * every code must come back with exactly the same content and symbology.
 */
class BarcodeRoundTripTest {
    private fun assertRoundTrip(
        content: String,
        format: LoyaltyCardFormat,
        expectedFormat: BarcodeFormat,
        expectedText: String = content,
        extraHints: Map<DecodeHintType, Any> = emptyMap(),
    ) {
        val matrix = assertNotNull(encodeBarcode(content = content, format = format), "Could not encode $content as $format")
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(expectedFormat),
            DecodeHintType.TRY_HARDER to true,
        ) + extraHints

        val result = MultiFormatReader().decode(matrix.toBinaryBitmap(), hints)

        assertEquals(expectedFormat, result.barcodeFormat)
        assertEquals(expectedText, result.text)
    }

    @Test
    fun ean13_roundTrips() {
        assertRoundTrip("4006381333931", LoyaltyCardFormat.Ean13, BarcodeFormat.EAN_13)
        assertRoundTrip("3850123456782", LoyaltyCardFormat.Ean13, BarcodeFormat.EAN_13)
        // Covers every first digit, i.e. every parity pattern
        listOf(
            "0000000000000",
            "1000000000009",
            "2000000000008",
            "5901234123457",
            "6000000000004",
            "7000000000003",
            "8000000000002",
            "9780201379624",
        ).forEach {
            assertRoundTrip(it, LoyaltyCardFormat.Ean13, BarcodeFormat.EAN_13)
        }
    }

    @Test
    fun ean8_upcA_and_upcE_roundTrip() {
        assertRoundTrip("96385074", LoyaltyCardFormat.Ean8, BarcodeFormat.EAN_8)
        assertRoundTrip("036000291452", LoyaltyCardFormat.UpcA, BarcodeFormat.UPC_A)
        assertRoundTrip("01234565", LoyaltyCardFormat.UpcE, BarcodeFormat.UPC_E)
        assertRoundTrip("04252614", LoyaltyCardFormat.UpcE, BarcodeFormat.UPC_E)
        assertRoundTrip("11234562", LoyaltyCardFormat.UpcE, BarcodeFormat.UPC_E)
    }

    @Test
    fun code128_roundTrips_inEveryCodeSet() {
        assertRoundTrip("ABC-123", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("12", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("12345678901234567890", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("CARD12345X", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("12345abc", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("0104012345\u001D1726", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("ab\u0001\u001FCD`~", LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
    }

    @Test
    fun code128_roundTrips_everyCharacterAndDigitPair() {
        (32..127).map { it.toChar() }.chunked(40).forEach { chunk ->
            assertRoundTrip(chunk.joinToString(""), LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        }
        (0..31).map { it.toChar() }.chunked(16).forEach { chunk ->
            assertRoundTrip(chunk.joinToString(""), LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        }
        (0..99).joinToString("") { it.toString().padStart(2, '0') }.chunked(40).forEach { chunk ->
            assertRoundTrip(chunk, LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        }
    }

    @Test
    fun code128_patternTable_matchesZxing() {
        val field = Class.forName("com.google.zxing.oned.Code128Reader").getDeclaredField("CODE_PATTERNS")
        field.isAccessible = true
        val reference = (field.get(null) as Array<*>).map { (it as IntArray).joinToString("") }

        assertEquals(reference, CODE_128_PATTERNS)
    }

    @Test
    fun code39_roundTrips_everyCharacter() {
        assertRoundTrip("SPIZA-123", LoyaltyCardFormat.Code39, BarcodeFormat.CODE_39)
        assertRoundTrip("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", LoyaltyCardFormat.Code39, BarcodeFormat.CODE_39)
        assertRoundTrip("A-. $/+%Z", LoyaltyCardFormat.Code39, BarcodeFormat.CODE_39)
    }

    @Test
    fun code93_roundTrips_fullAscii() {
        assertRoundTrip("SPIZA-93", LoyaltyCardFormat.Code93, BarcodeFormat.CODE_93)
        assertRoundTrip("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%", LoyaltyCardFormat.Code93, BarcodeFormat.CODE_93)
        (0..127).map { it.toChar() }.chunked(32).forEach { chunk ->
            assertRoundTrip(chunk.joinToString(""), LoyaltyCardFormat.Code93, BarcodeFormat.CODE_93)
        }
    }

    @Test
    fun itf_roundTrips() {
        assertRoundTrip("12345678901231", LoyaltyCardFormat.Itf, BarcodeFormat.ITF)
        assertRoundTrip("0123456789", LoyaltyCardFormat.Itf, BarcodeFormat.ITF)
    }

    @Test
    fun codabar_roundTrips_andAddsGuards_whenMissing() {
        val keepGuards = mapOf<DecodeHintType, Any>(DecodeHintType.RETURN_CODABAR_START_END to true)
        assertRoundTrip("A40156B", LoyaltyCardFormat.Codabar, BarcodeFormat.CODABAR, extraHints = keepGuards)
        assertRoundTrip("C0123456789-$:/.+D", LoyaltyCardFormat.Codabar, BarcodeFormat.CODABAR, extraHints = keepGuards)
        assertRoundTrip("40156", LoyaltyCardFormat.Codabar, BarcodeFormat.CODABAR, expectedText = "A40156A", extraHints = keepGuards)
    }

    @Test
    fun twoDimensionalCodes_roundTrip() {
        assertRoundTrip("https://example.com/card?id=123456", LoyaltyCardFormat.QrCode, BarcodeFormat.QR_CODE)
        assertRoundTrip("Kartica: Čevapi & Šljivovica", LoyaltyCardFormat.QrCode, BarcodeFormat.QR_CODE)
        assertRoundTrip("SPIZA 1234567890", LoyaltyCardFormat.Aztec, BarcodeFormat.AZTEC)
        assertRoundTrip("SPIZA PDF417 1234567890", LoyaltyCardFormat.Pdf417, BarcodeFormat.PDF_417)
    }

    @Test
    fun everyFormat_hasARoundTripSample() {
        // Fails when a format is added without teaching the encoder (and this test) about it.
        val samples = mapOf(
            LoyaltyCardFormat.Ean13 to ("4006381333931" to BarcodeFormat.EAN_13),
            LoyaltyCardFormat.Ean8 to ("96385074" to BarcodeFormat.EAN_8),
            LoyaltyCardFormat.UpcA to ("036000291452" to BarcodeFormat.UPC_A),
            LoyaltyCardFormat.UpcE to ("01234565" to BarcodeFormat.UPC_E),
            LoyaltyCardFormat.Code128 to ("CARD-128" to BarcodeFormat.CODE_128),
            LoyaltyCardFormat.Code39 to ("CARD-39" to BarcodeFormat.CODE_39),
            LoyaltyCardFormat.Code93 to ("CARD-93" to BarcodeFormat.CODE_93),
            LoyaltyCardFormat.Itf to ("0123456789" to BarcodeFormat.ITF),
            LoyaltyCardFormat.Codabar to ("A40156B" to BarcodeFormat.CODABAR),
            LoyaltyCardFormat.QrCode to ("CARD QR" to BarcodeFormat.QR_CODE),
            LoyaltyCardFormat.Aztec to ("CARD AZTEC" to BarcodeFormat.AZTEC),
            LoyaltyCardFormat.Pdf417 to ("CARD PDF417" to BarcodeFormat.PDF_417),
        )
        val keepGuards = mapOf<DecodeHintType, Any>(DecodeHintType.RETURN_CODABAR_START_END to true)

        assertEquals(LoyaltyCardFormat.entries.toSet(), samples.keys)
        samples.forEach { (format, sample) ->
            assertRoundTrip(sample.first, format, sample.second, extraHints = keepGuards)
        }
    }

    @Test
    fun linearCodes_roundTrip_atTheMaximumAcceptedLength() {
        assertRoundTrip("A".repeat(80), LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertRoundTrip("12".repeat(40), LoyaltyCardFormat.Code128, BarcodeFormat.CODE_128)
        assertNull(encodeBarcode(content = "A".repeat(81), format = LoyaltyCardFormat.Code128))
    }

    @Test
    fun twoDimensionalCodes_encode_theLongestAcceptedContent() {
        // The validator accepts up to 1000 characters: anything it accepts must also render, or the saved card shows no code.
        val longest = "1234567890".repeat(100)
        listOf(LoyaltyCardFormat.QrCode, LoyaltyCardFormat.Aztec, LoyaltyCardFormat.Pdf417).forEach { format ->
            assertNotNull(encodeBarcode(content = longest, format = format), "$format could not encode 1000 characters")
        }
    }

    @Test
    fun encodeBarcode_returnsNull_whenContentIsInvalidForTheFormat() {
        assertNull(encodeBarcode(content = "4006381333932", format = LoyaltyCardFormat.Ean13))
        assertNull(encodeBarcode(content = "lowercase", format = LoyaltyCardFormat.Code39))
        assertNull(encodeBarcode(content = "", format = LoyaltyCardFormat.QrCode))
    }
}

private fun BarcodeMatrix.toBinaryBitmap(): BinaryBitmap {
    val quietZone = QUIET_ZONE_MODULES * PIXELS_PER_MODULE
    val moduleHeight = if (isLinear) LINEAR_HEIGHT_PX else PIXELS_PER_MODULE
    val imageWidth = width * PIXELS_PER_MODULE + 2 * quietZone
    val imageHeight = height * moduleHeight + 2 * quietZone
    val pixels = IntArray(imageWidth * imageHeight) { WHITE }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (isDark(x = x, y = y).not()) continue
            for (dy in 0 until moduleHeight) {
                for (dx in 0 until PIXELS_PER_MODULE) {
                    val px = quietZone + x * PIXELS_PER_MODULE + dx
                    val py = quietZone + y * moduleHeight + dy
                    pixels[py * imageWidth + px] = BLACK
                }
            }
        }
    }
    return BinaryBitmap(HybridBinarizer(RGBLuminanceSource(imageWidth, imageHeight, pixels)))
}

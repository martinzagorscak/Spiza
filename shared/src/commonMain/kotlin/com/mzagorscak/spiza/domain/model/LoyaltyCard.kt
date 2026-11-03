package com.mzagorscak.spiza.domain.model

/**
 * The symbology a card code is rendered in. A card must be shown in the same symbology it was printed in:
 * checkout scanners often have only a few symbologies enabled, so the same digits as another barcode type do not scan.
 * The names are persisted in Room — renaming one needs a migration.
 */
enum class LoyaltyCardFormat(
    val isTwoDimensional: Boolean,
) {
    Ean13(isTwoDimensional = false),
    Ean8(isTwoDimensional = false),
    UpcA(isTwoDimensional = false),
    UpcE(isTwoDimensional = false),
    Code128(isTwoDimensional = false),
    Code39(isTwoDimensional = false),
    Code93(isTwoDimensional = false),
    Itf(isTwoDimensional = false),
    Codabar(isTwoDimensional = false),
    QrCode(isTwoDimensional = true),
    Aztec(isTwoDimensional = true),
    Pdf417(isTwoDimensional = true),
}

/** [value] is the exact encoded content (may contain control characters such as the GS1 separator). */
data class LoyaltyCardCode(
    val value: String,
    val format: LoyaltyCardFormat,
)

data class LoyaltyCard(
    val id: Long = 0L,
    val name: String,
    val colorArgb: Int,
    val codeText: String,
    val format: LoyaltyCardFormat,
)

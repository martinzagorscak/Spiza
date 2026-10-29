package com.mzagorscak.spiza.domain.model

enum class LoyaltyCardFormat {
    Barcode,
    QrCode,
}

data class LoyaltyCard(
    val id: Long = 0L,
    val name: String,
    val colorArgb: Int,
    val codeText: String,
    val format: LoyaltyCardFormat,
)

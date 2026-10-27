package com.mzagorscak.spiza.domain.model

data class LoyaltyCard(
    val id: Long = 0L,
    val name: String,
    val colorArgb: Int,
    val codeText: String,
    val isQrFormat: Boolean,
)

package com.mzagorscak.spiza.ui.model

import androidx.compose.ui.graphics.Color
import com.mzagorscak.spiza.domain.model.LoyaltyCard

data class PresentableLoyaltyCard(
    val id: Long = 0L,
    val name: String,
    val color: Color,
    val codeText: String,
    val isQrFormat: Boolean,
)

fun LoyaltyCard.toPresentable(): PresentableLoyaltyCard =
    PresentableLoyaltyCard(
        id = id,
        name = name,
        color = Color(colorArgb),
        codeText = codeText,
        isQrFormat = isQrFormat,
    )

fun List<LoyaltyCard>.toPresentable(): List<PresentableLoyaltyCard> = map { it.toPresentable() }

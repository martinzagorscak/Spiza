package com.mzagorscak.spiza.ui.model

import androidx.compose.ui.graphics.Color
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

data class PresentableLoyaltyCard(
    val id: Long = 0L,
    val name: String,
    val color: Color,
    val codeText: String,
    val format: LoyaltyCardFormat,
)

fun LoyaltyCard.toPresentable(): PresentableLoyaltyCard =
    PresentableLoyaltyCard(
        id = id,
        name = name,
        color = Color(colorArgb),
        codeText = codeText,
        format = format,
    )

fun List<LoyaltyCard>.toPresentable(): List<PresentableLoyaltyCard> = map { it.toPresentable() }

package com.mzagorscak.spiza.data.loyaltyCard.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import kotlin.time.Clock

@Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorArgb: Int,
    val codeText: String,
    val isQrFormat: Boolean,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
) {
    fun toDomainModel(): LoyaltyCard =
        LoyaltyCard(
            id = id,
            name = name,
            colorArgb = colorArgb,
            codeText = codeText,
            isQrFormat = isQrFormat,
        )
}

fun LoyaltyCard.toEntity(): LoyaltyCardEntity =
    LoyaltyCardEntity(
        id = id,
        name = name,
        colorArgb = colorArgb,
        codeText = codeText,
        isQrFormat = isQrFormat,
    )

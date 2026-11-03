package com.mzagorscak.spiza.ui.model

import androidx.compose.ui.graphics.Color
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

private const val DIGIT_GROUP_SIZE = 4

/** [codeText] is the exact encoded content; [displayCode] is the human-readable version printed under the barcode. */
data class PresentableLoyaltyCard(
    val id: Long = 0L,
    val name: String,
    val color: Color,
    val codeText: String,
    val format: LoyaltyCardFormat,
) {
    val displayCode: String = codeText.toDisplayCode()
}

fun LoyaltyCard.toPresentable(): PresentableLoyaltyCard =
    PresentableLoyaltyCard(
        id = id,
        name = name,
        color = Color(colorArgb),
        codeText = codeText,
        format = format,
    )

fun List<LoyaltyCard>.toPresentable(): List<PresentableLoyaltyCard> = map { it.toPresentable() }

/**
 * What a cashier would type in when scanning fails: control characters (e.g. the GS1 separator) are hidden
 * and long numbers are grouped in fours. Never use it as barcode content.
 */
internal fun String.toDisplayCode(): String {
    val visible = filterNot { it.isISOControl() }.trim()
    val isLongNumber = visible.length > DIGIT_GROUP_SIZE * 2 && visible.all { it in '0'..'9' }
    return if (isLongNumber) visible.chunked(DIGIT_GROUP_SIZE).joinToString(" ") else visible
}

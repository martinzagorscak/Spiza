package com.mzagorscak.spiza.ui.model

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test

class PresentableLoyaltyCardTest {
    @Test
    fun toPresentable_keepsEveryField_andTurnsTheArgbIntoAColor() {
        val card = LoyaltyCard(
            id = 7L,
            name = "Shop",
            colorArgb = 0xFF336699.toInt(),
            codeText = "4006381333931",
            format = LoyaltyCardFormat.Ean13,
            isShortcut = true,
        )

        val presentable = card.toPresentable()

        assertThat(presentable).isEqualTo(
            PresentableLoyaltyCard(
                id = 7L,
                name = "Shop",
                color = Color(0xFF336699),
                codeText = "4006381333931",
                format = LoyaltyCardFormat.Ean13,
                isShortcut = true,
            ),
        )
    }

    @Test
    fun toDisplayCode_groupsLongNumbersInFours() {
        assertThat("4006381333931".toDisplayCode()).isEqualTo("4006 3813 3393 1")
    }

    @Test
    fun toDisplayCode_keepsShortNumbersAndText_asTheyAre() {
        assertThat("96385074".toDisplayCode()).isEqualTo("96385074")
        assertThat("CARD-0042".toDisplayCode()).isEqualTo("CARD-0042")
    }

    @Test
    fun toDisplayCode_hidesControlCharacters() {
        assertThat("\u001D0104012345\u001D1726\r\n".toDisplayCode()).isEqualTo("0104 0123 4517 26")
        assertThat("AB\u001D12".toDisplayCode()).isEqualTo("AB12")
    }
}

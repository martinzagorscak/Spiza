package com.mzagorscak.spiza.ui.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PresentableLoyaltyCardTest {
    @Test
    fun toDisplayCode_groupsLongNumbersInFours() {
        assertEquals("4006 3813 3393 1", "4006381333931".toDisplayCode())
    }

    @Test
    fun toDisplayCode_keepsShortNumbersAndText_asTheyAre() {
        assertEquals("96385074", "96385074".toDisplayCode())
        assertEquals("CARD-0042", "CARD-0042".toDisplayCode())
    }

    @Test
    fun toDisplayCode_hidesControlCharacters() {
        assertEquals("0104 0123 4517 26", "\u001D0104012345\u001D1726\r\n".toDisplayCode())
        assertEquals("AB12", "AB\u001D12".toDisplayCode())
    }
}

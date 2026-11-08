package com.mzagorscak.spiza.data.local

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test

class SpizaDatabaseMigrationsTest {
    @Test
    fun legacyFormats_neverRewriteAFormatThatStillExists() {
        val currentNames = LoyaltyCardFormat.entries.map { it.name }.toSet()
        assertThat(LEGACY_LOYALTY_CARD_FORMATS.keys.filter { it in currentNames }).isEmpty()
    }

    @Test
    fun legacyQrCode_isStillAValidFormat() {
        assertThat(LoyaltyCardFormat.valueOf("QrCode")).isEqualTo(LoyaltyCardFormat.QrCode)
    }
}

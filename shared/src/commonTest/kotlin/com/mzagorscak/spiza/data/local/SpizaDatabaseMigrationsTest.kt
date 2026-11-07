package com.mzagorscak.spiza.data.local

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpizaDatabaseMigrationsTest {
    @Test
    fun legacyFormats_neverRewriteAFormatThatStillExists() {
        val currentNames = LoyaltyCardFormat.entries.map { it.name }.toSet()
        assertTrue(LEGACY_LOYALTY_CARD_FORMATS.keys.none { it in currentNames })
    }

    @Test
    fun legacyQrCode_isStillAValidFormat() {
        assertEquals(LoyaltyCardFormat.QrCode, LoyaltyCardFormat.valueOf("QrCode"))
    }

    @Test
    fun legacyFormatsSql_updatesOnlyTheLegacyRows() {
        assertEquals(
            listOf("UPDATE loyalty_cards SET format = 'Code128' WHERE format = 'Barcode'"),
            legacyLoyaltyCardFormatsSql(),
        )
    }
}

package com.mzagorscak.spiza.data.suggestion.local

import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpizaDatabaseMigrationsTest {
    @Test
    fun migration1To2_coversTheVersions() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }

    @Test
    fun legacyFormats_mapBarcodeToCode128_likeTheOldRenderer() {
        assertEquals(LoyaltyCardFormat.Code128, LEGACY_LOYALTY_CARD_FORMATS["Barcode"])
    }

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

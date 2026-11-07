package com.mzagorscak.spiza.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

/**
 * Formats stored before the custom barcode renderer. The generic `Barcode` was always drawn as Code 128,
 * so mapping it there keeps existing cards looking (and scanning) exactly as before.
 * Room reads enums by name and throws on an unknown one, so every legacy name must be mapped.
 */
internal val LEGACY_LOYALTY_CARD_FORMATS: Map<String, LoyaltyCardFormat> =
    mapOf("Barcode" to LoyaltyCardFormat.Code128)

internal fun legacyLoyaltyCardFormatsSql(): List<String> =
    LEGACY_LOYALTY_CARD_FORMATS.map { (legacy, format) ->
        "UPDATE loyalty_cards SET format = '${format.name}' WHERE format = '$legacy'"
    }

/** 1 → 2: [LoyaltyCardFormat] went from Barcode/QrCode to concrete symbologies; the table shape is unchanged. */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            legacyLoyaltyCardFormatsSql().forEach { connection.execSQL(it) }
        }
    }

/** 2 → 3: loyalty cards can be app shortcuts; existing cards are not. */
internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE loyalty_cards ADD COLUMN isShortcut INTEGER NOT NULL DEFAULT 0")
        }
    }

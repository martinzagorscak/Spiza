package com.mzagorscak.spiza.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import assertk.assertThat
import assertk.assertions.containsExactly
import com.mzagorscak.spiza.data.loyaltyCard.model.LoyaltyCardEntity
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val DATABASE_NAME = "spiza_migration_test.db"
private const val COLOR_ARGB = 0xFF336699.toInt()

/**
 * Opens the database at an old version from the committed schemas (`shared/schemas`), stores rows the way that app
 * version did, migrates and checks both the data and that the schema matches what Room expects for the new version.
 */
class SpizaDatabaseMigrationTest {
    private val databasePath = migrationTestDatabasePath(DATABASE_NAME)
    private lateinit var helper: MigrationTestHelper

    @BeforeTest
    fun setUp() {
        deleteDatabaseFiles(databasePath)
        helper = spizaMigrationTestHelper(databasePath)
    }

    @AfterTest
    fun tearDown() = deleteDatabaseFiles(databasePath)

    // Version 1 stored only the generic `Barcode` and `QrCode` formats.
    private fun createVersion1WithCards() =
        helper.createDatabase(1).use { connection ->
            connection.insertCard(id = 1L, name = "Shop", codeText = "4006381333931", format = "Barcode", createdAt = 100L)
            connection.insertCard(id = 2L, name = "Pharmacy", codeText = "https://example.com/card", format = "QrCode", createdAt = 200L)
            connection.execSQL(
                "INSERT INTO item_suggestions (id, name, normalizedName, useCount, lastUsedAt) VALUES (1, 'Milk', 'milk', 3, 300)",
            )
        }

    @Test
    fun migration1To2_rewritesLegacyBarcodeToCode128_andKeepsEveryOtherValue() {
        createVersion1WithCards()

        val migrated = helper.runMigrationsAndValidate(version = 2, migrations = listOf(MIGRATION_1_2))

        migrated.use { connection ->
            assertThat(connection.cardRows()).containsExactly(
                CardRow(id = 1L, name = "Shop", codeText = "4006381333931", format = "Code128", createdAt = 100L),
                CardRow(id = 2L, name = "Pharmacy", codeText = "https://example.com/card", format = "QrCode", createdAt = 200L),
            )
            assertThat(connection.suggestionNames()).containsExactly("Milk")
        }
    }

    @Test
    fun migration2To3_addsIsShortcutAsFalse_toExistingCards() {
        helper.createDatabase(2).use { connection ->
            connection.insertCard(id = 1L, name = "Shop", codeText = "4006381333931", format = "Ean13", createdAt = 100L)
        }

        val migrated = helper.runMigrationsAndValidate(version = 3, migrations = listOf(MIGRATION_2_3))

        migrated.use { connection ->
            assertThat(connection.cardRows()).containsExactly(
                CardRow(id = 1L, name = "Shop", codeText = "4006381333931", format = "Ean13", createdAt = 100L, isShortcut = false),
            )
        }
    }

    // Opens the migrated file exactly like the app does (getSpizaDatabase), so a migration missing from the production
    // builder or an identity-hash mismatch fails here, and the stored values must still map to entities.
    @Test
    fun productionDatabase_opensAVersion1Database_withEveryCardAndSuggestionReadable() =
        runTest {
            createVersion1WithCards()

            val database = getSpizaDatabase(spizaDatabaseBuilder(databasePath))

            try {
                assertThat(database.loyaltyCardDao().getAll().first()).containsExactly(
                    LoyaltyCardEntity(
                        id = 2L,
                        name = "Pharmacy",
                        colorArgb = COLOR_ARGB,
                        codeText = "https://example.com/card",
                        format = LoyaltyCardFormat.QrCode,
                        createdAt = 200L,
                        isShortcut = false,
                    ),
                    LoyaltyCardEntity(
                        id = 1L,
                        name = "Shop",
                        colorArgb = COLOR_ARGB,
                        codeText = "4006381333931",
                        format = LoyaltyCardFormat.Code128,
                        createdAt = 100L,
                        isShortcut = false,
                    ),
                )
                assertThat(database.suggestionDao().getAllLastUsedDesc().first()).containsExactly(
                    SuggestionEntity(id = 1L, name = "Milk", normalizedName = "milk", useCount = 3, lastUsedAt = 300L),
                )
            } finally {
                database.close()
            }
        }

    private data class CardRow(
        val id: Long,
        val name: String,
        val codeText: String,
        val format: String,
        val createdAt: Long,
        val colorArgb: Int = COLOR_ARGB,
        // null before version 3, which added the column.
        val isShortcut: Boolean? = null,
    )

    private fun SQLiteConnection.insertCard(
        id: Long,
        name: String,
        codeText: String,
        format: String,
        createdAt: Long,
    ) = prepare(
        "INSERT INTO loyalty_cards (id, name, colorArgb, codeText, format, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
    ).use { statement ->
        statement.bindLong(1, id)
        statement.bindText(2, name)
        statement.bindLong(3, COLOR_ARGB.toLong())
        statement.bindText(4, codeText)
        statement.bindText(5, format)
        statement.bindLong(6, createdAt)
        statement.step()
    }

    private fun SQLiteConnection.cardRows(): List<CardRow> =
        prepare("SELECT * FROM loyalty_cards ORDER BY id").use { statement ->
            val columns = List(statement.getColumnCount()) { statement.getColumnName(it) }
            val shortcutIndex = columns.indexOf("isShortcut")
            buildList {
                while (statement.step()) {
                    add(
                        CardRow(
                            id = statement.getLong(columns.indexOf("id")),
                            name = statement.getText(columns.indexOf("name")),
                            codeText = statement.getText(columns.indexOf("codeText")),
                            format = statement.getText(columns.indexOf("format")),
                            createdAt = statement.getLong(columns.indexOf("createdAt")),
                            colorArgb = statement.getInt(columns.indexOf("colorArgb")),
                            isShortcut = if (shortcutIndex >= 0) statement.getBoolean(shortcutIndex) else null,
                        ),
                    )
                }
            }
        }

    private fun SQLiteConnection.suggestionNames(): List<String> =
        prepare("SELECT name FROM item_suggestions ORDER BY id").use { statement ->
            buildList { while (statement.step()) add(statement.getText(0)) }
        }
}

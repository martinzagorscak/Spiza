package com.mzagorscak.spiza.data.local

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import com.mzagorscak.spiza.data.loyaltyCard.model.LoyaltyCardEntity
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomLocalDataCleanerTest {
    private lateinit var database: SpizaDatabase
    private lateinit var cleaner: RoomLocalDataCleaner

    @BeforeTest
    fun setUp() {
        database = inMemorySpizaDatabase()
        cleaner = RoomLocalDataCleaner(database = database)
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun clearAll_deletesEveryLoyaltyCardAndSuggestion_andNotifiesObservers() =
        runTest {
            database.loyaltyCardDao().insert(
                LoyaltyCardEntity(name = "Shop", colorArgb = 0, codeText = "4006381333931", format = LoyaltyCardFormat.Ean13),
            )
            database.suggestionDao().upsert(name = "Milk", normalizedName = "milk", lastUsedAt = 1L)

            // Observers (e.g. the app shortcut sync) must see the wipe, so the deleted cards' shortcuts are removed.
            database.loyaltyCardDao().getAll().test {
                assertThat(awaitItem()).hasSize(1)

                cleaner.clearAll()

                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(database.suggestionDao().getAll(SuggestionSortingType.LAST_USED_DESC).first()).isEmpty()
        }
}

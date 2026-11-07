package com.mzagorscak.spiza.data.shortcuts

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.shortcuts.AppShortcutsPublisher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppShortcutsRepositoryImplTest {
    private val publisher = RecordingPublisher()
    private val logger = FakeCrashLogger()
    private val repository = AppShortcutsRepositoryImpl(publisher = publisher, crashLogger = logger)

    @Test
    fun sync_publishesOnlyShortcutCards_withEveryExistingCardId() =
        runTest {
            val cards = listOf(card(id = 1L, isShortcut = true), card(id = 2L), card(id = 3L, isShortcut = true))

            repository.syncLoyaltyCardShortcuts(allCards = flowOf(cards), language = flowOf(AppLanguage.English))

            assertEquals(listOf(Published(cardIds = listOf(1L, 3L), existingCardIds = setOf(1L, 2L, 3L))), publisher.published)
        }

    @Test
    fun sync_skipsSnapshotsThatDoNotChangeTheShortcuts() =
        runTest {
            val first = listOf(card(id = 1L, isShortcut = true), card(id = 2L))
            val renamedNonShortcut = listOf(card(id = 1L, isShortcut = true), card(id = 2L, name = "Renamed"))

            repository.syncLoyaltyCardShortcuts(
                allCards = flowOf(first, renamedNonShortcut),
                language = flowOf(AppLanguage.English),
            )

            assertEquals(1, publisher.published.size)
        }

    @Test
    fun sync_republishesInTheNewLanguage_whenTheAppLanguageChanges() =
        runTest {
            repository.syncLoyaltyCardShortcuts(
                allCards = flowOf(listOf(card(id = 1L, isShortcut = true))),
                language = flowOf(AppLanguage.English, AppLanguage.Croatian),
            )

            assertEquals(listOf(AppLanguage.English, AppLanguage.Croatian), publisher.languages)
        }

    @Test
    fun sync_recordsPublishFailures_andKeepsSyncing() =
        runTest {
            publisher.failure = IllegalStateException("launcher unavailable")

            repository.syncLoyaltyCardShortcuts(
                allCards = flowOf(listOf(card(id = 1L, isShortcut = true)), listOf(card(id = 1L))),
                language = flowOf(AppLanguage.English),
            )

            assertEquals(2, logger.exceptions.size)
            assertTrue(publisher.published.isEmpty())
        }

    private data class Published(
        val cardIds: List<Long>,
        val existingCardIds: Set<Long>,
    )

    private class RecordingPublisher : AppShortcutsPublisher {
        val published = mutableListOf<Published>()
        val languages = mutableListOf<AppLanguage>()
        var failure: Exception? = null

        override val isSupported: Boolean = true

        override suspend fun publishLoyaltyCards(
            cards: List<LoyaltyCard>,
            existingCardIds: Set<Long>,
            language: AppLanguage,
        ) {
            failure?.let { throw it }
            published += Published(cardIds = cards.map { it.id }, existingCardIds = existingCardIds)
            languages += language
        }
    }

    private fun card(
        id: Long,
        isShortcut: Boolean = false,
        name: String = "Card",
    ) = LoyaltyCard(
        id = id,
        name = name,
        colorArgb = 0,
        codeText = "123",
        format = LoyaltyCardFormat.Code128,
        isShortcut = isShortcut,
    )
}

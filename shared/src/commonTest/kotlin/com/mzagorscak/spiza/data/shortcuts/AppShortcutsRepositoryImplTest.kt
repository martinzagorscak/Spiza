package com.mzagorscak.spiza.data.shortcuts

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.fakes.FakeAppShortcutsPublisher
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppShortcutsRepositoryImplTest {
    private val publisher = FakeAppShortcutsPublisher()
    private val logger = FakeCrashLogger()
    private val repository = AppShortcutsRepositoryImpl(publisher = publisher, crashLogger = logger)

    @Test
    fun sync_publishesOnlyShortcutCards_withEveryExistingCardId() =
        runTest {
            val cards = listOf(card(id = 1L, isShortcut = true), card(id = 2L), card(id = 3L, isShortcut = true))

            repository.syncLoyaltyCardShortcuts(allCards = flowOf(cards), language = flowOf(AppLanguage.English))

            assertThat(publisher.published).containsExactly(
                FakeAppShortcutsPublisher.Published(
                    cardIds = listOf(1L, 3L),
                    existingCardIds = setOf(1L, 2L, 3L),
                    language = AppLanguage.English,
                ),
            )
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

            assertThat(publisher.published).hasSize(1)
        }

    @Test
    fun sync_republishesInTheNewLanguage_whenTheAppLanguageChanges() =
        runTest {
            repository.syncLoyaltyCardShortcuts(
                allCards = flowOf(listOf(card(id = 1L, isShortcut = true))),
                language = flowOf(AppLanguage.English, AppLanguage.Croatian),
            )

            assertThat(publisher.published.map { it.language }).containsExactly(AppLanguage.English, AppLanguage.Croatian)
        }

    // region Error handling

    @Test
    fun sync_publishesNothing_whenShortcutsAreNotSupported() =
        runTest {
            val unsupportedPublisher = FakeAppShortcutsPublisher(isSupported = false)
            val unsupported = AppShortcutsRepositoryImpl(publisher = unsupportedPublisher, crashLogger = logger)

            unsupported.syncLoyaltyCardShortcuts(
                allCards = flowOf(listOf(card(id = 1L, isShortcut = true))),
                language = flowOf(AppLanguage.English),
            )

            assertThat(unsupportedPublisher.published).isEmpty()
        }

    @Test
    fun sync_recordsAFailingCardsStream_insteadOfThrowing() =
        runTest {
            val failure = IllegalStateException("db corrupted")

            repository.syncLoyaltyCardShortcuts(
                allCards = flow { throw failure },
                language = flowOf(AppLanguage.English),
            )

            val recorded = logger.exceptions.single()
            assertThat(recorded.cause?.message).isEqualTo(failure.message)
            assertThat(publisher.published).isEmpty()
        }

    @Test
    fun sync_recordsPublishFailures_andKeepsSyncing() =
        runTest {
            val failure = IllegalStateException("launcher unavailable")
            publisher.publishFailure = failure

            repository.syncLoyaltyCardShortcuts(
                allCards = flowOf(listOf(card(id = 1L, isShortcut = true)), listOf(card(id = 1L))),
                language = flowOf(AppLanguage.English),
            )

            // The second snapshot is still published after the first one failed.
            assertThat(publisher.published.map { it.cardIds }).containsExactly(listOf(1L), emptyList<Long>())
            assertThat(logger.exceptions.map { it.cause?.message }).containsExactly(failure.message, failure.message)
        }

    // endregion

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

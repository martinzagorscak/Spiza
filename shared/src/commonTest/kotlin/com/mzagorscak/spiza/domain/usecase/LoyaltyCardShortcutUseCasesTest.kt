package com.mzagorscak.spiza.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.ShortcutToggleResult
import com.mzagorscak.spiza.fakes.FakeLoyaltyCardRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoyaltyCardShortcutUseCasesTest {
    private val limit = ConstraintsManagerImpl.maxLoyaltyCardShortcuts
    private val repository = FakeLoyaltyCardRepository()
    private val toggle = ToggleLoyaltyCardShortcutUseCaseImpl(repository = repository, constraintsManager = ConstraintsManagerImpl)

    @Test
    fun toggle_pinsACardThatIsNotAShortcut() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))

            val result = toggle(1L)

            assertThat(result).isEqualTo(ShortcutToggleResult.Enabled)
            assertThat(repository.shortcutIds()).containsExactly(1L)
            // The limit is enforced by the repository's atomic write, so the use case must hand it the business limit.
            assertThat(repository.addedShortcuts).containsExactly(1L to limit)
        }

    @Test
    fun toggle_unpinsACardThatIsAShortcut() =
        runTest {
            repository.cards.value = listOf(card(id = 1L, isShortcut = true))

            assertThat(toggle(1L)).isEqualTo(ShortcutToggleResult.Disabled)
            assertThat(repository.shortcutIds()).isEmpty()
        }

    // region Error handling

    @Test
    fun toggle_reportsLimitReached_whenTheRepositoryRefusesToPinAnExistingCard() =
        runTest {
            val pinned = (1L..limit.toLong()).toList()
            repository.cards.value = pinned.map { card(id = it, isShortcut = true) } + card(id = 100L)

            val result = toggle(100L)

            assertThat(result).isEqualTo(ShortcutToggleResult.LimitReached(limit = limit))
        }

    @Test
    fun toggle_stillUnpinsWhenTheLimitIsReached() =
        runTest {
            val pinned = (1L..limit.toLong()).toList()
            repository.cards.value = pinned.map { card(id = it, isShortcut = true) }

            val result = toggle(1L)

            assertThat(result).isEqualTo(ShortcutToggleResult.Disabled)
            assertThat(repository.shortcutIds()).isEqualTo(pinned - 1L)
        }

    @Test
    fun toggle_reportsAMissingCard() =
        runTest {
            assertThat(toggle(42L)).isEqualTo(ShortcutToggleResult.CardNotFound)
        }

    @Test
    fun toggle_reportsAMissingCard_whenItIsDeletedBeforeItCouldBePinned() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            repository.beforeAddShortcut = { repository.cards.value = emptyList() }

            val result = toggle(1L)

            assertThat(result).isEqualTo(ShortcutToggleResult.CardNotFound)
            assertThat(repository.addedShortcuts).containsExactly(1L to limit)
        }

    // endregion

    private fun FakeLoyaltyCardRepository.shortcutIds(): List<Long> = cards.value.filter { it.isShortcut }.map { it.id }

    private fun card(
        id: Long,
        isShortcut: Boolean = false,
    ) = LoyaltyCard(
        id = id,
        name = "Card",
        colorArgb = 0,
        codeText = "123",
        format = LoyaltyCardFormat.Code128,
        isShortcut = isShortcut,
    )
}

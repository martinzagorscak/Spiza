package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.ShortcutToggleResult
import com.mzagorscak.spiza.fakes.FakeLoyaltyCardRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoyaltyCardShortcutUseCasesTest {
    private val repository = FakeLoyaltyCardRepository()
    private val toggle = ToggleLoyaltyCardShortcutUseCaseImpl(repository = repository, constraintsManager = ConstraintsManagerImpl)

    @Test
    fun toggle_pinsACardThatIsNotAShortcut() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))

            assertEquals(ShortcutToggleResult.Enabled, toggle(1L))
            assertEquals(listOf(1L), repository.shortcutIds())
        }

    @Test
    fun toggle_unpinsACardThatIsAShortcut() =
        runTest {
            repository.cards.value = listOf(card(id = 1L, isShortcut = true))

            assertEquals(ShortcutToggleResult.Disabled, toggle(1L))
            assertEquals(emptyList(), repository.shortcutIds())
        }

    @Test
    fun toggle_blocksPinningBeyondTheLimit_andChangesNothing() =
        runTest {
            repository.cards.value = (1L..3L).map { card(id = it, isShortcut = true) } + card(id = 4L)

            assertEquals(ShortcutToggleResult.LimitReached(limit = 3), toggle(4L))
            assertEquals(listOf(1L, 2L, 3L), repository.shortcutIds())
        }

    @Test
    fun toggle_stillUnpinsWhenTheLimitIsReached() =
        runTest {
            repository.cards.value = (1L..3L).map { card(id = it, isShortcut = true) }

            assertEquals(ShortcutToggleResult.Disabled, toggle(2L))
            assertEquals(listOf(1L, 3L), repository.shortcutIds())
        }

    @Test
    fun toggle_reportsAMissingCard() =
        runTest {
            assertEquals(ShortcutToggleResult.CardNotFound, toggle(42L))
        }

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

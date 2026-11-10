package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import app.cash.turbine.Event
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.usecase.CanUseAppShortcutsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetLoyaltyCardShortcutLimitUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ToggleLoyaltyCardShortcutUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeLoyaltyCardRepository
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoyaltyCardDetailsViewModelImplTest {
    private val repository = FakeLoyaltyCardRepository()
    private val crashLogger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        cardId: Long,
        areShortcutsSupported: Boolean = true,
    ) = LoyaltyCardDetailsViewModelImpl(
        cardId = cardId,
        observeLoyaltyCardByIdUseCase = ObserveLoyaltyCardByIdUseCaseImpl(repository = repository),
        deleteLoyaltyCardUseCase = DeleteLoyaltyCardUseCaseImpl(repository = repository),
        canUseAppShortcutsUseCase = object : CanUseAppShortcutsUseCase {
            override fun invoke() = areShortcutsSupported
        },
        getLoyaltyCardShortcutLimitUseCase = GetLoyaltyCardShortcutLimitUseCaseImpl(constraintsManager = ConstraintsManagerImpl),
        toggleLoyaltyCardShortcutUseCase = ToggleLoyaltyCardShortcutUseCaseImpl(
            repository = repository,
            constraintsManager = ConstraintsManagerImpl,
        ),
        crashLogger = crashLogger,
    )

    /** A loaded state with the shortcut support and limit the [viewModel] builder provides by default. */
    private fun loadedState() =
        LoyaltyCardDetailsState(
            isLoading = false,
            areShortcutsSupported = true,
            shortcutLimit = ConstraintsManagerImpl.maxLoyaltyCardShortcuts,
        )

    @Test
    fun state_showsTheRequestedCard_onceLoaded() =
        runTest {
            val requested = card(id = 2L, name = "Shop")
            repository.cards.value = listOf(card(id = 1L), requested)
            val viewModel = viewModel(cardId = 2L)

            viewModel.state().test {
                assertThat(expectMostRecentItem())
                    .isEqualTo(loadedState().copy(card = requested.toPresentable(), isCardMissing = false))
            }
        }

    @Test
    fun state_reportsShortcutsUnsupported_whenThePlatformHasNoShortcuts() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel(cardId = 1L, areShortcutsSupported = false)

            viewModel.state().test {
                assertThat(expectMostRecentItem().areShortcutsSupported).isFalse()
            }
        }

    @Test
    fun showDeleteConfirmationDialog_showsTheDialog_withoutDeleting() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.showDeleteConfirmationDialog()

                assertThat(expectMostRecentItem().dialogState).isEqualTo(LoyaltyCardDetailsState.DialogState.DeleteConfirmation)
            }
            assertThat(repository.deletedIds).isEmpty()
        }

    @Test
    fun dismissDialog_hidesTheDialog_withoutDeleting() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()
                viewModel.showDeleteConfirmationDialog()

                viewModel.dismissDialog()

                assertThat(expectMostRecentItem().dialogState).isEqualTo(LoyaltyCardDetailsState.DialogState.Hidden)
            }
            assertThat(repository.deletedIds).isEmpty()
        }

    @Test
    fun deleteCard_hidesTheDialog_whenConfirmed() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            repository.neverCompleteWrites = true
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()
                viewModel.showDeleteConfirmationDialog()

                viewModel.deleteCard()

                assertThat(expectMostRecentItem().dialogState).isEqualTo(LoyaltyCardDetailsState.DialogState.Hidden)
            }
            assertThat(repository.deletedIds).containsExactly(1L)
        }

    @Test
    fun deleteCard_deletesThisCardAndReportsItDeleted_withoutReportingItMissing() =
        runTest {
            repository.cards.value = listOf(card(id = 1L), card(id = 2L))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.deleteCard()

                val states = cancelAndConsumeRemainingEvents().filterIsInstance<Event.Item<LoyaltyCardDetailsState>>().map { it.value }
                assertThat(states).isNotEmpty()
                // The screen closes itself once deleted, so the card disappearing must never show the "missing" state.
                assertThat(states.any { it.isCardMissing }).isFalse()
                assertThat(states.last())
                    .isEqualTo(loadedState().copy(card = null, isCardMissing = false, isDeleted = true))
            }
            assertThat(repository.cards.value.map { it.id }).containsExactly(2L)
        }

    @Test
    fun deleteCard_deletesOnce_whileADeleteIsInProgress() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            repository.neverCompleteWrites = true
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.deleteCard()
                viewModel.deleteCard()

                val state = expectMostRecentItem()
                assertThat(state.isDeleting).isTrue()
                assertThat(state.isDeleted).isFalse()
                assertThat(state.card?.id).isEqualTo(1L)
            }
            assertThat(repository.deletedIds).containsExactly(1L)
        }

    @Test
    fun onToggleShortcut_pinsTheCardAndShowsTheAddedMessage_whenNotPinned() =
        runTest {
            repository.cards.value = listOf(card(id = 1L, name = "Shop"))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut()

                val state = expectMostRecentItem()
                assertThat(state.card?.isShortcut).isEqualTo(true)
                assertThat(state.shortcutMessage)
                    .isEqualTo(LoyaltyCardDetailsState.ShortcutMessage.Added(cardName = "Shop"))
            }
        }

    @Test
    fun onToggleShortcut_unpinsTheCardWithoutAMessage_whenPinned() =
        runTest {
            repository.cards.value = listOf(card(id = 1L, isShortcut = true))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut()

                val state = expectMostRecentItem()
                assertThat(state.card?.isShortcut).isEqualTo(false)
                assertThat(state.shortcutMessage).isEqualTo(LoyaltyCardDetailsState.ShortcutMessage.None)
            }
            assertThat(repository.removedShortcutIds).containsExactly(1L)
        }

    @Test
    fun onShortcutMessageShown_clearsTheMessage() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()
                viewModel.onToggleShortcut()

                viewModel.onShortcutMessageShown()

                assertThat(expectMostRecentItem().shortcutMessage).isEqualTo(LoyaltyCardDetailsState.ShortcutMessage.None)
            }
        }

    // region Error handling

    @Test
    fun onToggleShortcut_keepsTheCardUnpinnedAndShowsTheLimit_whenTheLimitIsReached() =
        runTest {
            val limit = ConstraintsManagerImpl.maxLoyaltyCardShortcuts
            val pinned = (1L..limit.toLong()).map { card(id = it, isShortcut = true) }
            val target = card(id = 100L)
            repository.cards.value = pinned + target
            val viewModel = viewModel(cardId = target.id)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut()

                val state = expectMostRecentItem()
                assertThat(state.card?.isShortcut).isEqualTo(false)
                assertThat(state.shortcutMessage)
                    .isEqualTo(LoyaltyCardDetailsState.ShortcutMessage.LimitReached(limit = limit))
            }
            assertThat(crashLogger.exceptions).isEmpty()
        }

    @Test
    fun onToggleShortcut_showsTheGenericErrorAndRecordsIt_whenTheToggleFails() =
        runTest {
            val failure = IllegalStateException("db closed")
            repository.cards.value = listOf(card(id = 1L))
            repository.shortcutFailure = failure
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut()

                val state = expectMostRecentItem()
                assertThat(state.card?.isShortcut).isEqualTo(false)
                assertThat(state.shortcutMessage).isEqualTo(LoyaltyCardDetailsState.ShortcutMessage.GenericError)
            }
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun state_isCardMissing_whenTheCardDoesNotExist() =
        runTest {
            val viewModel = viewModel(cardId = 42L)

            viewModel.state().test {
                assertThat(expectMostRecentItem().isCardMissing).isTrue()
            }
        }

    @Test
    fun state_stopsLoadingAndRecordsTheFailure_whenTheCardStreamFails() =
        runTest {
            val failure = IllegalStateException("db corrupted")
            repository.observeFailure = failure
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                assertThat(expectMostRecentItem())
                    .isEqualTo(loadedState().copy(card = null, isCardMissing = false))
            }
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun deleteCard_showsTheErrorAndKeepsTheCard_whenTheDeleteFails() =
        runTest {
            val failure = IllegalStateException("db closed")
            val stored = card(id = 1L)
            repository.cards.value = listOf(stored)
            repository.writeFailure = failure
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.deleteCard()

                assertThat(expectMostRecentItem())
                    .isEqualTo(loadedState().copy(card = stored.toPresentable(), hasError = true))
            }
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun onErrorShown_clearsTheError_afterAFailedDelete() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            repository.writeFailure = IllegalStateException("db closed")
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()
                viewModel.deleteCard()

                viewModel.onErrorShown()

                assertThat(expectMostRecentItem().hasError).isFalse()
            }
        }

    @Test
    fun state_isCardMissing_whenTheCardDisappearsAfterAFailedDelete() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            repository.writeFailure = IllegalStateException("db closed")
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()
                viewModel.deleteCard()

                repository.cards.value = emptyList()

                assertThat(expectMostRecentItem().isCardMissing).isTrue()
            }
        }

    // endregion

    private fun card(
        id: Long,
        name: String = "Card",
        isShortcut: Boolean = false,
    ) = LoyaltyCard(
        id = id,
        name = name,
        colorArgb = 0,
        codeText = "123",
        format = LoyaltyCardFormat.Code128,
        isShortcut = isShortcut,
    )
}

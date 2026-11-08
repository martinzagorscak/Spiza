package com.mzagorscak.spiza.ui.screens.loyaltyCards

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.usecase.CanUseAppShortcutsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardsUseCaseImpl
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
class LoyaltyCardsViewModelImplTest {
    private val repository = FakeLoyaltyCardRepository()
    private val crashLogger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(areShortcutsSupported: Boolean = true) =
        LoyaltyCardsViewModelImpl(
            observeLoyaltyCardsUseCase = ObserveLoyaltyCardsUseCaseImpl(repository = repository),
            canUseAppShortcutsUseCase = object : CanUseAppShortcutsUseCase {
                override fun invoke() = areShortcutsSupported
            },
            toggleLoyaltyCardShortcutUseCase = ToggleLoyaltyCardShortcutUseCaseImpl(
                repository = repository,
                constraintsManager = ConstraintsManagerImpl,
            ),
            crashLogger = crashLogger,
        )

    @Test
    fun state_showsTheStoredCardsAndShortcutSupport_onceLoaded() =
        runTest {
            val cards = listOf(card(id = 2L, name = "Shop", isShortcut = true), card(id = 1L))
            repository.cards.value = cards
            val viewModel = viewModel(areShortcutsSupported = false)

            viewModel.state().test {
                assertThat(expectMostRecentItem()).isEqualTo(
                    LoyaltyCardsState(
                        cards = cards.toPresentable(),
                        isLoading = false,
                        areShortcutsSupported = false,
                        message = LoyaltyCardsState.Message.None,
                    ),
                )
            }
        }

    @Test
    fun state_followsCardsAddedLater() =
        runTest {
            val viewModel = viewModel()

            viewModel.state().test {
                assertThat(expectMostRecentItem().cards).isEmpty()

                repository.cards.value = listOf(card(id = 1L, name = "Shop"))

                assertThat(awaitItem().cards.map { it.name }).containsExactly("Shop")
            }
        }

    @Test
    fun onToggleShortcut_pinsTheCard_andShowsAddedMessageWithItsName() =
        runTest {
            repository.cards.value = listOf(card(id = 1L, name = "Shop"))
            val viewModel = viewModel()

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut(cardId = 1L)

                val state = expectMostRecentItem()
                assertThat(state.message).isEqualTo(LoyaltyCardsState.Message.ShortcutAdded(cardName = "Shop"))
                assertThat(state.cards.single().isShortcut).isTrue()
            }
        }

    @Test
    fun onToggleShortcut_unpinsTheCard_withoutAMessage_whenItIsAShortcut() =
        runTest {
            repository.cards.value = listOf(card(id = 1L, isShortcut = true))
            val viewModel = viewModel()

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut(cardId = 1L)

                val state = expectMostRecentItem()
                assertThat(state.message).isEqualTo(LoyaltyCardsState.Message.None)
                assertThat(state.cards.single().isShortcut).isFalse()
            }
        }

    @Test
    fun onMessageShown_clearsTheMessage() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel()

            viewModel.state().test {
                viewModel.onToggleShortcut(cardId = 1L)
                assertThat(expectMostRecentItem().message).isEqualTo(LoyaltyCardsState.Message.ShortcutAdded(cardName = "Card"))

                viewModel.onMessageShown()

                assertThat(awaitItem().message).isEqualTo(LoyaltyCardsState.Message.None)
            }
        }

    // region Error handling

    @Test
    fun onToggleShortcut_showsLimitMessageWithTheLimit_whenLimitIsReached() =
        runTest {
            val limit = ConstraintsManagerImpl.maxLoyaltyCardShortcuts
            val notPinnedId = limit + 1L
            repository.cards.value = (1L..limit).map { card(id = it, isShortcut = true) } + card(id = notPinnedId)
            val viewModel = viewModel()

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut(cardId = notPinnedId)

                val state = awaitItem()
                assertThat(state.message).isEqualTo(LoyaltyCardsState.Message.ShortcutLimitReached(limit = limit))
                assertThat(state.cards.single { it.id == notPinnedId }.isShortcut).isFalse()
            }
            assertThat(crashLogger.exceptions).isEmpty()
        }

    @Test
    fun onToggleShortcut_showsNoMessage_whenTheCardNoLongerExists() =
        runTest {
            val viewModel = viewModel()

            viewModel.state().test {
                assertThat(expectMostRecentItem().message).isEqualTo(LoyaltyCardsState.Message.None)

                viewModel.onToggleShortcut(cardId = 42L)

                expectNoEvents()
            }
        }

    @Test
    fun onToggleShortcut_showsGenericErrorAndRecordsIt_whenTheWriteFails() =
        runTest {
            val failure = IllegalStateException("db closed")
            repository.cards.value = listOf(card(id = 1L))
            repository.shortcutFailure = failure
            val viewModel = viewModel()

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.onToggleShortcut(cardId = 1L)

                val state = awaitItem()
                assertThat(state.message).isEqualTo(LoyaltyCardsState.Message.GenericError)
                assertThat(state.cards.single().isShortcut).isFalse()
            }
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun state_showsNoCardsAndRecordsTheFailure_whenTheCardsStreamFails() =
        runTest {
            val failure = IllegalStateException("db corrupted")
            repository.observeFailure = failure
            val viewModel = viewModel()

            viewModel.state().test {
                val state = expectMostRecentItem()
                assertThat(state.cards).isEmpty()
                assertThat(state.isLoading).isFalse()
            }
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
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

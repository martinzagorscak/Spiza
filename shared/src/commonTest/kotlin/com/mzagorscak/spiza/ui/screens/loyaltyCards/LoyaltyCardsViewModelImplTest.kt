package com.mzagorscak.spiza.ui.screens.loyaltyCards

import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.usecase.CanUseAppShortcutsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ToggleLoyaltyCardShortcutUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeLoyaltyCardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoyaltyCardsViewModelImplTest {
    private val repository = FakeLoyaltyCardRepository()
    private val crashLogger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        LoyaltyCardsViewModelImpl(
            observeLoyaltyCardsUseCase = ObserveLoyaltyCardsUseCaseImpl(repository = repository),
            canUseAppShortcutsUseCase = object : CanUseAppShortcutsUseCase {
                override fun invoke() = true
            },
            toggleLoyaltyCardShortcutUseCase = ToggleLoyaltyCardShortcutUseCaseImpl(
                repository = repository,
                constraintsManager = ConstraintsManagerImpl,
            ),
            crashLogger = crashLogger,
        )

    @Test
    fun onToggleShortcut_showsLimitMessageWithTheLimit_untilShown_whenLimitIsReached() =
        runTest {
            repository.cards.value = (1L..3L).map { card(id = it, isShortcut = true) } + card(id = 4L)
            val viewModel = viewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state().collect {} }

            viewModel.onToggleShortcut(cardId = 4L)
            assertEquals(LoyaltyCardsState.Message.ShortcutLimitReached(limit = 3), viewModel.state().value.message)

            viewModel.onMessageShown()
            assertEquals(LoyaltyCardsState.Message.None, viewModel.state().value.message)
        }

    @Test
    fun onToggleShortcut_showsAddedMessage_whenTheCardBecomesAShortcut() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state().collect {} }

            viewModel.onToggleShortcut(cardId = 1L)

            assertEquals(
                LoyaltyCardsState.Message.ShortcutAdded(cardName = "Card"),
                viewModel.state().value.message,
            )
        }

    @Test
    fun onToggleShortcut_showsGenericErrorAndRecordsIt_whenTheWriteFails() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            repository.shortcutFailure = IllegalStateException("db closed")
            val viewModel = viewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state().collect {} }

            viewModel.onToggleShortcut(cardId = 1L)

            assertEquals(LoyaltyCardsState.Message.GenericError, viewModel.state().value.message)
            assertEquals(1, crashLogger.exceptions.size)
        }

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

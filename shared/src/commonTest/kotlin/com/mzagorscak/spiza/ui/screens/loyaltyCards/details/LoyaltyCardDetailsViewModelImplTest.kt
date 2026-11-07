package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCaseImpl
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoyaltyCardDetailsViewModelImplTest {
    private val repository = FakeLoyaltyCardRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(cardId: Long) =
        LoyaltyCardDetailsViewModelImpl(
            cardId = cardId,
            observeLoyaltyCardByIdUseCase = ObserveLoyaltyCardByIdUseCaseImpl(repository = repository),
            deleteLoyaltyCardUseCase = DeleteLoyaltyCardUseCaseImpl(repository = repository),
            crashLogger = FakeCrashLogger(),
        )

    @Test
    fun state_isCardMissing_whenTheCardDoesNotExist() =
        runTest {
            val viewModel = viewModel(cardId = 42L)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state().collect {} }

            assertTrue(viewModel.state().value.isCardMissing)
        }

    @Test
    fun state_isNotMissing_afterTheCardIsDeletedFromThisScreen() =
        runTest {
            repository.cards.value = listOf(card(id = 1L))
            val viewModel = viewModel(cardId = 1L)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state().collect {} }

            viewModel.deleteCard()

            assertTrue(repository.cards.value.isEmpty())
            assertFalse(viewModel.state().value.isCardMissing)
        }

    private fun card(id: Long) =
        LoyaltyCard(
            id = id,
            name = "Card",
            colorArgb = 0,
            codeText = "123",
            format = LoyaltyCardFormat.Code128,
        )
}

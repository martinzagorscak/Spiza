package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCaseImpl
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

    private fun viewModel(cardId: Long) =
        LoyaltyCardDetailsViewModelImpl(
            cardId = cardId,
            observeLoyaltyCardByIdUseCase = ObserveLoyaltyCardByIdUseCaseImpl(repository = repository),
            deleteLoyaltyCardUseCase = DeleteLoyaltyCardUseCaseImpl(repository = repository),
            crashLogger = crashLogger,
        )

    @Test
    fun state_showsTheRequestedCard_onceLoaded() =
        runTest {
            val requested = card(id = 2L, name = "Shop")
            repository.cards.value = listOf(card(id = 1L), requested)
            val viewModel = viewModel(cardId = 2L)

            viewModel.state().test {
                assertThat(expectMostRecentItem())
                    .isEqualTo(LoyaltyCardDetailsState(card = requested.toPresentable(), isLoading = false, isCardMissing = false))
            }
        }

    @Test
    fun deleteCard_deletesThisCard_withoutReportingItMissing() =
        runTest {
            repository.cards.value = listOf(card(id = 1L), card(id = 2L))
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                expectMostRecentItem()

                viewModel.deleteCard()

                // The screen closes itself after deleting, so the card disappearing must not show the "missing" state.
                assertThat(awaitItem()).isEqualTo(LoyaltyCardDetailsState(card = null, isLoading = false, isCardMissing = false))
            }
            assertThat(repository.cards.value.map { it.id }).containsExactly(2L)
        }

    // region Error handling

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
                    .isEqualTo(LoyaltyCardDetailsState(card = null, isLoading = false, isCardMissing = false))
            }
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    // TODO(error-handling): user is not informed
    @Test
    fun deleteCard_keepsTheCardAndRecordsTheFailure_whenTheDeleteFails() =
        runTest {
            val failure = IllegalStateException("db closed")
            repository.cards.value = listOf(card(id = 1L))
            repository.writeFailure = failure
            val viewModel = viewModel(cardId = 1L)

            viewModel.state().test {
                assertThat(expectMostRecentItem().card?.id).isEqualTo(1L)

                viewModel.deleteCard()

                expectNoEvents()
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
    ) = LoyaltyCard(
        id = id,
        name = name,
        colorArgb = 0,
        codeText = "123",
        format = LoyaltyCardFormat.Code128,
    )
}

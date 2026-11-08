package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import app.cash.turbine.Event
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
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
                    .isEqualTo(LoyaltyCardDetailsState(card = null, isLoading = false, isCardMissing = false, isDeleted = true))
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
                    .isEqualTo(LoyaltyCardDetailsState(card = stored.toPresentable(), isLoading = false, hasError = true))
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
    ) = LoyaltyCard(
        id = id,
        name = name,
        colorArgb = 0,
        codeText = "123",
        format = LoyaltyCardFormat.Code128,
    )
}

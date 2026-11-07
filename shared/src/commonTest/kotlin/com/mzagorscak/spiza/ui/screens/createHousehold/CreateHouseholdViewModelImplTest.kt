package com.mzagorscak.spiza.ui.screens.createHousehold

import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.UpdateHouseholdUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateHouseholdViewModelImplTest {
    private val households = FakeHouseholdRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(scenario: HouseholdScenario = HouseholdScenario.Create) =
        CreateHouseholdViewModelImpl(
            scenario = scenario,
            createHousehold = CreateHouseholdUseCaseImpl(households),
            updateHousehold = UpdateHouseholdUseCaseImpl(households),
            crashLogger = logger,
        )

    private fun submittedViewModel(scenario: HouseholdScenario = HouseholdScenario.Create) =
        viewModel(scenario).apply {
            onNameChange("Home")
            createHousehold()
        }

    @Test
    fun createHousehold_setsQuotaExceededError_untilTheNameIsEdited() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = submittedViewModel()

            val failed = viewModel.state().value
            assertEquals(CreateHouseholdState.ErrorState.QuotaExceeded, failed.errorState)
            assertFalse(failed.isSubmitting)
            assertFalse(failed.isCreated)
            assertEquals(1, logger.exceptions.size)

            viewModel.onNameChange("Home 2")

            assertEquals(CreateHouseholdState.ErrorState.None, viewModel.state().value.errorState)
        }

    @Test
    fun createHousehold_setsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            val state = submittedViewModel().state().value

            assertEquals(CreateHouseholdState.ErrorState.Generic, state.errorState)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun createHousehold_clearsPreviousError_whenResubmittedSuccessfully() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = submittedViewModel()

            households.writeFailure = null
            viewModel.createHousehold()

            assertEquals(CreateHouseholdState.ErrorState.None, viewModel.state().value.errorState)
            assertTrue(viewModel.state().value.isCreated)
            assertEquals(listOf("Home"), households.createdHouseholds)
        }

    @Test
    fun createHousehold_updatesTheEditedHousehold_insteadOfCreatingANewOne() =
        runTest {
            val scenario = HouseholdScenario.Edit(householdId = "household-1", householdName = "Home", householdDescription = "")

            val state = submittedViewModel(scenario).state().value

            assertTrue(state.isCreated)
            assertEquals(listOf("household-1" to "Home"), households.updatedHouseholds)
            assertTrue(households.createdHouseholds.isEmpty())
        }
}

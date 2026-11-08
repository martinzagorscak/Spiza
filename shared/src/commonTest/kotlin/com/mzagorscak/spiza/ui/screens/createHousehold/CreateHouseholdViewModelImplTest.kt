package com.mzagorscak.spiza.ui.screens.createHousehold

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.CreateHouseholdResult
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
    fun init_prefillsTheHousehold_whenEditing() =
        runTest {
            val scenario = HouseholdScenario.Edit(householdId = "household-1", householdName = "Home", householdDescription = "Flat")

            val state = viewModel(scenario).state().value

            assertThat(state).isEqualTo(CreateHouseholdState(id = "household-1", name = "Home", description = "Flat"))
            assertThat(state.isEditScenario).isTrue()
        }

    @Test
    fun onNameChange_acceptsNameUpToTheMaxLength() =
        runTest {
            val viewModel = viewModel()
            val maxName = "a".repeat(CreateHouseholdState.MAX_NAME_LENGTH)

            viewModel.onNameChange(maxName)

            assertThat(viewModel.state().value.name).isEqualTo(maxName)
            assertThat(viewModel.state().value.canSubmit).isTrue()
        }

    @Test
    fun createHousehold_createsHouseholdWithTypedNameAndDescription() =
        runTest {
            val viewModel = viewModel()
            viewModel.onNameChange("Home")
            viewModel.onDescriptionChange("Our flat")

            viewModel.createHousehold()

            assertThat(households.createdHouseholds).containsExactly("Home" to "Our flat")
            val state = viewModel.state().value
            assertThat(state.isCreated).isTrue()
            assertThat(state.isSubmitting).isFalse()
            assertThat(state.errorState).isEqualTo(CreateHouseholdState.ErrorState.None)
        }

    @Test
    fun createHousehold_closesTheScreen_whenTheHouseholdWillSyncOnceOnline() =
        runTest {
            households.createHouseholdResult = CreateHouseholdResult.Created(householdId = null)

            val state = submittedViewModel().state().value

            assertThat(state.isCreated).isTrue()
            assertThat(state.errorState).isEqualTo(CreateHouseholdState.ErrorState.None)
        }

    @Test
    fun createHousehold_updatesTheEditedHousehold_insteadOfCreatingANewOne() =
        runTest {
            val scenario = HouseholdScenario.Edit(householdId = "household-1", householdName = "Home", householdDescription = "")

            val state = submittedViewModel(scenario).state().value

            assertThat(state.isCreated).isTrue()
            assertThat(households.updatedHouseholds).containsExactly(
                FakeHouseholdRepository.UpdatedHousehold(householdId = "household-1", name = "Home", description = ""),
            )
            assertThat(households.createdHouseholds).isEmpty()
        }

    // region Error handling

    @Test
    fun onNameChange_ignoresName_whenItIsLongerThanTheMaxLength() =
        runTest {
            val viewModel = viewModel()
            val maxName = "a".repeat(CreateHouseholdState.MAX_NAME_LENGTH)
            viewModel.onNameChange(maxName)

            viewModel.onNameChange(maxName + "a")

            assertThat(viewModel.state().value.name).isEqualTo(maxName)
        }

    @Test
    fun createHousehold_setsQuotaExceededError_untilTheNameIsEdited() =
        runTest {
            val failure = DataException(DataException.Reason.QuotaExceeded, "quota")
            households.writeFailure = failure
            val viewModel = submittedViewModel()

            val failed = viewModel.state().value
            assertThat(failed.errorState).isEqualTo(CreateHouseholdState.ErrorState.QuotaExceeded)
            assertThat(failed.isSubmitting).isFalse()
            assertThat(failed.isCreated).isFalse()
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)

            viewModel.onNameChange("Home 2")

            assertThat(viewModel.state().value.errorState).isEqualTo(CreateHouseholdState.ErrorState.None)
        }

    @Test
    fun createHousehold_setsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            val state = submittedViewModel().state().value

            assertThat(state.errorState).isEqualTo(CreateHouseholdState.ErrorState.Generic)
            assertThat(state.isSubmitting).isFalse()
        }

    @Test
    fun createHousehold_clearsPreviousError_whenResubmittedSuccessfully() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = submittedViewModel()

            households.writeFailure = null
            viewModel.createHousehold()

            assertThat(viewModel.state().value.errorState).isEqualTo(CreateHouseholdState.ErrorState.None)
            assertThat(viewModel.state().value.isCreated).isTrue()
            // The failed attempt and the successful retry.
            assertThat(households.createdHouseholds).containsExactly("Home" to "", "Home" to "")
        }

    @Test
    fun createHousehold_doesNotSubmit_whenNameIsBlank() =
        runTest {
            val viewModel = viewModel()
            viewModel.onNameChange("   ")

            viewModel.createHousehold()

            assertThat(households.createdHouseholds).isEmpty()
            assertThat(viewModel.state().value.isCreated).isFalse()
        }

    @Test
    fun createHousehold_staysOnScreenWithLimitError_whenUserReachedTheHouseholdLimit() =
        runTest {
            val limit = ConstraintsManagerImpl.maxHouseholdsPerUser
            households.createHouseholdResult = CreateHouseholdResult.LimitReached(limit = limit)

            val state = submittedViewModel().state().value

            assertThat(state.errorState).isEqualTo(CreateHouseholdState.ErrorState.HouseholdLimitReached(limit = limit))
            assertThat(state.isCreated).isFalse()
            assertThat(state.isSubmitting).isFalse()
            assertThat(households.createdHouseholds).containsExactly("Home" to "")
        }

    @Test
    fun createHousehold_staysOnScreenWithGenericError_whenUserIsNotSignedIn() =
        runTest {
            households.createHouseholdResult = CreateHouseholdResult.NotSignedIn

            val state = submittedViewModel().state().value

            assertThat(state.errorState).isEqualTo(CreateHouseholdState.ErrorState.Generic)
            assertThat(state.isCreated).isFalse()
            assertThat(state.isSubmitting).isFalse()
        }

    @Test
    fun createHousehold_createsOnlyOnce_whenSubmittedAgainWhileSubmitting() =
        runTest {
            households.neverCompleteWrites = true
            val viewModel = submittedViewModel()
            assertThat(viewModel.state().value.isSubmitting).isTrue()

            viewModel.createHousehold()

            assertThat(households.createdHouseholds).hasSize(1)
        }

    // endregion
}

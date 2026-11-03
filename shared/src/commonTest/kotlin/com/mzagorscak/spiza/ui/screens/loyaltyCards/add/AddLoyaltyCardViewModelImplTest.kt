package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import com.mzagorscak.spiza.domain.loyaltyCard.LoyaltyCardCodes
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanEnterLoyaltyCardCodeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CheckPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.DetectLoyaltyCardFormatUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.NormalizeScannedLoyaltyCardCodeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.OpenAppSettingsUseCase
import com.mzagorscak.spiza.domain.usecase.RequestPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.ValidateLoyaltyCardCodeUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeLoyaltyCardRepository
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
class AddLoyaltyCardViewModelImplTest {
    private val repository = FakeLoyaltyCardRepository()
    private val crashLogger = FakeCrashLogger()

    private val grantedPermission =
        object : CheckPermissionUseCase, RequestPermissionUseCase {
            override suspend fun invoke(permission: PermissionType): PermissionStatus = PermissionStatus.Granted
        }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        AddLoyaltyCardViewModelImpl(
            addLoyaltyCardUseCase = AddLoyaltyCardUseCaseImpl(repository = repository),
            validateLoyaltyCardCodeUseCase = ValidateLoyaltyCardCodeUseCaseImpl(),
            canEnterLoyaltyCardCodeUseCase = CanEnterLoyaltyCardCodeUseCaseImpl(),
            detectLoyaltyCardFormatUseCase = DetectLoyaltyCardFormatUseCaseImpl(),
            normalizeScannedLoyaltyCardCodeUseCase = NormalizeScannedLoyaltyCardCodeUseCaseImpl(),
            checkPermissionUseCase = grantedPermission,
            requestPermissionUseCase = grantedPermission,
            openAppSettingsUseCase = object : OpenAppSettingsUseCase {
                override fun invoke() = Unit
            },
            crashLogger = crashLogger,
        )

    private fun AddLoyaltyCardViewModelImpl.scanTwice(code: ScannedCode) {
        onCodeScanned(code)
        onCodeScanned(code)
    }

    @Test
    fun onCodeScanned_ignoresASingleRead() {
        val viewModel = viewModel()

        viewModel.onCodeScanned(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        assertEquals(AddLoyaltyCardState.ScanStatus.Scanning, viewModel.state().value.scanStatus)
        assertEquals("", viewModel.state().value.codeText)
    }

    @Test
    fun onCodeScanned_acceptsCodeAndFormat_afterTwoIdenticalReads() {
        val viewModel = viewModel()

        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        val state = viewModel.state().value
        assertEquals(AddLoyaltyCardState.ScanStatus.Scanned, state.scanStatus)
        assertEquals("4006381333931", state.codeText)
        assertEquals(LoyaltyCardFormat.Ean13, state.format)
        assertTrue(state.isCodeValid)
    }

    @Test
    fun onCodeScanned_restartsConfirmation_whenTheReadChanges() {
        val viewModel = viewModel()

        viewModel.onCodeScanned(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))
        viewModel.onCodeScanned(ScannedCode(value = "96385074", format = LoyaltyCardFormat.Ean8))

        assertEquals(AddLoyaltyCardState.ScanStatus.Scanning, viewModel.state().value.scanStatus)
    }

    @Test
    fun onCodeScanned_normalizesUpcAReportedAsEan13() {
        val viewModel = viewModel()

        viewModel.scanTwice(ScannedCode(value = "0036000291452", format = LoyaltyCardFormat.Ean13))

        assertEquals("036000291452", viewModel.state().value.codeText)
        assertEquals(LoyaltyCardFormat.UpcA, viewModel.state().value.format)
    }

    @Test
    fun onCodeScanned_showsUnsupported_andLogsOnce_whenFormatIsUnknown() {
        val viewModel = viewModel()

        repeat(2) { viewModel.scanTwice(ScannedCode(value = "data matrix content", format = null)) }

        assertEquals(AddLoyaltyCardState.ScanStatus.Unsupported, viewModel.state().value.scanStatus)
        assertFalse(viewModel.state().value.isCodeValid)
        assertEquals(1, crashLogger.warnings.size)
    }

    @Test
    fun onScanAgainClick_clearsTheScannedCode() {
        val viewModel = viewModel()
        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        viewModel.onScanAgainClick()

        assertEquals(AddLoyaltyCardState.ScanStatus.Scanning, viewModel.state().value.scanStatus)
        assertEquals("", viewModel.state().value.codeText)
        assertFalse(viewModel.state().value.isCodeValid)
    }

    @Test
    fun onCodeChange_detectsFormat_whenTyped() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)

        viewModel.onCodeChange(" 4006381333931 ")

        assertEquals(LoyaltyCardFormat.Ean13, viewModel.state().value.format)
        assertTrue(viewModel.state().value.isCodeValid)
    }

    @Test
    fun onCodeChange_acceptsInput_upToTheMaximumLength() {
        val viewModel = viewModel()
        val longest = "A".repeat(LoyaltyCardCodes.MAX_CODE_LENGTH)

        viewModel.onCodeChange(longest)

        assertEquals(longest, viewModel.state().value.codeText)
    }

    @Test
    fun onCodeChange_rejectsInput_overTheMaximumLength() {
        val viewModel = viewModel()
        viewModel.onCodeChange("4006381333931")

        viewModel.onCodeChange("1".repeat(LoyaltyCardCodes.MAX_CODE_LENGTH + 1))

        val state = viewModel.state().value
        assertEquals("4006381333931", state.codeText)
        assertEquals(LoyaltyCardFormat.Ean13, state.format)
        assertTrue(state.isCodeValid)
    }

    @Test
    fun onCodeChange_keepsManuallySelectedFormat_andReportsInvalidCode() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
        viewModel.onFormatChange(LoyaltyCardFormat.Ean13)

        viewModel.onCodeChange("12345")

        assertEquals(LoyaltyCardFormat.Ean13, viewModel.state().value.format)
        assertTrue(viewModel.state().value.showCodeError)
        assertFalse(viewModel.state().value.isSaveEnabled)
    }

    @Test
    fun onSaveClick_savesScannedContentUntrimmed() =
        runTest {
            val scannedValue = "]C1 0104012345\u001D"
            val viewModel = viewModel()
            viewModel.onCardNameChange("  Shop  ")
            viewModel.scanTwice(ScannedCode(value = scannedValue, format = LoyaltyCardFormat.Code128))

            viewModel.onSaveClick()

            val saved = repository.addedCards.single()
            assertEquals("Shop", saved.name)
            assertEquals(scannedValue, saved.codeText)
            assertEquals(LoyaltyCardFormat.Code128, saved.format)
        }

    @Test
    fun onSaveClick_trimsTypedCode() =
        runTest {
            val viewModel = viewModel()
            viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
            viewModel.onCardNameChange("Shop")
            viewModel.onCodeChange(" 96385074 ")

            viewModel.onSaveClick()

            assertEquals("96385074", repository.addedCards.single().codeText)
            assertEquals(LoyaltyCardFormat.Ean8, repository.addedCards.single().format)
        }

    @Test
    fun onSaveClick_doesNothing_whenCodeIsInvalid() =
        runTest {
            val viewModel = viewModel()
            viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
            viewModel.onCardNameChange("Shop")
            viewModel.onFormatChange(LoyaltyCardFormat.Ean13)
            viewModel.onCodeChange("4006381333932")

            viewModel.onSaveClick()

            assertTrue(repository.addedCards.isEmpty())
        }
}

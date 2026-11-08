package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.ui.graphics.toArgb
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.loyaltyCard.LoyaltyCardCodes
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanEnterLoyaltyCardCodeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CheckPermissionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DetectLoyaltyCardFormatUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.NormalizeScannedLoyaltyCardCodeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.OpenAppSettingsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.RequestPermissionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ValidateLoyaltyCardCodeUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeLoyaltyCardRepository
import com.mzagorscak.spiza.fakes.FakePermissionRepository
import com.mzagorscak.spiza.ui.theme.LoyaltyCardColors
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
class AddLoyaltyCardViewModelImplTest {
    private val repository = FakeLoyaltyCardRepository()
    private val crashLogger = FakeCrashLogger()

    private val permissions = FakePermissionRepository()

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
            checkPermissionUseCase = CheckPermissionUseCaseImpl(repository = permissions),
            requestPermissionUseCase = RequestPermissionUseCaseImpl(repository = permissions),
            openAppSettingsUseCase = OpenAppSettingsUseCaseImpl(repository = permissions),
            crashLogger = crashLogger,
        )

    private fun AddLoyaltyCardViewModelImpl.scanTwice(code: ScannedCode) {
        onCodeScanned(code)
        onCodeScanned(code)
    }

    @Test
    fun onInputTypeChange_keepsTheScannedCode_whenSwitchingToManualEntry() {
        val viewModel = viewModel()
        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)

        val state = viewModel.state().value
        assertThat(state.inputType).isEqualTo(AddLoyaltyCardState.InputType.Manual)
        assertThat(state.codeText).isEqualTo("4006381333931")
        assertThat(state.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanned)
        assertThat(state.isCodeValid).isTrue()
    }

    @Test
    fun onInputTypeChange_clearsTheCodeAndRequestsCamera_whenSwitchingBackToTheCamera() {
        permissions.checkStatus = PermissionStatus.NotGranted
        permissions.requestStatus = PermissionStatus.Granted
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
        viewModel.onFormatChange(LoyaltyCardFormat.Ean8)
        viewModel.onCodeChange("96385074")

        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Camera)

        val state = viewModel.state().value
        assertThat(state.inputType).isEqualTo(AddLoyaltyCardState.InputType.Camera)
        assertThat(state.codeText).isEqualTo("")
        assertThat(state.isCodeValid).isFalse()
        assertThat(state.isFormatSelectedManually).isFalse()
        assertThat(permissions.requestedPermissions).containsExactly(PermissionType.CAMERA)
        assertThat(state.permissionStatus).isEqualTo(PermissionStatus.Granted)
    }

    @Test
    fun onCodeChange_detectsFormat_whenTyped() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)

        viewModel.onCodeChange(" 4006381333931 ")

        assertThat(viewModel.state().value.format).isEqualTo(LoyaltyCardFormat.Ean13)
        assertThat(viewModel.state().value.isCodeValid).isTrue()
    }

    @Test
    fun presentableCard_showsTheCode_onlyOnceItIsValid() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
        viewModel.onFormatChange(LoyaltyCardFormat.Ean13)

        viewModel.onCodeChange("400638133393")
        assertThat(
            viewModel
                .state()
                .value.presentableCard.codeText,
        ).isEqualTo("")

        viewModel.onCodeChange("4006381333931")
        assertThat(
            viewModel
                .state()
                .value.presentableCard.codeText,
        ).isEqualTo("4006381333931")
    }

    @Test
    fun onCodeChange_editingAScannedCode_isSavedTrimmedLikeATypedCode() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCardNameChange("Shop")
            viewModel.scanTwice(ScannedCode(value = "CARD-0042", format = LoyaltyCardFormat.Code128))
            viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)

            viewModel.onCodeChange(" CARD-0043 ")
            viewModel.onSaveClick()

            assertThat(viewModel.state().value.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanning)
            assertThat(repository.addedCards.single().codeText).isEqualTo("CARD-0043")
            assertThat(repository.addedCards.single().format).isEqualTo(LoyaltyCardFormat.Code128)
        }

    @Test
    fun onFormatChange_revalidatesTheCurrentCode_inTheChosenFormat() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
        viewModel.onCodeChange("4006381333931")

        viewModel.onFormatChange(LoyaltyCardFormat.Code128)
        assertThat(viewModel.state().value.format).isEqualTo(LoyaltyCardFormat.Code128)
        assertThat(viewModel.state().value.isFormatSelectedManually).isTrue()
        assertThat(viewModel.state().value.isCodeValid).isTrue()

        viewModel.onFormatChange(LoyaltyCardFormat.Ean8)
        assertThat(viewModel.state().value.isCodeValid).isFalse()
    }

    @Test
    fun onCodeScanned_acceptsCode_onlyAfterTwoIdenticalReadsInARow() {
        val viewModel = viewModel()

        viewModel.onCodeScanned(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))
        // A single read is ignored.
        assertThat(viewModel.state().value.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanning)

        viewModel.onCodeScanned(ScannedCode(value = "96385074", format = LoyaltyCardFormat.Ean8))
        // A changed read restarts.
        assertThat(viewModel.state().value.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanning)

        viewModel.onCodeScanned(ScannedCode(value = "96385074", format = LoyaltyCardFormat.Ean8))
        val state = viewModel.state().value
        assertThat(state.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanned)
        assertThat(state.codeText).isEqualTo("96385074")
        assertThat(state.format).isEqualTo(LoyaltyCardFormat.Ean8)
        assertThat(state.isCodeValid).isTrue()
    }

    @Test
    fun onCodeScanned_ignoresFurtherReads_onceACodeIsScanned() {
        val viewModel = viewModel()
        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        viewModel.scanTwice(ScannedCode(value = "96385074", format = LoyaltyCardFormat.Ean8))

        assertThat(viewModel.state().value.codeText).isEqualTo("4006381333931")
        assertThat(viewModel.state().value.format).isEqualTo(LoyaltyCardFormat.Ean13)
    }

    @Test
    fun onCodeScanned_normalizesUpcAReportedAsEan13() {
        val viewModel = viewModel()

        viewModel.scanTwice(ScannedCode(value = "0036000291452", format = LoyaltyCardFormat.Ean13))

        assertThat(viewModel.state().value.codeText).isEqualTo("036000291452")
        assertThat(viewModel.state().value.format).isEqualTo(LoyaltyCardFormat.UpcA)
    }

    @Test
    fun onImageCodeDecoded_acceptsCode_afterASingleRead() {
        val viewModel = viewModel()

        viewModel.onImageCodeDecoded(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        val state = viewModel.state().value
        assertThat(state.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanned)
        assertThat(state.codeText).isEqualTo("4006381333931")
        assertThat(state.format).isEqualTo(LoyaltyCardFormat.Ean13)
        assertThat(state.isCodeValid).isTrue()
    }

    @Test
    fun onCodeScanned_needsTwoNewReads_afterAnImageWasDecoded() {
        val viewModel = viewModel()
        val code = ScannedCode(value = "96385074", format = LoyaltyCardFormat.Ean8)
        viewModel.onCodeScanned(code)

        viewModel.onImageCodeDecoded(null)
        viewModel.onCodeScanned(code)

        assertThat(viewModel.state().value.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.NotFoundInImage)
    }

    @Test
    fun onScanAgainClick_clearsTheScannedCode() {
        val viewModel = viewModel()
        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        viewModel.onScanAgainClick()

        assertThat(viewModel.state().value.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Scanning)
        assertThat(viewModel.state().value.codeText).isEqualTo("")
        assertThat(viewModel.state().value.isCodeValid).isFalse()
    }

    @Test
    fun onSaveClick_savesScannedContentUntrimmed() =
        runTest {
            val scannedValue = "]C1 0104012345\u001D"
            val viewModel = viewModel()
            viewModel.onCardNameChange("  Shop  ")
            viewModel.scanTwice(ScannedCode(value = scannedValue, format = LoyaltyCardFormat.Code128))

            viewModel.onSaveClick()

            val saved = repository.cards.value.single()
            assertThat(saved.name).isEqualTo("Shop")
            assertThat(saved.codeText).isEqualTo(scannedValue)
            assertThat(saved.format).isEqualTo(LoyaltyCardFormat.Code128)
        }

    @Test
    fun onSaveClick_trimsTypedCode() =
        runTest {
            val viewModel = viewModel()
            viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
            viewModel.onCardNameChange("Shop")
            viewModel.onCodeChange(" 96385074 ")

            viewModel.onSaveClick()

            assertThat(repository.addedCards.single().codeText).isEqualTo("96385074")
            assertThat(repository.addedCards.single().format).isEqualTo(LoyaltyCardFormat.Ean8)
        }

    @Test
    fun onSaveClick_savesTheSelectedColor() =
        runTest {
            val color = LoyaltyCardColors.palette.last { it != LoyaltyCardColors.defaultColor }
            val viewModel = viewModel()
            viewModel.onCardNameChange("Shop")
            viewModel.onColorSelect(color)
            viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

            viewModel.onSaveClick()

            assertThat(repository.addedCards.single().colorArgb).isEqualTo(color.toArgb())
        }

    @Test
    fun onSaveClick_reportsTheCardSaved_onceItIsStored() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCardNameChange("Shop")
            viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

            viewModel.onSaveClick()

            val state = viewModel.state().value
            assertThat(state.isSaved).isTrue()
            assertThat(state.isSaving).isFalse()
            assertThat(state.isSaveEnabled).isFalse()
            assertThat(repository.cards.value).hasSize(1)
        }

    @Test
    fun onSaveClick_savesOnce_whileASaveIsInProgress() =
        runTest {
            repository.neverCompleteWrites = true
            val viewModel = viewModel()
            viewModel.onCardNameChange("Shop")
            viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

            viewModel.onSaveClick()
            viewModel.onSaveClick()

            assertThat(repository.addedCards).hasSize(1)
            assertThat(viewModel.state().value.isSaving).isTrue()
            assertThat(viewModel.state().value.isSaved).isFalse()
            assertThat(viewModel.state().value.isSaveEnabled).isFalse()
        }

    @Test
    fun checkPermission_requestsAndStoresReturnedStatus_whenCameraIsNotGranted() {
        permissions.checkStatus = PermissionStatus.NotGranted
        permissions.requestStatus = PermissionStatus.Denied
        val viewModel = viewModel()

        viewModel.checkPermission()

        assertThat(permissions.requestedPermissions).containsExactly(PermissionType.CAMERA)
        assertThat(viewModel.state().value.permissionStatus).isEqualTo(PermissionStatus.Denied)
    }

    @Test
    fun checkPermission_doesNotRequest_whenCameraIsAlreadyGranted() {
        val viewModel = viewModel()

        viewModel.checkPermission()

        assertThat(permissions.requestedPermissions).isEmpty()
        assertThat(viewModel.state().value.permissionStatus).isEqualTo(PermissionStatus.Granted)
    }

    @Test
    fun checkPermission_onlyStoresTheStatus_duringManualEntry() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
        permissions.checkStatus = PermissionStatus.NotGranted

        viewModel.checkPermission()

        assertThat(permissions.checkedPermissions).containsExactly(PermissionType.CAMERA)
        assertThat(permissions.requestedPermissions).isEmpty()
        assertThat(viewModel.state().value.permissionStatus).isEqualTo(PermissionStatus.NotGranted)
    }

    @Test
    fun requestPermission_requestsTheCamera_andStoresTheResult() {
        permissions.requestStatus = PermissionStatus.DeniedAlways
        val viewModel = viewModel()

        viewModel.requestPermission()

        assertThat(permissions.requestedPermissions).containsExactly(PermissionType.CAMERA)
        assertThat(viewModel.state().value.permissionStatus).isEqualTo(PermissionStatus.DeniedAlways)
    }

    @Test
    fun openSettings_opensTheAppSettings() {
        val viewModel = viewModel()

        viewModel.openSettings()

        assertThat(permissions.openAppSettingsCount).isEqualTo(1)
    }

    // region Error handling

    @Test
    fun onCodeChange_rejectsInput_overTheMaximumLength() {
        val viewModel = viewModel()
        viewModel.onCodeChange("4006381333931")

        viewModel.onCodeChange("1".repeat(LoyaltyCardCodes.MAX_CODE_LENGTH + 1))

        val state = viewModel.state().value
        assertThat(state.codeText).isEqualTo("4006381333931")
        assertThat(state.format).isEqualTo(LoyaltyCardFormat.Ean13)
        assertThat(state.isCodeValid).isTrue()
    }

    @Test
    fun onCodeChange_keepsManuallySelectedFormat_andReportsInvalidCode() {
        val viewModel = viewModel()
        viewModel.onInputTypeChange(AddLoyaltyCardState.InputType.Manual)
        viewModel.onFormatChange(LoyaltyCardFormat.Ean13)

        viewModel.onCodeChange("12345")

        assertThat(viewModel.state().value.format).isEqualTo(LoyaltyCardFormat.Ean13)
        assertThat(viewModel.state().value.showCodeError).isTrue()
        assertThat(viewModel.state().value.isSaveEnabled).isFalse()
    }

    @Test
    fun onCodeScanned_showsUnsupported_andLogsOnce_whenFormatIsUnknown() {
        val viewModel = viewModel()

        repeat(2) { viewModel.scanTwice(ScannedCode(value = "data matrix content", format = null)) }

        assertThat(viewModel.state().value.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Unsupported)
        assertThat(viewModel.state().value.isCodeValid).isFalse()
        assertThat(crashLogger.warnings).hasSize(1)
    }

    @Test
    fun onImageCodeDecoded_showsNotFoundAndClearsACameraCode_whenImageHasNoCode() {
        val viewModel = viewModel()
        viewModel.onCardNameChange("Store")
        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        viewModel.onImageCodeDecoded(null)

        val state = viewModel.state().value
        assertThat(state.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.NotFoundInImage)
        assertThat(state.codeText).isEqualTo("")
        assertThat(state.isCodeValid).isFalse()
        assertThat(state.isSaveEnabled).isFalse()
    }

    @Test
    fun onImageCodeDecoded_showsUnsupportedAndClearsACameraCode_whenImageCodeIsUnsupported() {
        val viewModel = viewModel()
        viewModel.onCardNameChange("Store")
        viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

        viewModel.onImageCodeDecoded(ScannedCode(value = "data matrix content", format = null))

        val state = viewModel.state().value
        assertThat(state.scanStatus).isEqualTo(AddLoyaltyCardState.ScanStatus.Unsupported)
        assertThat(state.codeText).isEqualTo("")
        assertThat(state.isSaveEnabled).isFalse()
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

            assertThat(repository.addedCards).isEmpty()
        }

    @Test
    fun onSaveClick_showsTheErrorAndKeepsTheScreenOpen_whenSavingFails() =
        runTest {
            val failure = IllegalStateException("db closed")
            repository.writeFailure = failure
            val viewModel = viewModel()
            viewModel.onCardNameChange("Shop")
            viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))

            viewModel.onSaveClick()

            val state = viewModel.state().value
            assertThat(state.hasError).isTrue()
            assertThat(state.isSaved).isFalse()
            assertThat(state.isSaving).isFalse()
            // The form is kept so the user can save again.
            assertThat(state.cardName).isEqualTo("Shop")
            assertThat(state.isSaveEnabled).isTrue()
            assertThat(repository.cards.value).isEmpty()
            assertThat(
                crashLogger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun onErrorShown_clearsTheError_afterAFailedSave() =
        runTest {
            repository.writeFailure = IllegalStateException("db closed")
            val viewModel = viewModel()
            viewModel.onCardNameChange("Shop")
            viewModel.scanTwice(ScannedCode(value = "4006381333931", format = LoyaltyCardFormat.Ean13))
            viewModel.onSaveClick()

            viewModel.onErrorShown()

            assertThat(viewModel.state().value.hasError).isFalse()
            assertThat(viewModel.state().value.isSaveEnabled).isTrue()
        }

    @Test
    fun checkPermission_doesNotRequest_whenCameraIsPermanentlyDenied() {
        permissions.checkStatus = PermissionStatus.DeniedAlways
        val viewModel = viewModel()

        viewModel.checkPermission()

        assertThat(permissions.requestedPermissions).isEmpty()
        assertThat(viewModel.state().value.permissionStatus).isEqualTo(PermissionStatus.DeniedAlways)
    }

    // endregion
}

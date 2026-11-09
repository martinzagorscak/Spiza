package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.CanEnterLoyaltyCardCodeUseCase
import com.mzagorscak.spiza.domain.usecase.CheckPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.DetectLoyaltyCardFormatUseCase
import com.mzagorscak.spiza.domain.usecase.GetLoyaltyCardByIdUseCase
import com.mzagorscak.spiza.domain.usecase.NormalizeScannedLoyaltyCardCodeUseCase
import com.mzagorscak.spiza.domain.usecase.OpenAppSettingsUseCase
import com.mzagorscak.spiza.domain.usecase.RequestPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.ValidateLoyaltyCardCodeUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** [cardId] is the card to edit, or `null` to add a new card. */
internal class AddLoyaltyCardViewModelImpl(
    private val cardId: Long?,
    private val getLoyaltyCardByIdUseCase: GetLoyaltyCardByIdUseCase,
    private val addLoyaltyCardUseCase: AddLoyaltyCardUseCase,
    private val updateLoyaltyCardUseCase: UpdateLoyaltyCardUseCase,
    private val validateLoyaltyCardCodeUseCase: ValidateLoyaltyCardCodeUseCase,
    private val canEnterLoyaltyCardCodeUseCase: CanEnterLoyaltyCardCodeUseCase,
    private val detectLoyaltyCardFormatUseCase: DetectLoyaltyCardFormatUseCase,
    private val normalizeScannedLoyaltyCardCodeUseCase: NormalizeScannedLoyaltyCardCodeUseCase,
    private val checkPermissionUseCase: CheckPermissionUseCase,
    private val requestPermissionUseCase: RequestPermissionUseCase,
    private val openAppSettingsUseCase: OpenAppSettingsUseCase,
    private val crashLogger: CrashLogger,
) : AddLoyaltyCardViewModel() {
    private val state = MutableStateFlow(
        AddLoyaltyCardState(
            isEditing = cardId != null,
            // An existing card already has a code, so editing starts without opening the camera.
            inputType = if (cardId != null) AddLoyaltyCardState.InputType.Manual else AddLoyaltyCardState.InputType.Camera,
        ),
    )

    // A code is accepted only after two identical reads in a row: a single frame can be misread (e.g. a partly visible barcode).
    private var pendingScan: ScannedCode? = null

    init {
        cardId?.let(::loadCard)
    }

    override fun state(): StateFlow<AddLoyaltyCardState> = state.asStateFlow()

    private fun loadCard(id: Long) {
        launchSafely(
            crashLogger = crashLogger,
            context = "Loading loyalty card to edit failed",
            onError = { state.update { it.copy(hasError = true) } },
        ) {
            val card = getLoyaltyCardByIdUseCase(id)?.toPresentable()
            if (card == null) {
                // E.g. deleted while the screen was being opened; the details screen behind shows that it is gone.
                crashLogger.logWarning("Loyalty card to edit not found (id: $id)")
                state.update { it.copy(isCardMissing = true) }
                return@launchSafely
            }
            state.update {
                it.copy(
                    cardName = card.name,
                    selectedColor = card.color,
                    codeText = card.codeText,
                    format = card.format,
                    isFormatSelectedManually = true,
                    isCodeValid = isValid(value = card.codeText, format = card.format),
                    // The stored code is exact content (a scanned code may end in control characters), so it is kept
                    // untrimmed like a scanned one until the user edits it.
                    scanStatus = AddLoyaltyCardState.ScanStatus.Scanned,
                )
            }
        }
    }

    override fun onCardNameChange(name: String) {
        state.update { it.copy(cardName = name) }
    }

    override fun onColorSelect(color: Color) {
        state.update { it.copy(selectedColor = color) }
    }

    override fun onCodeChange(code: String) {
        // Over-long input is rejected rather than cut: a truncated paste could form a different, still valid code.
        if (canEnterLoyaltyCardCodeUseCase(code).not()) return
        state.update { current ->
            val trimmed = code.trim()
            val format = when {
                current.isFormatSelectedManually || trimmed.isEmpty() -> current.format
                else -> detectLoyaltyCardFormatUseCase(trimmed)
            }
            current.copy(
                codeText = code,
                format = format,
                isCodeValid = isValid(value = trimmed, format = format),
                // Once edited by hand the code is no longer the exact scanned content.
                scanStatus = AddLoyaltyCardState.ScanStatus.Scanning,
            )
        }
    }

    override fun onFormatChange(format: LoyaltyCardFormat) {
        state.update { current ->
            current.copy(
                format = format,
                isFormatSelectedManually = true,
                isCodeValid = isValid(value = current.codeText.trim(), format = format),
            )
        }
    }

    override fun onCodeScanned(code: ScannedCode) {
        if (state.value.scanStatus == AddLoyaltyCardState.ScanStatus.Scanned) return
        if (code != pendingScan) {
            pendingScan = code
            return
        }
        pendingScan = null
        acceptScannedCode(code)
    }

    override fun onImageCodeDecoded(code: ScannedCode?) {
        // A still image is read once and reliably, so it skips the two-read confirmation of the camera.
        pendingScan = null
        // The camera may have confirmed a code while the picker was open; the picked image replaces it,
        // otherwise a code the user never saw could be saved.
        state.update { it.resetCode() }
        if (code == null) {
            state.update { it.copy(scanStatus = AddLoyaltyCardState.ScanStatus.NotFoundInImage) }
            return
        }
        acceptScannedCode(code)
    }

    private fun acceptScannedCode(code: ScannedCode) {
        val format = code.format
        val normalized = format?.let { normalizeScannedLoyaltyCardCodeUseCase(LoyaltyCardCode(value = code.value, format = it)) }
        if (normalized == null || validateLoyaltyCardCodeUseCase(normalized).not()) {
            if (state.value.scanStatus != AddLoyaltyCardState.ScanStatus.Unsupported) {
                crashLogger.logWarning("Scanned loyalty card code is not supported (format: ${format ?: "unknown"})")
            }
            state.update { it.copy(scanStatus = AddLoyaltyCardState.ScanStatus.Unsupported) }
            return
        }
        state.update {
            it.copy(
                codeText = normalized.value,
                format = normalized.format,
                isFormatSelectedManually = true,
                isCodeValid = true,
                scanStatus = AddLoyaltyCardState.ScanStatus.Scanned,
            )
        }
    }

    override fun onScanAgainClick() {
        pendingScan = null
        state.update { it.resetCode() }
    }

    override fun onInputTypeChange(inputType: AddLoyaltyCardState.InputType) {
        if (inputType == state.value.inputType) return
        // A scanned code is kept when switching to manual entry so it can be corrected; the camera always starts fresh.
        state.update { current ->
            when (inputType) {
                AddLoyaltyCardState.InputType.Camera -> current.resetCode().copy(inputType = inputType)
                AddLoyaltyCardState.InputType.Manual -> current.copy(inputType = inputType)
            }
        }
        if (inputType == AddLoyaltyCardState.InputType.Camera) {
            pendingScan = null
            requestCameraPermissionIfNecessary()
        }
    }

    override fun onSaveClick() {
        val currentState = state.value
        if (currentState.isSaveEnabled.not()) return
        val isScanned = currentState.scanStatus == AddLoyaltyCardState.ScanStatus.Scanned
        state.update { it.copy(isSaving = true, hasError = false) }
        launchSafely(
            crashLogger = crashLogger,
            context = "Saving loyalty card failed",
            // The form stays filled in so the user can retry instead of losing the card.
            onError = { state.update { it.copy(isSaving = false, hasError = true) } },
        ) {
            val card = LoyaltyCard(
                id = cardId ?: 0L,
                name = currentState.cardName.trim(),
                colorArgb = currentState.selectedColor.toArgb(),
                // Scanned content may legitimately end in control characters (e.g. a GS1 separator), so it is never trimmed.
                codeText = if (isScanned) currentState.codeText else currentState.codeText.trim(),
                format = currentState.format,
            )
            if (cardId != null) updateLoyaltyCardUseCase(card) else addLoyaltyCardUseCase(card)
            state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    override fun onErrorShown() {
        state.update { it.copy(hasError = false) }
    }

    override fun checkPermission() {
        if (state.value.inputType == AddLoyaltyCardState.InputType.Camera) {
            requestCameraPermissionIfNecessary()
            return
        }
        launchSafely(crashLogger = crashLogger, context = "Checking camera permission failed") {
            val status = checkPermissionUseCase(PermissionType.CAMERA)
            state.update { it.copy(permissionStatus = status) }
        }
    }

    override fun requestPermission() {
        launchSafely(crashLogger = crashLogger, context = "Requesting camera permission failed") {
            val status = requestPermissionUseCase(PermissionType.CAMERA)
            state.update { it.copy(permissionStatus = status) }
        }
    }

    override fun openSettings() {
        openAppSettingsUseCase()
    }

    private fun isValid(
        value: String,
        format: LoyaltyCardFormat,
    ): Boolean = validateLoyaltyCardCodeUseCase(LoyaltyCardCode(value = value, format = format))

    private fun AddLoyaltyCardState.resetCode(): AddLoyaltyCardState =
        copy(
            codeText = "",
            isCodeValid = false,
            isFormatSelectedManually = false,
            scanStatus = AddLoyaltyCardState.ScanStatus.Scanning,
        )

    private fun requestCameraPermissionIfNecessary() {
        launchSafely(crashLogger = crashLogger, context = "Checking camera permission failed") {
            val cameraPermissionStatus = checkPermissionUseCase(PermissionType.CAMERA)
            val isNotGranted = cameraPermissionStatus != PermissionStatus.Granted
            val isNotPermanentlyDenied = cameraPermissionStatus != PermissionStatus.DeniedAlways
            state.update { it.copy(permissionStatus = cameraPermissionStatus) }
            if (isNotGranted && isNotPermanentlyDenied) {
                requestPermission()
            }
        }
    }
}

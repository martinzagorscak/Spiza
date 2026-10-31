package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.CheckPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.OpenAppSettingsUseCase
import com.mzagorscak.spiza.domain.usecase.RequestPermissionUseCase
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AddLoyaltyCardViewModelImpl(
    private val addLoyaltyCardUseCase: AddLoyaltyCardUseCase,
    private val checkPermissionUseCase: CheckPermissionUseCase,
    private val requestPermissionUseCase: RequestPermissionUseCase,
    private val openAppSettingsUseCase: OpenAppSettingsUseCase,
    private val crashLogger: CrashLogger,
) : AddLoyaltyCardViewModel() {
    private val state = MutableStateFlow(AddLoyaltyCardState())

    override fun state(): StateFlow<AddLoyaltyCardState> = state.asStateFlow()

    override fun onCardNameChange(name: String) {
        state.update { it.copy(cardName = name) }
    }

    override fun onColorSelect(color: Color) {
        state.update { it.copy(selectedColor = color) }
    }

    override fun onCodeChange(code: String) {
        state.update { it.copy(codeText = code) }
    }

    override fun onInputTypeChange(inputType: AddLoyaltyCardState.InputType) {
        state.update { it.copy(inputType = inputType) }
        if (inputType == AddLoyaltyCardState.InputType.Camera) {
            requestCameraPermissionIfNecessary()
        }
    }

    override fun onFormatChange(format: LoyaltyCardFormat) {
        state.update { it.copy(format = format) }
    }

    override fun onSaveClick() {
        val currentState = state.value
        if (currentState.isSaveEnabled.not()) return
        launchSafely(crashLogger = crashLogger, context = "Saving loyalty card failed") {
            addLoyaltyCardUseCase(
                LoyaltyCard(
                    name = currentState.cardName.trim(),
                    colorArgb = currentState.selectedColor.toArgb(),
                    codeText = currentState.codeText.trim(),
                    format = currentState.format,
                ),
            )
        }
    }

    override fun checkPermission() {
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

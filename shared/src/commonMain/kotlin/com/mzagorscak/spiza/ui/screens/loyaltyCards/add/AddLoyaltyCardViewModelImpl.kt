package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.repository.PermissionRepository
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AddLoyaltyCardViewModelImpl(
    private val addLoyaltyCardUseCase: AddLoyaltyCardUseCase,
    private val permissionRepository: PermissionRepository,
) : AddLoyaltyCardViewModel() {
    private val state = MutableStateFlow(AddLoyaltyCardState())

    init {
        initialRequestCameraPermissionIfNecessary()
    }

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
    }

    override fun onFormatChange(isQr: Boolean) {
        state.update { it.copy(isQrFormat = isQr) }
    }

    override fun onSaveClick() {
        val currentState = state.value
        if (currentState.isSaveEnabled.not()) return
        viewModelScope.launch {
            addLoyaltyCardUseCase(
                LoyaltyCard(
                    name = currentState.cardName.trim(),
                    colorArgb = currentState.selectedColor.toArgb(),
                    codeText = currentState.codeText.trim(),
                    isQrFormat = currentState.isQrFormat,
                ),
            )
        }
    }

    override fun checkPermission() {
        viewModelScope.launch {
            val status = permissionRepository.checkPermission(PermissionType.CAMERA)
            state.update { it.copy(permissionStatus = status) }
        }
    }

    override fun requestPermission() {
        viewModelScope.launch {
            val status = permissionRepository.requestPermission(PermissionType.CAMERA)
            state.update { it.copy(permissionStatus = status) }
        }
    }

    override fun openSettings() {
        permissionRepository.openAppSettings()
    }

    private fun initialRequestCameraPermissionIfNecessary() {
        viewModelScope.launch {
            val cameraPermissionStatus = permissionRepository.checkPermission(PermissionType.CAMERA)
            val isNotGranted = cameraPermissionStatus != PermissionStatus.Granted
            val isNotPermanentlyDenied = cameraPermissionStatus != PermissionStatus.DeniedAlways
            state.update { it.copy(permissionStatus = cameraPermissionStatus) }
            if (isNotGranted && isNotPermanentlyDenied) {
                requestPermission()
            }
        }
    }
}

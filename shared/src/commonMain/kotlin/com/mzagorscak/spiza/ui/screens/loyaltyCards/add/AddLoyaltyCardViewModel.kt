package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.theme.LoyaltyCardColors
import kotlinx.coroutines.flow.StateFlow

data class AddLoyaltyCardState(
    val cardName: String = "",
    val selectedColor: Color = LoyaltyCardColors.defaultColor,
    val codeText: String = "",
    val isQrFormat: Boolean = true,
    val inputType: InputType = InputType.Manual,
    val permissionStatus: PermissionStatus? = null,
) {
    val isSaveEnabled: Boolean
        get() = cardName.isNotBlank() && codeText.isNotBlank()

    val presentableCard: PresentableLoyaltyCard
        get() = PresentableLoyaltyCard(
            name = cardName,
            color = selectedColor,
            codeText = codeText,
            isQrFormat = isQrFormat,
        )

    enum class InputType {
        Manual,
        Camera,
    }
}

abstract class AddLoyaltyCardViewModel : ViewModel() {
    abstract fun state(): StateFlow<AddLoyaltyCardState>

    abstract fun onCardNameChange(name: String)

    abstract fun onColorSelect(color: Color)

    abstract fun onInputTypeChange(inputType: AddLoyaltyCardState.InputType)

    abstract fun onCodeChange(code: String)

    abstract fun onFormatChange(isQr: Boolean)

    abstract fun onSaveClick()

    abstract fun checkPermission()

    abstract fun requestPermission()

    abstract fun openSettings()
}

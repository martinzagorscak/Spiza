package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.theme.LoyaltyCardColors
import kotlinx.coroutines.flow.StateFlow

/**
 * [codeText] is kept exactly as scanned (no trimming) while [scanStatus] is [ScanStatus.Scanned];
 * typed codes are trimmed before validation and saving.
 */
data class AddLoyaltyCardState(
    val cardName: String = "",
    val selectedColor: Color = LoyaltyCardColors.defaultColor,
    val codeText: String = "",
    val format: LoyaltyCardFormat = LoyaltyCardFormat.Code128,
    val isFormatSelectedManually: Boolean = false,
    val isCodeValid: Boolean = false,
    val inputType: InputType = InputType.Camera,
    val scanStatus: ScanStatus = ScanStatus.Scanning,
    val permissionStatus: PermissionStatus? = null,
) {
    val isSaveEnabled: Boolean
        get() = cardName.isNotBlank() && isCodeValid

    val showCodeError: Boolean
        get() = codeText.isNotBlank() && isCodeValid.not()

    val presentableCard: PresentableLoyaltyCard
        get() = PresentableLoyaltyCard(
            name = cardName,
            color = selectedColor,
            codeText = if (isCodeValid) codeText else "",
            format = format,
        )

    enum class InputType {
        Camera,
        Manual,
    }

    enum class ScanStatus {
        Scanning,

        /** The camera read a code the app cannot display; scanning continues. */
        Unsupported,
        Scanned,
    }
}

abstract class AddLoyaltyCardViewModel : ViewModel() {
    abstract fun state(): StateFlow<AddLoyaltyCardState>

    abstract fun onCardNameChange(name: String)

    abstract fun onColorSelect(color: Color)

    abstract fun onInputTypeChange(inputType: AddLoyaltyCardState.InputType)

    abstract fun onCodeChange(code: String)

    abstract fun onFormatChange(format: LoyaltyCardFormat)

    abstract fun onCodeScanned(code: ScannedCode)

    abstract fun onScanAgainClick()

    abstract fun onSaveClick()

    abstract fun checkPermission()

    abstract fun requestPermission()

    abstract fun openSettings()
}

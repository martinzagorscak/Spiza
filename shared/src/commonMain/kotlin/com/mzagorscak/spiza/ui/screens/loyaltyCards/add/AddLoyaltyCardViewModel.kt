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
    val isSaving: Boolean = false,
    /** One-shot: the card was stored, the screen closes. */
    val isSaved: Boolean = false,
    /** Transient: the save failed; shown once and cleared with `onErrorShown()`. */
    val hasError: Boolean = false,
) {
    val isSaveEnabled: Boolean
        get() = cardName.isNotBlank() && isCodeValid && isSaving.not() && isSaved.not()

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

        /** No code was found in an image picked from the gallery (or it could not be read). */
        NotFoundInImage,
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

    /** [code] is `null` when the picked image has no readable code. */
    abstract fun onImageCodeDecoded(code: ScannedCode?)

    abstract fun onScanAgainClick()

    abstract fun onSaveClick()

    abstract fun onErrorShown()

    abstract fun checkPermission()

    abstract fun requestPermission()

    abstract fun openSettings()
}

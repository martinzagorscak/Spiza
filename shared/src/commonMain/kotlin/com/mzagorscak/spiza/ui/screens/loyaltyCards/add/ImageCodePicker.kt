package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that opens the system photo picker and reads a code from the chosen image (e.g. a screenshot).
 * [onCodeDecoded] gets `null` when the image has no code or cannot be read, and is not called when the user cancels.
 *
 * The system pickers (Android Photo Picker, iOS PHPicker) run outside the app and hand over only the chosen image,
 * so no photo/storage permission is requested.
 */
@Composable
expect fun rememberImageCodePicker(onCodeDecoded: (ScannedCode?) -> Unit): () -> Unit

package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat

/** A code read by the camera. [format] is `null` for symbologies the app cannot display (e.g. Data Matrix). */
data class ScannedCode(
    val value: String,
    val format: LoyaltyCardFormat?,
)

/**
 * Live camera preview that reports every code it reads, exactly as encoded. Requires the camera permission.
 * The same code is reported repeatedly while it stays in view.
 */
@Composable
expect fun CodeScanner(
    onCodeScanned: (ScannedCode) -> Unit,
    modifier: Modifier = Modifier,
)

package com.mzagorscak.spiza.ui.deviceSettings

import androidx.compose.runtime.Composable

/**
 * Forces the screen brightness to [value] for as long as this composable is part of the
 * composition, restoring the previous brightness once it leaves.
 */
@Composable
expect fun KeepScreenBrightness(value: Float = 1f)

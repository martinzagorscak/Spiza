package com.mzagorscak.spiza.ui.screens.appUpdate

import androidx.compose.runtime.Composable

/**
 * Handles back while the required-update screen is shown, so back never reveals the app underneath.
 * Must be composed after NavDisplay: the most recently registered handler takes precedence.
 */
@Composable
expect fun RequiredUpdateBackHandler()

package com.mzagorscak.spiza.ui.components

import androidx.compose.runtime.Composable

/** Sets the status bar icon color; [useLightIcons] = true is for dark backgrounds behind the status bar. */
@Composable
expect fun StatusBarAppearance(useLightIcons: Boolean)

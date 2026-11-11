package com.mzagorscak.spiza.ui.clipboard

/**
 * True when the system already confirms a copy to the clipboard itself,
 * so the app should not show its own confirmation on top of it.
 */
expect val showsSystemCopyConfirmation: Boolean

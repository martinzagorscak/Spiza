package com.mzagorscak.spiza.ui.components.common

import org.jetbrains.compose.resources.DrawableResource

data class CTA(
    val label: String,
    val iconResource: DrawableResource? = null,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
)

package com.mzagorscak.spiza.ui.locale

import androidx.compose.runtime.Composable
import com.mzagorscak.spiza.domain.model.AppLanguage

/** Applies [language] to the platform locale so string resources resolve to it, recomposing content on change. */
@Composable
expect fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
)

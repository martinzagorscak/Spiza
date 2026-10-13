package com.mzagorscak.spiza.ui.locale

import androidx.compose.runtime.Composable
import com.mzagorscak.spiza.domain.model.AppLanguage
import platform.Foundation.NSUserDefaults

@Composable
actual fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(language.tag), forKey = "AppleLanguages")
    content()
}

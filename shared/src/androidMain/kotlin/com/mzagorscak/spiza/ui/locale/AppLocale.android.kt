package com.mzagorscak.spiza.ui.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mzagorscak.spiza.domain.model.AppLanguage
import java.util.Locale

@Composable
actual fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
    val context = LocalContext.current.createConfigurationContext(configuration)
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides context,
    ) {
        content()
    }
}

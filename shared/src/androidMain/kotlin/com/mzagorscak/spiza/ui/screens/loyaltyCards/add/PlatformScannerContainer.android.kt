package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mzagorscak.spiza.auth.CurrentActivityProvider
import com.mzagorscak.spiza.permissions.findComponentActivity
import org.koin.compose.koinInject

@Composable
actual fun PlatformScannerContainer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activityProvider = koinInject<CurrentActivityProvider>()
    val activity = remember(context, activityProvider) {
        context.findComponentActivity() ?: (activityProvider.current as? androidx.activity.ComponentActivity)
    }

    if (activity != null) {
        CompositionLocalProvider(LocalContext provides activity) {
            content()
        }
    } else {
        content()
    }
}

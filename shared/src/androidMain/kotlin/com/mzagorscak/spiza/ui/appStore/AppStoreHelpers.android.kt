package com.mzagorscak.spiza.ui.appStore

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.mzagorscak.spiza.crash.CrashLogger

private const val PLAY_STORE_APP_URL = "market://details?id="
private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id="

@Composable
actual fun rememberOpenAppStore(crashLogger: CrashLogger): () -> Unit {
    val context = LocalContext.current
    return remember(context, crashLogger) { { context.openAppStore(crashLogger) } }
}

private fun Context.openAppStore(crashLogger: CrashLogger) {
    // Devices without Google Play (or with it disabled) cannot handle market://, so fall back to the web page.
    if (startView(url = PLAY_STORE_APP_URL + packageName)) return
    if (startView(url = PLAY_STORE_WEB_URL + packageName)) return
    crashLogger.logWarning(message = "Opening the store failed: no app can open the Play Store link")
}

private fun Context.startView(url: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (notFound: ActivityNotFoundException) {
        false
    }

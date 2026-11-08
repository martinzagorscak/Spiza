package com.mzagorscak.spiza.ui.appStore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mzagorscak.spiza.crash.CrashLogger
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// TODO(ios-release): replace with the numeric Apple ID from App Store Connect (see docs/RELEASE.md).
private const val APP_STORE_ID = ""

private const val APP_STORE_APP_URL = "itms-apps://apps.apple.com/app/id"

// Used while APP_STORE_ID is not set, so the button still leads somewhere useful.
private const val APP_STORE_SEARCH_URL = "https://apps.apple.com/search?term=Spiza"

@Composable
actual fun rememberOpenAppStore(crashLogger: CrashLogger): () -> Unit = remember(crashLogger) { { openAppStore(crashLogger) } }

private fun openAppStore(crashLogger: CrashLogger) {
    val isAppStoreIdSet = APP_STORE_ID.isNotEmpty() && APP_STORE_ID.all(Char::isDigit)
    val urlString = if (isAppStoreIdSet) APP_STORE_APP_URL + APP_STORE_ID else APP_STORE_SEARCH_URL
    val url =
        NSURL.URLWithString(urlString) ?: run {
            crashLogger.logWarning(message = "Opening the store failed: invalid App Store URL")
            return
        }
    UIApplication.sharedApplication.openURL(
        url = url,
        options = emptyMap<Any?, Any>(),
        completionHandler = { success ->
            if (success.not()) crashLogger.logWarning(message = "Opening the store failed: the App Store URL was not opened")
        },
    )
}

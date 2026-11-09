package com.mzagorscak.spiza.ui.appStore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mzagorscak.spiza.crash.CrashLogger
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// TODO(ios-release): replace with the numeric Apple ID from App Store Connect (see docs/RELEASE.md).
private const val APP_STORE_ID = ""

// A universal link: on a device it opens the App Store app directly, on the simulator (no App Store) Safari.
private const val APP_STORE_APP_URL = "https://apps.apple.com/app/id"

@Composable
actual fun rememberOpenAppStore(crashLogger: CrashLogger): () -> Unit = remember(crashLogger) { { openAppStore(crashLogger) } }

private fun openAppStore(crashLogger: CrashLogger) {
    val isAppStoreIdSet = APP_STORE_ID.isNotEmpty() && APP_STORE_ID.all(Char::isDigit)
    if (isAppStoreIdSet.not()) {
        crashLogger.logWarning(message = "Opening the store failed: APP_STORE_ID is not set")
        return
    }
    val url =
        NSURL.URLWithString(APP_STORE_APP_URL + APP_STORE_ID) ?: run {
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

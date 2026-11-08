package com.mzagorscak.spiza.ui.appStore

import androidx.compose.runtime.Composable
import com.mzagorscak.spiza.crash.CrashLogger

/** Returns an action that opens this app's page in the platform store (Google Play / App Store). */
@Composable
expect fun rememberOpenAppStore(crashLogger: CrashLogger): () -> Unit

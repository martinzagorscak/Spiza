package com.mzagorscak.spiza

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mzagorscak.spiza.domain.usecase.SyncLoyaltyCardShortcutsUseCase
import org.koin.compose.koinInject

@Suppress("FunctionName")
fun MainViewController() =
    ComposeUIViewController {
        SyncLoyaltyCardShortcutsEffect()
        App()
    }

/** Mirrors `MainActivity`: Quick Actions are re-published only while the app is in the foreground. */
@Composable
private fun SyncLoyaltyCardShortcutsEffect() {
    val syncLoyaltyCardShortcuts = koinInject<SyncLoyaltyCardShortcutsUseCase>()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, syncLoyaltyCardShortcuts) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            syncLoyaltyCardShortcuts()
        }
    }
}

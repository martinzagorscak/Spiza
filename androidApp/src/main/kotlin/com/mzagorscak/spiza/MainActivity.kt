package com.mzagorscak.spiza

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mzagorscak.spiza.domain.usecase.SyncLoyaltyCardShortcutsUseCase
import com.mzagorscak.spiza.navigation.ShortcutNavigationRequests
import com.mzagorscak.spiza.shortcuts.AppShortcutIntents
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val syncLoyaltyCardShortcuts: SyncLoyaltyCardShortcutsUseCase by inject()
    private val shortcutNavigationRequests: ShortcutNavigationRequests by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleShortcutIntent(intent)
        }
        // Foreground only: shortcut updates from the background are rate limited, and this re-publishes after a restore.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncLoyaltyCardShortcuts()
            }
        }
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (intent?.hasExtra(AppShortcutIntents.EXTRA_LOYALTY_CARD_ID) != true) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        val cardId = intent.getLongExtra(AppShortcutIntents.EXTRA_LOYALTY_CARD_ID, INVALID_CARD_ID)
        if (cardId <= INVALID_CARD_ID) return
        shortcutNavigationRequests.openLoyaltyCard(cardId)
    }

    private companion object {
        // Room ids start at 1.
        const val INVALID_CARD_ID = 0L
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

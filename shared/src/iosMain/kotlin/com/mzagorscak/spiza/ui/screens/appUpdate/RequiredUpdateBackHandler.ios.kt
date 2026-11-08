package com.mzagorscak.spiza.ui.screens.appUpdate

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

// iOS has no system back; this only swallows the edge-swipe back gesture handled by NavDisplay.
@Composable
actual fun RequiredUpdateBackHandler() {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        onBackCompleted = {},
    )
}

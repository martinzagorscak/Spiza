package com.mzagorscak.spiza.ui.screens.appUpdate

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mzagorscak.spiza.ui.deviceSettings.findActivity

// Back leaves the app like on any root screen, but keeps the task so the user returns to the update screen.
@Composable
actual fun RequiredUpdateBackHandler() {
    val activity = LocalView.current.context.findActivity()
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        onBackCompleted = { activity?.moveTaskToBack(true) },
    )
}

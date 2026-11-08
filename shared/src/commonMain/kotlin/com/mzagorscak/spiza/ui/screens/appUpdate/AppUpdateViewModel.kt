package com.mzagorscak.spiza.ui.screens.appUpdate

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.AppUpdateStatus
import kotlinx.coroutines.flow.StateFlow

data class AppUpdateState(
    val status: AppUpdateStatus = AppUpdateStatus.UpToDate,
)

/** Checks on every app start whether the installed version must or should be updated. */
abstract class AppUpdateViewModel : ViewModel() {
    abstract fun state(): StateFlow<AppUpdateState>

    /** Re-runs the check; called each time the app comes to the foreground (including the cold start). */
    abstract fun onAppStarted()

    /** The store is opened by the caller; a recommended update is hidden for the rest of this session. */
    abstract fun onUpdateClick()

    /** "Later": hides the recommended update and does not recommend this version again. */
    abstract fun onDismissRecommended()

    /** Outside tap / back on the recommended update: hides it for the rest of this session only. */
    abstract fun onHideRecommended()
}

package com.mzagorscak.spiza.ui.screens.appUpdate

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.model.AppUpdateStatus
import com.mzagorscak.spiza.domain.usecase.DismissRecommendedUpdateUseCase
import com.mzagorscak.spiza.domain.usecase.GetAppUpdateStatusUseCase
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AppUpdateViewModelImpl(
    private val getAppUpdateStatus: GetAppUpdateStatusUseCase,
    private val dismissRecommendedUpdate: DismissRecommendedUpdateUseCase,
    private val crashLogger: CrashLogger,
) : AppUpdateViewModel() {
    private val state = MutableStateFlow(AppUpdateState())
    private var checkJob: Job? = null

    // Once the user has closed a recommendation, re-checks in the same session must not show it again.
    private var isRecommendationHidden = false

    override fun state(): StateFlow<AppUpdateState> = state.asStateFlow()

    override fun onAppStarted() {
        if (checkJob?.isActive == true) return
        checkJob =
            launchSafely(
                crashLogger = crashLogger,
                context = "Checking app update failed",
                // Fail open: a failed check must never block the user.
                onError = { state.update { it.copy(status = AppUpdateStatus.UpToDate) } },
            ) {
                val status = getAppUpdateStatus()
                state.update { it.copy(status = status.unlessHidden()) }
            }
    }

    override fun onUpdateClick() {
        // Not persisted: if the user comes back without updating, the recommendation is shown again on the next app launch.
        hideRecommendation()
    }

    override fun onDismissRecommended() {
        val recommended = state.value.status as? AppUpdateStatus.UpdateRecommended ?: return
        hideRecommendation()
        // The dialog is already closed; a failed save only means the recommendation is shown again on the next launch.
        launchSafely(crashLogger = crashLogger, context = "Dismissing recommended update failed") {
            dismissRecommendedUpdate(recommendedVersion = recommended.recommendedVersion)
        }
    }

    override fun onHideRecommended() = hideRecommendation()

    private fun hideRecommendation() {
        if (state.value.status !is AppUpdateStatus.UpdateRecommended) return
        isRecommendationHidden = true
        state.update { it.copy(status = AppUpdateStatus.UpToDate) }
    }

    private fun AppUpdateStatus.unlessHidden(): AppUpdateStatus =
        if (this is AppUpdateStatus.UpdateRecommended && isRecommendationHidden) AppUpdateStatus.UpToDate else this
}

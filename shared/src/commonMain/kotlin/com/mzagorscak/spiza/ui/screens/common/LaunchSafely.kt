package com.mzagorscak.spiza.ui.screens.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.ExpectedErrors
import com.mzagorscak.spiza.crash.recordError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches [block] in [viewModelScope] so that a failure never crashes the app. Cancellation is rethrown; any other
 * exception is logged with [recordError] under [context] and passed to [onError] (e.g. to show an error state).
 * Use this instead of `viewModelScope.launch` for every ViewModel action.
 */
internal fun ViewModel.launchSafely(
    crashLogger: CrashLogger,
    context: String,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job =
    viewModelScope.launch {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            crashLogger.recordError(context = context, error = error, expected = ExpectedErrors.writes)
            onError(error)
        }
    }

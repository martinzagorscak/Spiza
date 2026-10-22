package com.mzagorscak.spiza.crash

import kotlin.experimental.ExperimentalNativeApi

class IosCrashLogger(
    private val bridge: IosCrashLoggerBridge?,
) : CrashLogger {
    @OptIn(ExperimentalNativeApi::class)
    override fun recordException(throwable: Throwable) {
        bridge?.recordException(
            message = throwable.toString(),
            stackTrace = throwable.getStackTrace().toList(),
        )
    }

    override fun logWarning(message: String) {
        bridge?.logWarning(message)
    }
}

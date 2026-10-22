package com.mzagorscak.spiza.crash

/** Implemented in Swift (iosApp). */
interface IosCrashLoggerBridge {
    fun recordException(
        message: String,
        stackTrace: List<String>,
    )

    fun logWarning(message: String)
}

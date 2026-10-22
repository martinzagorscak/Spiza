package com.mzagorscak.spiza.crash

interface CrashLogger {
    fun recordException(throwable: Throwable)

    fun logWarning(message: String)
}

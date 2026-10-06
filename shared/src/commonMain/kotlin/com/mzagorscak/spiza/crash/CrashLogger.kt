package com.mzagorscak.spiza.crash

interface CrashLogger {
    fun recordException(throwable: Throwable)
}

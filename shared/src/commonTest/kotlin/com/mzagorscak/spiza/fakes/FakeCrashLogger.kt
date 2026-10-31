package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.crash.CrashLogger

class FakeCrashLogger : CrashLogger {
    val exceptions = mutableListOf<Throwable>()
    val warnings = mutableListOf<String>()

    override fun recordException(throwable: Throwable) {
        exceptions += throwable
    }

    override fun logWarning(message: String) {
        warnings += message
    }
}

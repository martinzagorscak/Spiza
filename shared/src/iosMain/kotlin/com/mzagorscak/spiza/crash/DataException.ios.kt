package com.mzagorscak.spiza.crash

actual fun Throwable.dataErrorReason(): DataException.Reason =
    generateSequence(this) { it.cause }
        .firstNotNullOfOrNull { (it as? DataException)?.reason }
        ?: DataException.Reason.Unknown

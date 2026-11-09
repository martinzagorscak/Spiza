package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.local.LocalDataCleaner

/** Every [clearAll] attempt is recorded **before** [clearFailure] applies. The Room implementation is tested in `databaseTest`. */
class FakeLocalDataCleaner : LocalDataCleaner {
    /** When set, [clearAll] throws it. */
    var clearFailure: Throwable? = null

    var clearAllCalls = 0

    override suspend fun clearAll() {
        clearAllCalls++
        clearFailure?.let { throw it }
    }
}

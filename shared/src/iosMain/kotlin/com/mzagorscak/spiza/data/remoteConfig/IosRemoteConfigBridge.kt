package com.mzagorscak.spiza.data.remoteConfig

import com.mzagorscak.spiza.data.BridgeError

interface IosRemoteConfigBridge {
    /** Fetches and activates the latest values; [error] is null on success. */
    fun fetchAndActivate(onComplete: (error: BridgeError?) -> Unit)

    // Each getter returns the activated value of [key] (its default if nothing was activated yet); no network call.
    fun getLong(key: String): Long

    fun getBoolean(key: String): Boolean

    fun getString(key: String): String
}

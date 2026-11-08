package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.remoteConfig.BooleanKey
import com.mzagorscak.spiza.data.remoteConfig.LongKey
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigDataSource
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigKey
import com.mzagorscak.spiza.data.remoteConfig.StringKey
import kotlinx.coroutines.awaitCancellation

/** Remote Config; a successful fetch activates the [publish]ed values like the real SDK, unset keys read their default. */
class FakeRemoteConfigDataSource : RemoteConfigDataSource {
    private val serverValues = mutableMapOf<String, Any>()
    private val activatedValues = mutableMapOf<String, Any>()

    /** When set, [fetchAndActivate] throws it. */
    var fetchFailure: Throwable? = null

    /** When set, every getter throws it. */
    var readFailure: Throwable? = null

    /** The fetch never completes, like a request that hangs without a connection. */
    var neverCompleteFetch = false

    /** Publishes [value] in the console; it is read only after the next successful fetch. */
    fun <T : Any> publish(
        key: RemoteConfigKey<T>,
        value: T,
    ) {
        serverValues[key.name] = value
    }

    /** Simulates a value activated by a fetch on an earlier app start. */
    fun <T : Any> activate(
        key: RemoteConfigKey<T>,
        value: T,
    ) {
        activatedValues[key.name] = value
    }

    override suspend fun fetchAndActivate() {
        fetchFailure?.let { throw it }
        if (neverCompleteFetch) awaitCancellation()
        activatedValues.clear()
        activatedValues.putAll(serverValues)
    }

    override suspend fun getLong(key: LongKey): Long = read(key)

    override suspend fun getBoolean(key: BooleanKey): Boolean = read(key)

    override suspend fun getString(key: StringKey): String = read(key)

    // Safe: publish and activate only store values of the key's own type.
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> read(key: RemoteConfigKey<T>): T {
        readFailure?.let { throw it }
        return activatedValues[key.name] as T? ?: key.default
    }
}

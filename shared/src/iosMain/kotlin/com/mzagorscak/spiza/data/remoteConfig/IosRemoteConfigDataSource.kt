package com.mzagorscak.spiza.data.remoteConfig

import com.mzagorscak.spiza.data.toDataException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosRemoteConfigDataSource(
    private val bridge: IosRemoteConfigBridge,
) : RemoteConfigDataSource {
    override suspend fun fetchAndActivate() =
        suspendCancellableCoroutine { continuation ->
            bridge.fetchAndActivate { error ->
                if (error != null) {
                    continuation.resumeWithException(error.toDataException())
                } else {
                    continuation.resume(Unit)
                }
            }
        }

    override suspend fun getLong(key: LongKey): Long = bridge.getLong(key.name)

    override suspend fun getBoolean(key: BooleanKey): Boolean = bridge.getBoolean(key.name)

    override suspend fun getString(key: StringKey): String = bridge.getString(key.name)
}

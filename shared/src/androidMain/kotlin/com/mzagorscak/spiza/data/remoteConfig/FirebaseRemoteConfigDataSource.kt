package com.mzagorscak.spiza.data.remoteConfig

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.mzagorscak.spiza.crash.DataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3600L

// Debuggable builds fetch on every check, so a value changed in the console shows up on the next start instead of after an hour.
private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L

class FirebaseRemoteConfigDataSource(
    isDebuggable: Boolean,
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) : RemoteConfigDataSource {
    // Settings and defaults are applied once per instance; every call awaits them so the first fetch and read already use them.
    private val setup: Task<Void> =
        Tasks.whenAll(
            remoteConfig.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings
                    .Builder()
                    .setMinimumFetchIntervalInSeconds(
                        if (isDebuggable) DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS else MINIMUM_FETCH_INTERVAL_SECONDS,
                    ).build(),
            ),
            remoteConfig.setDefaultsAsync(RemoteConfigKeys.defaults),
        )

    override suspend fun fetchAndActivate() {
        setup.await()
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (throttled: FirebaseRemoteConfigFetchThrottledException) {
            // Same as iOS: throttling is temporary and not a bug, so it is classified like being offline.
            throw DataException(reason = DataException.Reason.Offline, message = throttled.message, cause = throttled)
        }
    }

    override suspend fun getLong(key: LongKey): Long = readActivated { remoteConfig.getLong(key.name) }

    override suspend fun getBoolean(key: BooleanKey): Boolean = readActivated { remoteConfig.getBoolean(key.name) }

    override suspend fun getString(key: StringKey): String = readActivated { remoteConfig.getString(key.name) }

    // The getters may block until the activated values are loaded from disk.
    private suspend fun <T> readActivated(read: () -> T): T {
        setup.await()
        return withContext(Dispatchers.IO) { read() }
    }
}

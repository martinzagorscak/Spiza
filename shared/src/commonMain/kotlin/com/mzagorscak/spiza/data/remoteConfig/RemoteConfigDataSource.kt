package com.mzagorscak.spiza.data.remoteConfig

/**
 * Firebase Remote Config, shared by every feature that reads remote values (app update, feature flags...).
 * Keys and their defaults live in [RemoteConfigKeys]; each feature repository maps the values it needs to its domain model.
 */
interface RemoteConfigDataSource {
    /** Fetches and activates the latest values. Throws on failure (offline, throttled, SDK error). */
    suspend fun fetchAndActivate()

    // Each getter returns the activated value of the key, without a network call: the last successfully fetched value,
    // or the key's default if there was none.
    suspend fun getLong(key: LongKey): Long

    suspend fun getBoolean(key: BooleanKey): Boolean

    suspend fun getString(key: StringKey): String
}

/** A Remote Config parameter with its type and the [default] used until a value has been fetched. */
sealed class RemoteConfigKey<T : Any>(
    val name: String,
    val default: T,
)

class LongKey(
    name: String,
    default: Long,
) : RemoteConfigKey<Long>(name, default)

class BooleanKey(
    name: String,
    default: Boolean,
) : RemoteConfigKey<Boolean>(name, default)

class StringKey(
    name: String,
    default: String,
) : RemoteConfigKey<String>(name, default)

/**
 * Every Remote Config parameter the app reads. Declare keys with [long], [boolean] or [string] so they are registered
 * in [defaults] automatically: a key without a default would read the SDK's built-in one (0, false, ""), which looks like a real value.
 */
object RemoteConfigKeys {
    // Must stay above the keys: object properties are initialised top to bottom.
    private val keys = mutableListOf<RemoteConfigKey<*>>()

    // App update thresholds are encoded versions (see AppVersionProvider); 0 = not set, never triggers an update.
    // Each platform has its own keys because version numbers and release timing differ: a value set for one platform
    // must never block the other.
    val ANDROID_MIN_SUPPORTED_VERSION = long(name = "android_min_supported_version", default = 0L)
    val ANDROID_RECOMMENDED_VERSION = long(name = "android_recommended_version", default = 0L)
    val IOS_MIN_SUPPORTED_VERSION = long(name = "ios_min_supported_version", default = 0L)
    val IOS_RECOMMENDED_VERSION = long(name = "ios_recommended_version", default = 0L)

    /** Defaults of every key above, applied to the SDK before the first fetch or read. Shared with Swift as `RemoteConfigKeys.shared`. */
    val defaults: Map<String, Any> by lazy { keys.associate { it.name to it.default } }

    private fun long(
        name: String,
        default: Long,
    ) = LongKey(name = name, default = default).also(keys::add)

    private fun boolean(
        name: String,
        default: Boolean,
    ) = BooleanKey(name = name, default = default).also(keys::add)

    private fun string(
        name: String,
        default: String,
    ) = StringKey(name = name, default = default).also(keys::add)
}

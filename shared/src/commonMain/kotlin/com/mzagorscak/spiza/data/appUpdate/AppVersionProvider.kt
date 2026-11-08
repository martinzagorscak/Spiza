package com.mzagorscak.spiza.data.appUpdate

interface AppVersionProvider {
    /** Installed app version encoded as major * 10_000 + minor * 100 + patch. */
    fun currentVersion(): Long
}

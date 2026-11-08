package com.mzagorscak.spiza.data.appUpdate

import platform.Foundation.NSBundle

private const val SHORT_VERSION_KEY = "CFBundleShortVersionString"
private const val UNKNOWN_VERSION = 0L

class IosAppVersionProvider : AppVersionProvider {
    // MARKETING_VERSION (Config.xcconfig) is "major.minor.patch". If it ever can't be parsed we report 0 = unknown, and the
    // update check treats an unknown version as "never block": a malformed version string must not lock every user out.
    override fun currentVersion(): Long {
        val versionName = NSBundle.mainBundle.objectForInfoDictionaryKey(SHORT_VERSION_KEY) as? String
        return versionName?.let(::parseAppVersion) ?: UNKNOWN_VERSION
    }
}

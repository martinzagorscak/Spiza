package com.mzagorscak.spiza.data.appUpdate

private const val MAJOR_MULTIPLIER = 10_000L
private const val MINOR_MULTIPLIER = 100L
private const val MAX_MINOR_OR_PATCH = 99

private val VERSION_REGEX = Regex("""^(\d+)(?:\.(\d+))?(?:\.(\d+))?""")

/**
 * Parses a version name like "1.2.3" into major * 10_000 + minor * 100 + patch (the Android versionCode scheme).
 * A missing minor or patch is 0 and a build suffix ("1.2.3-qa", "1.2.3 (5)") is ignored.
 * Returns null when [versionName] does not start with a version, has more than three components,
 * or minor/patch is outside 0..99 (it would overlap the next component).
 */
internal fun parseAppVersion(versionName: String): Long? {
    val trimmed = versionName.trim()
    val match = VERSION_REGEX.find(trimmed) ?: return null
    val rest = trimmed.substring(match.range.last + 1)
    if (rest.startsWith(".")) return null

    val major = match.groupValues[1].toIntOrNull() ?: return null
    val minor = match.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: return null
    val patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return null
    if (minor > MAX_MINOR_OR_PATCH || patch > MAX_MINOR_OR_PATCH) return null

    return major * MAJOR_MULTIPLIER + minor * MINOR_MULTIPLIER + patch
}

package com.mzagorscak.spiza.domain.model

/**
 * Version thresholds set remotely, encoded as major * 10_000 + minor * 100 + patch.
 * A value of 0 means "not set" and never triggers an update.
 */
data class AppVersionConfig(
    val minSupportedVersion: Long,
    val recommendedVersion: Long,
)

package com.mzagorscak.spiza.data

import com.mzagorscak.spiza.crash.DataException

/**
 * An error reported by a Swift bridge. [reason] is a [DataException.Reason] name that Swift maps from the Firebase
 * error code (see `Error.toBridgeError()` in SwiftHouseholdBridge.swift), so Kotlin can classify iOS failures.
 */
data class BridgeError(
    val reason: String,
    val message: String,
)

fun BridgeError.toDataException(): DataException =
    DataException(
        reason = DataException.Reason.entries.firstOrNull { it.name == reason } ?: DataException.Reason.Unknown,
        message = message,
    )

package com.mzagorscak.spiza.fakes

import androidx.activity.ComponentActivity
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController

// Android-only: moko's Android PermissionsController adds bind(ComponentActivity), so it cannot live in commonTest.

/** Every call is recorded **before** [provideFailure] applies. */
class FakePermissionsController : PermissionsController {
    /** What [getPermissionState] returns; [isPermissionGranted] is true only for [PermissionState.Granted]. */
    var state: PermissionState = PermissionState.Granted

    /** When set, [providePermission] throws it, like moko when the user denies the system dialog. */
    var provideFailure: Exception? = null
    val checkedPermissions = mutableListOf<Permission>()
    val providedPermissions = mutableListOf<Permission>()
    var openAppSettingsCount = 0

    override suspend fun providePermission(permission: Permission) {
        providedPermissions += permission
        provideFailure?.let { throw it }
    }

    override suspend fun isPermissionGranted(permission: Permission): Boolean = state == PermissionState.Granted

    override suspend fun getPermissionState(permission: Permission): PermissionState {
        checkedPermissions += permission
        return state
    }

    override fun openAppSettings() {
        openAppSettingsCount++
    }

    override fun bind(activity: ComponentActivity) = Unit
}

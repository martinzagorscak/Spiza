package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.repository.PermissionRepository

/** Every call is recorded; [requestPermission] does not change [checkStatus], set it when a test needs the granted state to stick. */
class FakePermissionRepository(
    /** What [checkPermission] returns. */
    var checkStatus: PermissionStatus = PermissionStatus.Granted,
    /** What [requestPermission] returns, i.e. the user's answer to the system dialog. */
    var requestStatus: PermissionStatus = PermissionStatus.Granted,
) : PermissionRepository {
    val requestedPermissions = mutableListOf<PermissionType>()
    val checkedPermissions = mutableListOf<PermissionType>()
    var openAppSettingsCount = 0
        private set

    override suspend fun checkPermission(permission: PermissionType): PermissionStatus {
        checkedPermissions += permission
        return checkStatus
    }

    override suspend fun requestPermission(permission: PermissionType): PermissionStatus {
        requestedPermissions += permission
        return requestStatus
    }

    override fun openAppSettings() {
        openAppSettingsCount++
    }
}

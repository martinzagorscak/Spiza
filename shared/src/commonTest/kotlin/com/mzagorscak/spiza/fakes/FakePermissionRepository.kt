package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.repository.PermissionRepository

class FakePermissionRepository(
    var checkStatus: PermissionStatus = PermissionStatus.Granted,
    var requestStatus: PermissionStatus = PermissionStatus.Granted,
) : PermissionRepository {
    val requestedPermissions = mutableListOf<PermissionType>()

    override suspend fun checkPermission(permission: PermissionType): PermissionStatus = checkStatus

    override suspend fun requestPermission(permission: PermissionType): PermissionStatus {
        requestedPermissions += permission
        return requestStatus
    }

    override fun openAppSettings() = Unit
}

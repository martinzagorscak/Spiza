package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType

interface PermissionRepository {
    suspend fun checkPermission(permission: PermissionType): PermissionStatus

    suspend fun requestPermission(permission: PermissionType): PermissionStatus

    fun openAppSettings()
}

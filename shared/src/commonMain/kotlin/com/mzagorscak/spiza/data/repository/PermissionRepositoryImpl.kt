package com.mzagorscak.spiza.data.repository

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.repository.PermissionRepository
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.CancellationException

class PermissionRepositoryImpl(
    private val permissionsController: PermissionsController,
    private val crashLogger: CrashLogger,
) : PermissionRepository {
    override suspend fun checkPermission(permission: PermissionType): PermissionStatus {
        val mokoPermission = permission.toMokoPermission()
        val state = permissionsController.getPermissionState(mokoPermission)
        return state.toDomainStatus()
    }

    override suspend fun requestPermission(permission: PermissionType): PermissionStatus {
        val mokoPermission = permission.toMokoPermission()
        return try {
            permissionsController.providePermission(mokoPermission)
            PermissionStatus.Granted
        } catch (_: DeniedAlwaysException) {
            PermissionStatus.DeniedAlways
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            // Denied (not permanently) or dismissed: report the current state instead of failing.
            crashLogger.logWarning(message = "Permission request for $permission ended without a grant: ${e.message}")
            val state = permissionsController.getPermissionState(mokoPermission)
            state.toDomainStatus()
        }
    }

    override fun openAppSettings() {
        permissionsController.openAppSettings()
    }
}

private fun PermissionType.toMokoPermission(): Permission =
    when (this) {
        PermissionType.CAMERA -> Permission.CAMERA
    }

private fun PermissionState.toDomainStatus(): PermissionStatus =
    when (this) {
        PermissionState.Granted -> PermissionStatus.Granted
        PermissionState.Denied -> PermissionStatus.Denied
        PermissionState.DeniedAlways -> PermissionStatus.DeniedAlways
        PermissionState.NotGranted, PermissionState.NotDetermined -> PermissionStatus.NotGranted
    }

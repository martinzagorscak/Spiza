package com.mzagorscak.spiza.domain.model

sealed interface PermissionStatus {
    data object NotGranted : PermissionStatus

    data object Granted : PermissionStatus

    data object Denied : PermissionStatus

    data object DeniedAlways : PermissionStatus
}

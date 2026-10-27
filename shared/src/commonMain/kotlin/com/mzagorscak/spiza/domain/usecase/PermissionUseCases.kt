package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.domain.model.PermissionType
import com.mzagorscak.spiza.domain.repository.PermissionRepository

interface CheckPermissionUseCase {
    suspend operator fun invoke(permission: PermissionType): PermissionStatus
}

class CheckPermissionUseCaseImpl(
    private val repository: PermissionRepository,
) : CheckPermissionUseCase {
    override suspend operator fun invoke(permission: PermissionType): PermissionStatus = repository.checkPermission(permission)
}

interface RequestPermissionUseCase {
    suspend operator fun invoke(permission: PermissionType): PermissionStatus
}

class RequestPermissionUseCaseImpl(
    private val repository: PermissionRepository,
) : RequestPermissionUseCase {
    override suspend operator fun invoke(permission: PermissionType): PermissionStatus = repository.requestPermission(permission)
}

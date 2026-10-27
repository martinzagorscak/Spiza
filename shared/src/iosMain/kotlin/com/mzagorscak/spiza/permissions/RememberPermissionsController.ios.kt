package com.mzagorscak.spiza.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.ios.PermissionsController as IosPermissionsController

@Composable
actual fun rememberPlatformPermissionsController(): PermissionsController =
    remember {
        IosPermissionsController()
    }

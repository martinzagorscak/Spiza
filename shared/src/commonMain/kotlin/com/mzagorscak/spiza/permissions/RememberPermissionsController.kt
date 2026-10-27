package com.mzagorscak.spiza.permissions

import androidx.compose.runtime.Composable
import dev.icerock.moko.permissions.PermissionsController

/**
 * Custom permissions controller factory and binder.
 *
 * Why custom logic?
 * Standard moko-permissions [rememberPermissionsControllerFactory] relies on LocalContext.current
 * directly. In Compose Multiplatform navigation setups (such as Navigation3 / NavDisplay), the local
 * context can be wrapped inside a ContextWrapper or an Application context, which causes moko-permissions'
 * BindEffect to throw an IllegalStateException ("context is not instance of ComponentActivity").
 *
 * This custom implementation unwraps context wrappers and falls back to CurrentActivityProvider to
 * reliably locate the underlying ComponentActivity (`MainActivity`) for Android permission binding.
 */
@Composable
expect fun rememberPlatformPermissionsController(): PermissionsController

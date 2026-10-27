package com.mzagorscak.spiza.permissions

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mzagorscak.spiza.auth.CurrentActivityProvider
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.PermissionsControllerImpl
import org.koin.compose.koinInject

@Composable
actual fun rememberPlatformPermissionsController(): PermissionsController {
    val context = LocalContext.current
    val activityProvider = koinInject<CurrentActivityProvider>()
    val activity = remember(context, activityProvider) {
        context.findComponentActivity() ?: (activityProvider.current as? ComponentActivity)
    }

    return remember(activity, context) {
        PermissionsController(
            applicationContext = context.applicationContext,
        ).apply {
            if (this is PermissionsControllerImpl && activity != null) {
                this.bind(activity)
            }
        }
    }
}

internal fun Context.findComponentActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? ComponentActivity
}

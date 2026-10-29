package com.mzagorscak.spiza.ui.deviceSettings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
actual fun KeepScreenBrightness(value: Float) {
    val activity = LocalView.current.context.findActivity() ?: return

    DisposableEffect(activity, value) {
        val window = activity.window
        val originalBrightness = window.attributes.screenBrightness

        window.attributes = window.attributes.apply {
            screenBrightness = value.coerceIn(minimumValue = 0f, maximumValue = 1f)
        }

        onDispose {
            window.attributes = window.attributes.apply {
                screenBrightness = originalBrightness
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

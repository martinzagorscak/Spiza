package com.mzagorscak.spiza.ui.deviceSettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIScreen

@Composable
actual fun KeepScreenBrightness(value: Float) {
    DisposableEffect(value) {
        val originalBrightness = UIScreen.mainScreen.brightness

        UIScreen.mainScreen.brightness = value.coerceIn(minimumValue = 0f, maximumValue = 1f).toDouble()

        onDispose {
            UIScreen.mainScreen.brightness = originalBrightness
        }
    }
}

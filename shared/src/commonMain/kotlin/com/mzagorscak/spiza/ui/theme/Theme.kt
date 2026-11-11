package com.mzagorscak.spiza.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Palette: deep blues + sand/beige tones.
private val NightBlue = Color(0xFF0E1C2B)
private val DeepBlue = Color(0xFF13293D)
private val SlateBlue = Color(0xFF24405A)
private val SkyBlue = Color(0xFF7FB8D6)
private val Ivory = Color(0xFFF6F0E6)
private val Beige = Color(0xFFEBDCCB)
private val Sand = Color(0xFFD2B79B)
private val WarmBeige = Color(0xFFC8A98A)
private val DarkBrown = Color(0xFF3A2A1A)
private val Taupe = Color(0xFF6B5A4A)
private val Error = Color(0xFFB00020)
private val ErrorLight = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)

/*
 * Cheat sheet (every "onX" is the content color to use ON TOP of "X"):
 * - Screens / list rows:      background    -> text/icons: onBackground
 * - Cards, dialogs, menus:    surface       -> text/icons: onSurface (secondary text: onSurfaceVariant)
 * - Main buttons, top bar:    primary       -> text/icons: onPrimary
 * - Chips, avatars, tonal:    primaryContainer -> text/icons: onPrimaryContainer
 * - Dividers / borders:       outlineVariant / outline
 * onBackground and onSurface are intentionally the same color.
 */
private val LightColorScheme =
    lightColorScheme(
        primary = DeepBlue,
        onPrimary = Ivory,
        primaryContainer = Beige,
        onPrimaryContainer = DeepBlue,
        secondary = WarmBeige,
        onSecondary = DarkBrown,
        secondaryContainer = Sand,
        onSecondaryContainer = DarkBrown,
        tertiary = SlateBlue,
        onTertiary = Ivory,
        tertiaryContainer = Beige,
        onTertiaryContainer = DeepBlue,
        background = Ivory,
        onBackground = DeepBlue,
        surface = Beige,
        onSurface = DeepBlue,
        surfaceVariant = Sand,
        onSurfaceVariant = Taupe,
        outline = WarmBeige,
        outlineVariant = Sand,
        inversePrimary = SkyBlue,
        error = Error,
        onError = Ivory,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Sand,
        onPrimary = DarkBrown,
        primaryContainer = SlateBlue,
        onPrimaryContainer = Beige,
        secondary = SkyBlue,
        onSecondary = NightBlue,
        secondaryContainer = SlateBlue,
        onSecondaryContainer = Beige,
        tertiary = WarmBeige,
        onTertiary = DarkBrown,
        tertiaryContainer = SlateBlue,
        onTertiaryContainer = Beige,
        background = NightBlue,
        onBackground = Beige,
        surface = DeepBlue,
        onSurface = Beige,
        surfaceVariant = SlateBlue,
        onSurfaceVariant = Sand,
        outline = Taupe,
        outlineVariant = SlateBlue,
        inversePrimary = DeepBlue,
        error = ErrorLight,
        onError = OnErrorDark,
    )

private const val THEME_TRANSITION_DURATION_MS = 400

@Composable
fun TheAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Blends the two palettes instead of swapping them, so every themed color fades together when the theme changes.
    val darkness by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = THEME_TRANSITION_DURATION_MS),
        label = "themeDarkness",
    )
    val colorScheme =
        when (darkness) {
            0f -> LightColorScheme
            1f -> DarkColorScheme
            else -> lerp(start = LightColorScheme, stop = DarkColorScheme, fraction = darkness)
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

// Every color of the scheme, including the ones Material derives itself, so none of them jumps mid-transition.
private fun lerp(
    start: ColorScheme,
    stop: ColorScheme,
    fraction: Float,
): ColorScheme =
    start.copy(
        primary = lerp(start = start.primary, stop = stop.primary, fraction = fraction),
        onPrimary = lerp(start = start.onPrimary, stop = stop.onPrimary, fraction = fraction),
        primaryContainer = lerp(start = start.primaryContainer, stop = stop.primaryContainer, fraction = fraction),
        onPrimaryContainer = lerp(start = start.onPrimaryContainer, stop = stop.onPrimaryContainer, fraction = fraction),
        inversePrimary = lerp(start = start.inversePrimary, stop = stop.inversePrimary, fraction = fraction),
        secondary = lerp(start = start.secondary, stop = stop.secondary, fraction = fraction),
        onSecondary = lerp(start = start.onSecondary, stop = stop.onSecondary, fraction = fraction),
        secondaryContainer = lerp(start = start.secondaryContainer, stop = stop.secondaryContainer, fraction = fraction),
        onSecondaryContainer = lerp(start = start.onSecondaryContainer, stop = stop.onSecondaryContainer, fraction = fraction),
        tertiary = lerp(start = start.tertiary, stop = stop.tertiary, fraction = fraction),
        onTertiary = lerp(start = start.onTertiary, stop = stop.onTertiary, fraction = fraction),
        tertiaryContainer = lerp(start = start.tertiaryContainer, stop = stop.tertiaryContainer, fraction = fraction),
        onTertiaryContainer = lerp(start = start.onTertiaryContainer, stop = stop.onTertiaryContainer, fraction = fraction),
        background = lerp(start = start.background, stop = stop.background, fraction = fraction),
        onBackground = lerp(start = start.onBackground, stop = stop.onBackground, fraction = fraction),
        surface = lerp(start = start.surface, stop = stop.surface, fraction = fraction),
        onSurface = lerp(start = start.onSurface, stop = stop.onSurface, fraction = fraction),
        surfaceVariant = lerp(start = start.surfaceVariant, stop = stop.surfaceVariant, fraction = fraction),
        onSurfaceVariant = lerp(start = start.onSurfaceVariant, stop = stop.onSurfaceVariant, fraction = fraction),
        surfaceTint = lerp(start = start.surfaceTint, stop = stop.surfaceTint, fraction = fraction),
        inverseSurface = lerp(start = start.inverseSurface, stop = stop.inverseSurface, fraction = fraction),
        inverseOnSurface = lerp(start = start.inverseOnSurface, stop = stop.inverseOnSurface, fraction = fraction),
        error = lerp(start = start.error, stop = stop.error, fraction = fraction),
        onError = lerp(start = start.onError, stop = stop.onError, fraction = fraction),
        errorContainer = lerp(start = start.errorContainer, stop = stop.errorContainer, fraction = fraction),
        onErrorContainer = lerp(start = start.onErrorContainer, stop = stop.onErrorContainer, fraction = fraction),
        outline = lerp(start = start.outline, stop = stop.outline, fraction = fraction),
        outlineVariant = lerp(start = start.outlineVariant, stop = stop.outlineVariant, fraction = fraction),
        scrim = lerp(start = start.scrim, stop = stop.scrim, fraction = fraction),
        surfaceBright = lerp(start = start.surfaceBright, stop = stop.surfaceBright, fraction = fraction),
        surfaceDim = lerp(start = start.surfaceDim, stop = stop.surfaceDim, fraction = fraction),
        surfaceContainer = lerp(start = start.surfaceContainer, stop = stop.surfaceContainer, fraction = fraction),
        surfaceContainerHigh = lerp(start = start.surfaceContainerHigh, stop = stop.surfaceContainerHigh, fraction = fraction),
        surfaceContainerHighest = lerp(start = start.surfaceContainerHighest, stop = stop.surfaceContainerHighest, fraction = fraction),
        surfaceContainerLow = lerp(start = start.surfaceContainerLow, stop = stop.surfaceContainerLow, fraction = fraction),
        surfaceContainerLowest = lerp(start = start.surfaceContainerLowest, stop = stop.surfaceContainerLowest, fraction = fraction),
        primaryFixed = lerp(start = start.primaryFixed, stop = stop.primaryFixed, fraction = fraction),
        primaryFixedDim = lerp(start = start.primaryFixedDim, stop = stop.primaryFixedDim, fraction = fraction),
        onPrimaryFixed = lerp(start = start.onPrimaryFixed, stop = stop.onPrimaryFixed, fraction = fraction),
        onPrimaryFixedVariant = lerp(start = start.onPrimaryFixedVariant, stop = stop.onPrimaryFixedVariant, fraction = fraction),
        secondaryFixed = lerp(start = start.secondaryFixed, stop = stop.secondaryFixed, fraction = fraction),
        secondaryFixedDim = lerp(start = start.secondaryFixedDim, stop = stop.secondaryFixedDim, fraction = fraction),
        onSecondaryFixed = lerp(start = start.onSecondaryFixed, stop = stop.onSecondaryFixed, fraction = fraction),
        onSecondaryFixedVariant = lerp(start = start.onSecondaryFixedVariant, stop = stop.onSecondaryFixedVariant, fraction = fraction),
        tertiaryFixed = lerp(start = start.tertiaryFixed, stop = stop.tertiaryFixed, fraction = fraction),
        tertiaryFixedDim = lerp(start = start.tertiaryFixedDim, stop = stop.tertiaryFixedDim, fraction = fraction),
        onTertiaryFixed = lerp(start = start.onTertiaryFixed, stop = stop.onTertiaryFixed, fraction = fraction),
        onTertiaryFixedVariant = lerp(start = start.onTertiaryFixedVariant, stop = stop.onTertiaryFixedVariant, fraction = fraction),
    )

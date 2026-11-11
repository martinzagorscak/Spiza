package com.mzagorscak.spiza.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun TheAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

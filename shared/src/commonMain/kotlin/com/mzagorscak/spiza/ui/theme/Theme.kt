package com.mzagorscak.spiza.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkyBlue = Color(0xFF7FB8D6)
private val DeepBlue = Color(0xFF13293D)
private val OceanBlue = Color(0xFF247BA0)
private val Mist = Color(0xFFE8F1F2)
private val Sand = Color(0xFFD2B79B)
private val DarkBrown = Color(0xFF3A2A1A)
private val Brown = Color(0xFF5A4430)
private val LightSand = Color(0xFFEFE0CF)
private val WarmBeige = Color(0xFFC8A98A)
private val DarkWood = Color(0xFF332213)
private val DarkBeige = Color(0xFF4E3B2A)
private val Ivory = Color(0xFFEFEFE5)
private val LightBrown = Color(0xFFA67C5B)
private val Beige = Color(0xFFEBDCCB)
private val BeigeAccent = Color(0xFFC4A484)
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Error = Color(0xFFB00020)

private val DarkColorScheme =
    darkColorScheme(
        primary = SkyBlue,
        onPrimary = DeepBlue,
        primaryContainer = OceanBlue,
        onPrimaryContainer = Mist,
        inversePrimary = OceanBlue,
        secondary = Sand,
        onSecondary = DarkBrown,
        secondaryContainer = Beige,
        onSecondaryContainer = DarkBrown,
        tertiary = WarmBeige,
        onTertiary = DarkWood,
        tertiaryContainer = DarkBeige,
        onTertiaryContainer = Ivory,
        background = DeepBlue,
        onBackground = Ivory,
        surface = Black,
        onSurface = White,
        error = Error,
        onError = White,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = OceanBlue,
        onPrimary = White,
        primaryContainer = Mist,
        onPrimaryContainer = DeepBlue,
        inversePrimary = SkyBlue,
        secondary = LightBrown,
        onSecondary = White,
        secondaryContainer = Brown,
        onSecondaryContainer = LightSand,
        tertiary = BeigeAccent,
        onTertiary = DeepBlue,
        tertiaryContainer = Ivory,
        onTertiaryContainer = DarkWood,
        background = Ivory,
        onBackground = DeepBlue,
        surface = White,
        onSurface = Black,
        error = Error,
        onError = White,
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

package com.mzagorscak.spiza.ui.components.animatable.animatedicons

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding050
import com.mzagorscak.spiza.ui.theme.padding200

private const val ANIMATION_DURATION_MS = 500

// The gear has six teeth, so any multiple of 60 degrees ends on a pose identical to the resting one.
private const val DEGREES_PER_TURN = 120f
private const val GEAR_CENTER_X = 12.5f
private const val GEAR_CENTER_Y = 12f

private val GearPath =
    addPathNodes(
        "M14.279,2.152C13.908,2 13.439,2 12.5,2C11.561,2 11.092,2 10.721,2.152C10.227,2.355 9.835,2.745 9.631,3.235" +
            "C9.537,3.458 9.501,3.718 9.486,4.098C9.465,4.656 9.177,5.172 8.69,5.451C8.203,5.73 7.609,5.72 7.111,5.459" +
            "C6.773,5.281 6.528,5.183 6.286,5.151C5.756,5.082 5.22,5.224 4.796,5.547C4.478,5.789 4.243,6.193 3.774,7" +
            "C3.304,7.807 3.07,8.21 3.017,8.605C2.948,9.131 3.091,9.663 3.417,10.083C3.565,10.276 3.774,10.437 4.098,10.639" +
            "C4.574,10.936 4.88,11.442 4.88,12C4.88,12.558 4.574,13.064 4.098,13.361C3.774,13.563 3.565,13.724 3.416,13.917" +
            "C3.091,14.337 2.947,14.869 3.017,15.395C3.07,15.789 3.304,16.193 3.774,17C4.243,17.807 4.478,18.211 4.796,18.453" +
            "C5.22,18.776 5.756,18.918 6.286,18.849C6.528,18.817 6.773,18.719 7.111,18.541C7.609,18.28 8.203,18.27 8.69,18.549" +
            "C9.177,18.828 9.465,19.344 9.486,19.902C9.501,20.281 9.537,20.542 9.631,20.765C9.835,21.255 10.227,21.645 10.721,21.848" +
            "C11.092,22 11.561,22 12.5,22C13.439,22 13.908,22 14.279,21.848C14.773,21.645 15.165,21.255 15.369,20.765" +
            "C15.463,20.542 15.499,20.281 15.514,19.902C15.535,19.344 15.823,18.828 16.31,18.549C16.797,18.27 17.391,18.28 17.889,18.541" +
            "C18.227,18.719 18.472,18.817 18.714,18.849C19.244,18.918 19.78,18.776 20.204,18.453C20.522,18.211 20.757,17.807 21.226,17" +
            "C21.696,16.193 21.93,15.789 21.983,15.395" +
            "C22.052,14.869 21.909,14.337 21.583,13.916C21.435,13.724 21.226,13.563 20.902,13.361" +
            "C20.426,13.064 20.12,12.558 20.12,12C20.12,11.442 20.426,10.936 20.902,10.639C21.226,10.437 21.435,10.276 21.584,10.083" +
            "C21.909,9.663 22.052,9.131 21.983,8.605C21.93,8.211 21.696,7.807 21.226,7C20.757,6.193 20.522,5.789 20.204,5.547" +
            "C19.78,5.224 19.244,5.082 18.714,5.151C18.472,5.183 18.227,5.281 17.889,5.459C17.392,5.72 16.797,5.73 16.31,5.451" +
            "C15.823,5.172 15.535,4.656 15.514,4.098C15.499,3.718 15.463,3.458 15.369,3.235C15.165,2.745 14.773,2.355 14.279,2.152Z" +
            "M12.5,15C14.17,15 15.523,13.657 15.523,12C15.523,10.343 14.17,9 12.5,9C10.83,9 9.477,10.343 9.477,12" +
            "C9.477,13.657 10.83,15 12.5,15Z",
    )

/** A settings gear that turns once more every time [turns] grows; count the clicks into it. */
@Composable
fun rememberAnimatedSettingsPainter(turns: Int): Painter =
    rememberAnimatedIconPainter(
        targetProgress = turns.toFloat(),
        label = "settingsTurns",
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
    ) { progress ->
        Group(rotation = DEGREES_PER_TURN * progress, pivotX = GEAR_CENTER_X, pivotY = GEAR_CENTER_Y) {
            Path(pathData = GearPath, pathFillType = PathFillType.EvenOdd, fill = AnimatedIconFill)
        }
    }

@Composable
private fun AnimatedSettingsIconLayoutPreview() {
    var turns by remember { mutableIntStateOf(0) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedSettingsPainter(turns = turns),
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = { turns++ },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AnimatedSettingsIconPreviewLightMode() {
    TheAppTheme {
        AnimatedSettingsIconLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnimatedSettingsIconPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AnimatedSettingsIconLayoutPreview()
    }
}

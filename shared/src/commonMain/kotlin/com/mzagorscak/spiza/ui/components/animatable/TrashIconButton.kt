package com.mzagorscak.spiza.ui.components.animatable

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.animatable.animatedicons.rememberAnimatedTrashPainter
import com.mzagorscak.spiza.ui.theme.TheAppTheme

/** Delete button; the trash can lid opens on every click as feedback while the confirmation shows up. */
@Composable
fun TrashIconButton(
    onClick: () -> Unit,
    tintColor: Color,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    var clicks by remember { mutableIntStateOf(0) }
    IconButton(
        painter = rememberAnimatedTrashPainter(clicks = clicks),
        onClick = {
            clicks++
            onClick()
        },
        isEnabled = isEnabled,
        appearance = IconButtonAppearance(
            variant = IconButtonVariant.IconBased,
            size = IconButtonSize.Medium,
            tintColor = tintColor,
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TrashIconButtonPreviewLightMode() {
    TheAppTheme {
        TrashIconButton(onClick = {}, tintColor = MaterialTheme.colorScheme.onBackground)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TrashIconButtonPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        TrashIconButton(onClick = {}, tintColor = MaterialTheme.colorScheme.onBackground)
    }
}

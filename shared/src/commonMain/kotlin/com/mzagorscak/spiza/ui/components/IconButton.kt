package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding050
import com.mzagorscak.spiza.ui.theme.padding200
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_google

enum class IconButtonSize { Small, Medium, Large, }

enum class IconButtonVariant { IconBased, ImageBased }

data class IconButtonAppearance(
    val variant: IconButtonVariant,
    val size: IconButtonSize,
) {
    companion object {
        val Default = IconButtonAppearance(
            variant = IconButtonVariant.ImageBased,
            size = IconButtonSize.Medium,
        )
    }

    @Composable
    fun getSizeInDp(): Dp =
        when (size) {
            IconButtonSize.Small -> 32.dp
            IconButtonSize.Medium -> 40.dp
            IconButtonSize.Large -> 48.dp
        }
}

@Composable
fun IconButton(
    resource: DrawableResource,
    appearance: IconButtonAppearance = IconButtonAppearance.Default,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
            ),
    ) {
        when (appearance.variant) {
            IconButtonVariant.IconBased -> {
                Icon(
                    painter = painterResource(resource),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(appearance.getSizeInDp())
                        .padding(padding200),
                )
            }

            IconButtonVariant.ImageBased -> {
                Image(
                    painter = painterResource(resource),
                    contentDescription = null,
                    modifier = Modifier
                        .size(appearance.getSizeInDp())
                        .padding(padding200),
                )
            }
        }
    }
}

@Composable
private fun IconButtonLayoutPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding050),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(padding200),
    ) {
        IconButton(
            resource = Res.drawable.ic_back,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Small,
            ),
            onClick = {},
        )
        IconButton(
            resource = Res.drawable.ic_back,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Medium,
            ),
            onClick = {},
        )
        IconButton(
            resource = Res.drawable.ic_back,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Large,
            ),
            onClick = {},
        )
        IconButton(
            resource = Res.drawable.ic_google,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.ImageBased,
                size = IconButtonSize.Small,
            ),
            onClick = {},
        )
        IconButton(
            resource = Res.drawable.ic_google,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.ImageBased,
                size = IconButtonSize.Medium,
            ),
            onClick = {},
        )
        IconButton(
            resource = Res.drawable.ic_google,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.ImageBased,
                size = IconButtonSize.Large,
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun IconButtonPreviewLightMode() {
    TheAppTheme {
        IconButtonLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun IconButtonPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        IconButtonLayoutPreview()
    }
}

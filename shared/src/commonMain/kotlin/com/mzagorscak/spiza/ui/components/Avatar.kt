package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import org.jetbrains.compose.resources.painterResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_profile_placeholder

enum class AvatarSize(
    val inDp: Dp,
) {
    Small(32.dp),
    Medium(52.dp),
    Large(72.dp),
    ExtraLarge(102.dp),
}

@Composable
fun Avatar(
    photoUrl: String?,
    size: AvatarSize = AvatarSize.Medium,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(color = if (photoUrl != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer)
            .size(size.inDp),
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(Res.drawable.ic_profile_placeholder),
                error = painterResource(Res.drawable.ic_profile_placeholder),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_profile_placeholder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxSize().padding(padding100).clip(CircleShape),
            )
        }
    }
}

@Composable
private fun AvatarLayoutPreview() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(padding200),
    ) {
        Avatar(photoUrl = null, size = AvatarSize.Small)
        Avatar(photoUrl = null, size = AvatarSize.Medium)
        Avatar(photoUrl = null, size = AvatarSize.Large)
        Avatar(photoUrl = null, size = AvatarSize.ExtraLarge)
        Avatar(
            photoUrl = "https://t3.ftcdn.net/jpg/02/99/04/20/360_F_299042079_vGBD7wIlSeNl7vOevWHiL93G4koMM967.jpg",
            size = AvatarSize.Small,
        )
        Avatar(
            photoUrl = "https://t3.ftcdn.net/jpg/02/99/04/20/360_F_299042079_vGBD7wIlSeNl7vOevWHiL93G4koMM967.jpg",
            size = AvatarSize.Medium,
        )
        Avatar(
            photoUrl = "https://t3.ftcdn.net/jpg/02/99/04/20/360_F_299042079_vGBD7wIlSeNl7vOevWHiL93G4koMM967.jpg",
            size = AvatarSize.Large,
        )
        Avatar(
            photoUrl = "https://t3.ftcdn.net/jpg/02/99/04/20/360_F_299042079_vGBD7wIlSeNl7vOevWHiL93G4koMM967.jpg",
            size = AvatarSize.ExtraLarge,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AvatarPreviewLightMode() {
    TheAppTheme {
        AvatarLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AvatarPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        AvatarLayoutPreview()
    }
}

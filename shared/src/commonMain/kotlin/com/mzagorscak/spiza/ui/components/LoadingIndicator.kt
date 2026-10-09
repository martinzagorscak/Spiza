package com.mzagorscak.spiza.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.theme.TheAppTheme

enum class LoadingIndicatorSize(
    val sizeInDp: Dp,
) {
    Small(sizeInDp = 24.dp),
    Medium(sizeInDp = 48.dp),
    Large(sizeInDp = 72.dp),
}

@Composable
fun LoadingIndicator(
    size: LoadingIndicatorSize = LoadingIndicatorSize.Medium,
    modifier: Modifier = Modifier,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.sizeInDp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LoadingIndicatorPreviewLightMode() {
    TheAppTheme {
        LoadingIndicator()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LoadingIndicatorPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        LoadingIndicator()
    }
}

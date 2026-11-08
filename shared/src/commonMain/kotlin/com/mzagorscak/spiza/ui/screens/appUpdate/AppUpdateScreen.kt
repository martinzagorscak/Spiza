package com.mzagorscak.spiza.ui.screens.appUpdate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.domain.model.AppUpdateStatus
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.app_update_recommended_description
import spiza.shared.generated.resources.app_update_recommended_later
import spiza.shared.generated.resources.app_update_recommended_title
import spiza.shared.generated.resources.app_update_required_description
import spiza.shared.generated.resources.app_update_required_title
import spiza.shared.generated.resources.app_update_update_button

/**
 * Shown above the whole app (not a destination): a required update covers every screen and back cannot reveal it,
 * a recommended update is a dismissible dialog. Draws nothing when the app is up to date.
 */
@Composable
fun AppUpdateScreen(
    state: AppUpdateState,
    callbacks: AppUpdateScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    when (state.status) {
        AppUpdateStatus.UpToDate -> Unit

        is AppUpdateStatus.UpdateRecommended ->
            Dialog(
                title = stringResource(Res.string.app_update_recommended_title),
                description = stringResource(Res.string.app_update_recommended_description),
                dismissCTA = CTA(
                    label = stringResource(Res.string.app_update_recommended_later),
                    onClick = callbacks.onDismissRecommended,
                ),
                confirmCTA = CTA(
                    label = stringResource(Res.string.app_update_update_button),
                    onClick = callbacks.onUpdateClick,
                ),
                onDismissRequest = callbacks.onHideRecommended,
            )

        AppUpdateStatus.UpdateRequired ->
            RequiredUpdateContent(
                onUpdateClick = callbacks.onUpdateClick,
                modifier = modifier,
            )
    }
}

@Composable
private fun RequiredUpdateContent(
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RequiredUpdateBackHandler()

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
    }

    // Surface consumes touches, so nothing underneath can be used.
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.safeDrawingPadding(),
        ) {
            FallbackState(
                title = stringResource(Res.string.app_update_required_title),
                description = stringResource(Res.string.app_update_required_description),
                cta = CTA(
                    label = stringResource(Res.string.app_update_update_button),
                    onClick = onUpdateClick,
                ),
            )
        }
    }
}

data class AppUpdateScreenCallbacks(
    val onUpdateClick: () -> Unit,
    val onDismissRecommended: () -> Unit,
    val onHideRecommended: () -> Unit,
)

@Preview
@Composable
private fun RequiredUpdatePreviewLightMode() {
    TheAppTheme {
        RequiredUpdateContent(onUpdateClick = {})
    }
}

@Preview
@Composable
private fun RequiredUpdatePreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        RequiredUpdateContent(onUpdateClick = {})
    }
}

@Composable
private fun RecommendedUpdatePreview() {
    AppUpdateScreen(
        state = AppUpdateState(status = AppUpdateStatus.UpdateRecommended(recommendedVersion = 10_100)),
        callbacks = AppUpdateScreenCallbacks(onUpdateClick = {}, onDismissRecommended = {}, onHideRecommended = {}),
    )
}

@Preview
@Composable
private fun RecommendedUpdatePreviewLightMode() {
    TheAppTheme {
        RecommendedUpdatePreview()
    }
}

@Preview
@Composable
private fun RecommendedUpdatePreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        RecommendedUpdatePreview()
    }
}

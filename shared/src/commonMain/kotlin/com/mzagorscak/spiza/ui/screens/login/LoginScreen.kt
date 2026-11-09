package com.mzagorscak.spiza.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.LoadingIndicator
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.padding600
import com.mzagorscak.spiza.ui.theme.padding800
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_app_title
import spiza.shared.generated.resources.ic_google
import spiza.shared.generated.resources.local_farm_stand
import spiza.shared.generated.resources.login_app_title
import spiza.shared.generated.resources.login_error
import spiza.shared.generated.resources.login_sign_in_with_google
import spiza.shared.generated.resources.login_tagline

private val AppTitleWidth = 220.dp
private val LoginButtonIconSize = 20.dp
private val LoginButtonHeight = 56.dp
private const val LIGHT_SCRIM_ALPHA = 0.75f
private const val DARK_SCRIM_ALPHA = 0.9f
private const val TAGLINE_ALPHA = 0.85f

// Google sign-in branding: a white button with dark text, independent of the app theme.
private val GoogleButtonContainerColor = Color.White
private val GoogleButtonContentColor = Color(0xFF1F1F1F)

@Composable
fun LoginScreen(
    state: LoginState,
    callbacks: LoginCallbacks,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val loginErrorMessage = stringResource(Res.string.login_error)
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbarHostState.showSnackbar(loginErrorMessage)
            callbacks.onLoginErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues),
        ) {
            Image(
                painter = painterResource(Res.drawable.local_farm_stand),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            val scrimAlpha = if (isSystemInDarkTheme()) DARK_SCRIM_ALPHA else LIGHT_SCRIM_ALPHA
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = scrimAlpha * 0.3f),
                            0.45f to Color.Black.copy(alpha = scrimAlpha * 0.6f),
                            1f to Color.Black.copy(alpha = scrimAlpha),
                        ),
                    ),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = padding600)
                    .padding(bottom = padding800),
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(Res.drawable.ic_app_title),
                    contentDescription = stringResource(Res.string.login_app_title),
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.width(AppTitleWidth),
                )

                Text(
                    text = stringResource(Res.string.login_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = TAGLINE_ALPHA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = padding400),
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LoginButtonHeight),
                ) {
                    if (state.isLoading) {
                        LoadingIndicator()
                    } else {
                        Button(
                            label = stringResource(Res.string.login_sign_in_with_google),
                            onClick = callbacks.onSignInWithGoogle,
                            appearance = ButtonAppearance(
                                variant = ButtonVariant.Primary,
                                size = ButtonSize.Large,
                                containerColor = GoogleButtonContainerColor,
                                contentColor = GoogleButtonContentColor,
                            ),
                            leadingContent = {
                                Image(
                                    painter = painterResource(Res.drawable.ic_google),
                                    contentDescription = null,
                                    modifier = Modifier.size(LoginButtonIconSize),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = LoginButtonHeight),
                        )
                    }
                }
            }
        }
    }
}

data class LoginCallbacks(
    val onSignInWithGoogle: () -> Unit,
    val onLoginErrorShown: () -> Unit,
)

@Preview
@Composable
private fun LoginScreenPreview() {
    TheAppTheme {
        LoginScreen(
            state = LoginState(),
            callbacks = LoginCallbacks(
                onSignInWithGoogle = {},
                onLoginErrorShown = {},
            ),
        )
    }
}

@Preview
@Composable
private fun LoginScreenDarkPreview() {
    TheAppTheme(darkTheme = true) {
        LoginScreen(
            state = LoginState(),
            callbacks = LoginCallbacks(
                onSignInWithGoogle = {},
                onLoginErrorShown = {},
            ),
        )
    }
}

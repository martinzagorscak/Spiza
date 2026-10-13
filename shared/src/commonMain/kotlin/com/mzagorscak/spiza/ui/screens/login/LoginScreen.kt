package com.mzagorscak.spiza.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_google
import spiza.shared.generated.resources.local_farm_stand
import spiza.shared.generated.resources.login_sign_in_with_google

private val LoginButtonIconSize = 20.dp

@Composable
fun LoginScreen(
    state: LoginState,
    callbacks: LoginCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.consumeWindowInsets(paddingValues),
        ) {
            Image(
                painter = painterResource(Res.drawable.local_farm_stand),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isSystemInDarkTheme()) 0.7f else 0.5f)
                    .background(Color.Black),
            )

            Button(
                label = stringResource(Res.string.login_sign_in_with_google),
                onClick = callbacks.onSignInWithGoogle,
                isEnabled = !state.isLoading,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                ),
                leadingContent = {
                    Image(
                        painter = painterResource(Res.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(LoginButtonIconSize),
                    )
                },
            )
        }
    }
}

data class LoginCallbacks(
    val onSignInWithGoogle: () -> Unit = {},
)

@Preview
@Composable
private fun LoginScreenPreview() {
    TheAppTheme {
        LoginScreen(
            state = LoginState(),
            callbacks = LoginCallbacks(),
        )
    }
}

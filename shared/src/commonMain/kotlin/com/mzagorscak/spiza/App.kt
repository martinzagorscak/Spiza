package com.mzagorscak.spiza

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_google

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val authService = koinInject<AuthService>()
    val authState by authService.authState.collectAsState(initial = null)
    TheAppTheme {
        Scaffold(
            topBar = {
                TopBar(
                    title = "Spiza",
                    leadingContent = {
                        IconButton(
                            resource = Res.drawable.ic_back,
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                            ),
                            onClick = {},
                        )
                    },
                )
            },
        ) { paddingValues ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.onPrimary)
                        .fillMaxSize(),
            ) {
                if (authState != null) {
                    Button(
                        label = "Sign Out",
                        leadingContent = @Composable {
                            Image(
                                painter = painterResource(Res.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        trailingContent = @Composable {
                            Image(
                                painter = painterResource(Res.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                authService.signOut()
                            }
                        },
                    )
                } else {
                    Button(
                        label = "Sign In with Google",
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                authService.signIn(provider = AuthProvider.Google)
                            }
                        },
                        leadingContent = {
                            Image(
                                painter = painterResource(Res.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        trailingContent = {
                            Image(
                                painter = painterResource(Res.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

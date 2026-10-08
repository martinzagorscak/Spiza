package com.mzagorscak.spiza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val authService = koinInject<AuthService>()
    val authState by authService.authState.collectAsState(initial = null)
    TheAppTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
        ) {
            if (authState != null) {
                Text(text = "Hello, ${authState?.displayName ?: "User"}!")
                Text(text = "${authState?.email}")
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            authService.signOut()
                        }
                    },
                ) {
                    Text(text = "Sign Out")
                }
            } else {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            authService.signIn(provider = AuthProvider.Google)
                        }
                    },
                ) {
                    Text(text = "Sign In with Google")
                }
            }
        }
    }
}

package com.mzagorscak.spiza.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import org.jetbrains.compose.resources.painterResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_google

@Composable
fun LoginScreen(
    state: LoginState,
    onSignInWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding200, Alignment.CenterVertically),
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.onPrimary)
                .fillMaxSize()
                .padding(padding200),
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
        Button(
            label = "Sign In with Google",
            onClick = onSignInWithGoogle,
            isEnabled = !state.isLoading,
            leadingContent = {
                Image(
                    painter = painterResource(Res.drawable.ic_google),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    TheAppTheme {
        LoginScreen(state = LoginState(), onSignInWithGoogle = {})
    }
}

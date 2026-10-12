package com.mzagorscak.spiza.ui.screens.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200

@Composable
fun LandingScreen(
    state: LandingState,
    onSignOut: () -> Unit,
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
        Text(text = "Landing", style = MaterialTheme.typography.headlineMedium)
        state.displayName?.let { Text(text = "Welcome, $it") }
        Button(label = "Sign Out", onClick = onSignOut)
    }
}

@Preview
@Composable
private fun LandingScreenPreview() {
    TheAppTheme {
        LandingScreen(state = LandingState(displayName = "Martin"), onSignOut = {})
    }
}

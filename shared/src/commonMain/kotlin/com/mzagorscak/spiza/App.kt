package com.mzagorscak.spiza

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.navigation.AppNavigation
import com.mzagorscak.spiza.ui.theme.TheAppTheme

@Composable
@Preview
fun App() {
    TheAppTheme {
        AppNavigation()
    }
}

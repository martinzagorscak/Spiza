package com.mzagorscak.spiza.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.ui.components.Avatar
import com.mzagorscak.spiza.ui.components.AvatarSize
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.ListingItem
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.model.PresentableUserProfile
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.error_generic
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_refresh
import spiza.shared.generated.resources.ic_right_chevron
import spiza.shared.generated.resources.settings_copy_key
import spiza.shared.generated.resources.settings_generate_key
import spiza.shared.generated.resources.settings_household_management
import spiza.shared.generated.resources.settings_key_copied
import spiza.shared.generated.resources.settings_language_croatian
import spiza.shared.generated.resources.settings_language_english
import spiza.shared.generated.resources.settings_loyalty_cards
import spiza.shared.generated.resources.settings_section_appearance
import spiza.shared.generated.resources.settings_section_general
import spiza.shared.generated.resources.settings_section_language
import spiza.shared.generated.resources.settings_section_profile
import spiza.shared.generated.resources.settings_shopping_items_saving
import spiza.shared.generated.resources.settings_sign_out
import spiza.shared.generated.resources.settings_theme_dark
import spiza.shared.generated.resources.settings_theme_light
import spiza.shared.generated.resources.settings_theme_system
import spiza.shared.generated.resources.settings_title
import spiza.shared.generated.resources.settings_user_key_hint
import spiza.shared.generated.resources.settings_user_key_label

@Composable
fun SettingsScreen(
    state: SettingsState,
    callbacks: SettingsScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(Res.string.error_generic)
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbarHostState.showSnackbar(errorMessage)
            callbacks.onErrorShown()
        }
    }
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.settings_title),
                leadingContent = {
                    IconButton(
                        resource = Res.drawable.ic_back,
                        onClick = callbacks.onBackClick,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(padding400),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(padding400),
        ) {
            Section(stringResource(Res.string.settings_section_profile)) {
                ProfileCard(
                    profile = state.profile,
                    showHouseholdJoinKeySection = state.showHouseholdJoinKeySection,
                    snackbarHostState = snackbarHostState,
                    onCreateJoinKeyClick = callbacks.onCreateJoinKeyClick,
                    onRefreshJoinKeyClick = callbacks.onRefreshJoinKeyClick,
                )
            }
            Section(stringResource(Res.string.settings_section_appearance)) {
                ThemeMode.entries.forEach { mode ->
                    OptionItem(
                        label = stringResource(mode.labelRes()),
                        selected = state.themeMode == mode,
                        onClick = { callbacks.onThemeModeSelected(mode) },
                    )
                }
            }
            Section(stringResource(Res.string.settings_section_language)) {
                AppLanguage.entries.forEach { language ->
                    OptionItem(
                        label = stringResource(language.labelRes()),
                        selected = state.language == language,
                        onClick = { callbacks.onLanguageSelected(language) },
                    )
                }
            }
            Section(stringResource(Res.string.settings_section_general)) {
                ListingItem(
                    label = stringResource(Res.string.settings_household_management),
                    onClick = callbacks.onHouseholdManagementClick,
                    trailingContent = {
                        IconButton(
                            resource = Res.drawable.ic_right_chevron,
                            onClick = callbacks.onHouseholdManagementClick,
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                                tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    },
                    showDivider = true,
                )
                ListingItem(
                    label = stringResource(Res.string.settings_shopping_items_saving),
                    onClick = callbacks.onShoppingItemsSavingClick,
                    trailingContent = {
                        IconButton(
                            resource = Res.drawable.ic_right_chevron,
                            onClick = callbacks.onShoppingItemsSavingClick,
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                                tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    },
                    showDivider = true,
                )
                ListingItem(
                    label = stringResource(Res.string.settings_loyalty_cards),
                    onClick = callbacks.onLoyaltyCardsClick,
                    trailingContent = {
                        IconButton(
                            resource = Res.drawable.ic_right_chevron,
                            onClick = callbacks.onLoyaltyCardsClick,
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                                tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    },
                )
            }
            Button(
                label = stringResource(Res.string.settings_sign_out),
                onClick = callbacks.onSignOutClick,
                isEnabled = !state.isSigningOut,
                appearance = ButtonAppearance(variant = ButtonVariant.Secondary, size = ButtonSize.Large),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: PresentableUserProfile,
    showHouseholdJoinKeySection: Boolean,
    snackbarHostState: SnackbarHostState,
    onCreateJoinKeyClick: () -> Unit,
    onRefreshJoinKeyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(Res.string.settings_key_copied)

    Column(verticalArrangement = Arrangement.spacedBy(padding200), modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(padding200)) {
            Avatar(photoUrl = profile.photoUrl, size = AvatarSize.Large)
            Column {
                profile.name?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                profile.email?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (showHouseholdJoinKeySection) {
            HouseholdJoinKeySection(
                profile = profile,
                onCreateJoinKeyClick = onCreateJoinKeyClick,
                onRefreshJoinKeyClick = onRefreshJoinKeyClick,
                onCopyJoinKeyClick = { joinKey ->
                    scope.launch {
                        clipboard.setText(AnnotatedString(joinKey))
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HouseholdJoinKeySection(
    profile: PresentableUserProfile,
    onCreateJoinKeyClick: () -> Unit,
    onRefreshJoinKeyClick: () -> Unit,
    onCopyJoinKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(Res.string.settings_user_key_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            resource = Res.drawable.ic_refresh,
            onClick = onRefreshJoinKeyClick,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Medium,
                tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
    if (profile.joinKey != null) {
        Text(
            text = profile.joinKey,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(Res.string.settings_user_key_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            label = stringResource(Res.string.settings_copy_key),
            onClick = { onCopyJoinKeyClick(profile.joinKey) },
            appearance = ButtonAppearance(variant = ButtonVariant.Tertiary, size = ButtonSize.Small),
        )
    } else {
        Button(
            label = stringResource(Res.string.settings_generate_key),
            onClick = onCreateJoinKeyClick,
            appearance = ButtonAppearance(variant = ButtonVariant.Secondary, size = ButtonSize.Medium),
        )
    }
}

@Composable
private fun OptionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListingItem(
        label = label,
        onClick = onClick,
        trailingContent = { RadioButton(selected = selected, onClick = onClick) },
    )
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
    }
}

private fun ThemeMode.labelRes() =
    when (this) {
        ThemeMode.System -> Res.string.settings_theme_system
        ThemeMode.Light -> Res.string.settings_theme_light
        ThemeMode.Dark -> Res.string.settings_theme_dark
    }

private fun AppLanguage.labelRes() =
    when (this) {
        AppLanguage.English -> Res.string.settings_language_english
        AppLanguage.Croatian -> Res.string.settings_language_croatian
    }

data class SettingsScreenCallbacks(
    val onBackClick: () -> Unit,
    val onCreateJoinKeyClick: () -> Unit,
    val onRefreshJoinKeyClick: () -> Unit,
    val onThemeModeSelected: (ThemeMode) -> Unit,
    val onLanguageSelected: (AppLanguage) -> Unit,
    val onHouseholdManagementClick: () -> Unit,
    val onShoppingItemsSavingClick: () -> Unit,
    val onLoyaltyCardsClick: () -> Unit,
    val onSignOutClick: () -> Unit,
    val onErrorShown: () -> Unit,
)

@Preview
@Composable
private fun SettingsScreenPreview() {
    TheAppTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsState(
                profile = PresentableUserProfile("Jane Doe", "jane@example.com", null, "abc123"),
            ),
            callbacks = SettingsScreenCallbacks(
                onBackClick = {},
                onCreateJoinKeyClick = {},
                onRefreshJoinKeyClick = {},
                onThemeModeSelected = {},
                onLanguageSelected = {},
                onHouseholdManagementClick = {},
                onShoppingItemsSavingClick = {},
                onLoyaltyCardsClick = {},
                onSignOutClick = {},
                onErrorShown = {},
            ),
        )
    }
}

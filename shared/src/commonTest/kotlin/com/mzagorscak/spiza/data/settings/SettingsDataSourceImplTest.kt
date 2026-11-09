package com.mzagorscak.spiza.data.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.fakes.FakeSettings
import kotlin.test.Test

class SettingsDataSourceImplTest {
    private val storage = FakeSettings()

    @Test
    fun init_exposesDefaults_whenNothingIsStored() {
        val dataSource = SettingsDataSourceImpl(settings = storage)

        assertThat(dataSource.themeMode.value).isEqualTo(ThemeMode.System)
        assertThat(dataSource.language.value).isEqualTo(AppLanguage.Default)
        assertThat(dataSource.selectedHouseholdId.value).isNull()
        assertThat(dataSource.suggestionsSavingEnabled.value).isFalse()
        assertThat(dataSource.dismissedRecommendedVersion.value).isNull()
        assertThat(dataSource.onboardingCompleted.value).isFalse()
    }

    @Test
    fun setters_updateTheirFlowsImmediately() {
        val dataSource = SettingsDataSourceImpl(settings = storage)

        dataSource.setThemeMode(ThemeMode.Dark)
        dataSource.setLanguage(AppLanguage.Croatian)
        dataSource.setSelectedHouseholdId("household-2")
        dataSource.setSuggestionsSavingEnabled(true)
        dataSource.setDismissedRecommendedVersion(10_203)
        dataSource.setOnboardingCompleted()

        assertThat(dataSource.themeMode.value).isEqualTo(ThemeMode.Dark)
        assertThat(dataSource.currentThemeMode).isEqualTo(ThemeMode.Dark)
        assertThat(dataSource.language.value).isEqualTo(AppLanguage.Croatian)
        assertThat(dataSource.currentLanguage).isEqualTo(AppLanguage.Croatian)
        assertThat(dataSource.selectedHouseholdId.value).isEqualTo("household-2")
        assertThat(dataSource.suggestionsSavingEnabled.value).isTrue()
        assertThat(dataSource.dismissedRecommendedVersion.value).isEqualTo(10_203L)
        assertThat(dataSource.onboardingCompleted.value).isTrue()
    }

    @Test
    fun setters_persistValues_soTheNextAppStartRestoresThem() {
        SettingsDataSourceImpl(settings = storage).apply {
            setThemeMode(ThemeMode.Light)
            setLanguage(AppLanguage.Croatian)
            setSelectedHouseholdId("household-2")
            setSuggestionsSavingEnabled(true)
            setDismissedRecommendedVersion(10_203)
            setOnboardingCompleted()
        }

        val restarted = SettingsDataSourceImpl(settings = storage)

        assertThat(restarted.currentThemeMode).isEqualTo(ThemeMode.Light)
        assertThat(restarted.currentLanguage).isEqualTo(AppLanguage.Croatian)
        assertThat(restarted.selectedHouseholdId.value).isEqualTo("household-2")
        assertThat(restarted.suggestionsSavingEnabled.value).isTrue()
        assertThat(restarted.dismissedRecommendedVersion.value).isEqualTo(10_203L)
        assertThat(restarted.onboardingCompleted.value).isTrue()
    }

    @Test
    fun clear_resetsTheUsersPreferencesToTheirDefaults_inMemoryAndInStorage() {
        val dataSource =
            SettingsDataSourceImpl(settings = storage).apply {
                setThemeMode(ThemeMode.Dark)
                setLanguage(AppLanguage.Croatian)
                setSelectedHouseholdId("household-2")
                setSuggestionsSavingEnabled(true)
                setDismissedRecommendedVersion(10_203)
                setOnboardingCompleted()
            }

        dataSource.clear()

        val restarted = SettingsDataSourceImpl(settings = storage)
        listOf(dataSource, restarted).forEach {
            assertThat(it.themeMode.value).isEqualTo(ThemeMode.System)
            assertThat(it.language.value).isEqualTo(AppLanguage.Default)
            assertThat(it.selectedHouseholdId.value).isNull()
            assertThat(it.suggestionsSavingEnabled.value).isFalse()
            assertThat(it.onboardingCompleted.value).isFalse()
        }
    }

    // The dismissed recommended update describes the installed app on this device, not the user's data.
    @Test
    fun clear_keepsTheDismissedRecommendedVersion() {
        val dataSource = SettingsDataSourceImpl(settings = storage).apply { setDismissedRecommendedVersion(10_203) }

        dataSource.clear()

        assertThat(dataSource.dismissedRecommendedVersion.value).isEqualTo(10_203L)
        assertThat(SettingsDataSourceImpl(settings = storage).dismissedRecommendedVersion.value).isEqualTo(10_203L)
    }

    // On iOS the store is the app's shared NSUserDefaults, which also holds SDK entries.
    @Test
    fun clear_keepsKeysThatAreNotPreferencesOfTheApp() {
        storage.putString("sdk_key", "value")

        SettingsDataSourceImpl(settings = storage).clear()

        assertThat(storage.values).isEqualTo(mapOf<String, Any>("sdk_key" to "value"))
    }

    // region Error handling

    @Test
    fun init_fallsBackToSystemTheme_whenTheStoredThemeIsUnknown() {
        storage.putString("theme_mode", "Sepia")

        assertThat(SettingsDataSourceImpl(settings = storage).currentThemeMode).isEqualTo(ThemeMode.System)
    }

    // endregion
}

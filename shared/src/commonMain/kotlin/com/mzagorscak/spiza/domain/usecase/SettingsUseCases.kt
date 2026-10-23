package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(): Flow<UserProfile?> = repository.profile
}

class GetCurrentUserProfileUseCase(
    private val repository: UserRepository,
) {
    operator fun invoke(): UserProfile? = repository.currentProfile
}

class GetUsersUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userIds: List<String>): List<UserProfile> = repository.getUsers(userIds)
}

class ObserveThemeModeUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

class GetCurrentThemeModeUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): ThemeMode = repository.currentThemeMode
}

class SetThemeModeUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class ObserveAppLanguageUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppLanguage> = repository.language
}

class GetCurrentAppLanguageUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): AppLanguage = repository.currentLanguage
}

class SetAppLanguageUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

class ObserveSuggestionsSavingEnabledUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.suggestionsSavingEnabled
}

class SetSuggestionsSavingEnabledUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSuggestionsSavingEnabled(enabled)
}

class SignInUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(provider: AuthProvider): AuthResult = repository.signIn(provider)
}

class SignOutUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke() = repository.signOut()
}

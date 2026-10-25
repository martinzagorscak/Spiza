package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.auth.AuthProvider
import com.mzagorscak.spiza.auth.AuthResult
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import com.mzagorscak.spiza.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

interface ObserveUserProfileUseCase {
    operator fun invoke(): Flow<UserProfile?>
}

class ObserveUserProfileUseCaseImpl(
    private val repository: UserRepository,
) : ObserveUserProfileUseCase {
    override operator fun invoke(): Flow<UserProfile?> = repository.profile
}

interface GetCurrentUserProfileUseCase {
    operator fun invoke(): UserProfile?
}

class GetCurrentUserProfileUseCaseImpl(
    private val repository: UserRepository,
) : GetCurrentUserProfileUseCase {
    override operator fun invoke(): UserProfile? = repository.currentProfile
}

interface GetUsersUseCase {
    suspend operator fun invoke(userIds: List<String>): List<UserProfile>
}

class GetUsersUseCaseImpl(
    private val repository: UserRepository,
) : GetUsersUseCase {
    override suspend operator fun invoke(userIds: List<String>): List<UserProfile> = repository.getUsers(userIds)
}

interface ObserveThemeModeUseCase {
    operator fun invoke(): Flow<ThemeMode>
}

class ObserveThemeModeUseCaseImpl(
    private val repository: SettingsRepository,
) : ObserveThemeModeUseCase {
    override operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

interface GetCurrentThemeModeUseCase {
    operator fun invoke(): ThemeMode
}

class GetCurrentThemeModeUseCaseImpl(
    private val repository: SettingsRepository,
) : GetCurrentThemeModeUseCase {
    override operator fun invoke(): ThemeMode = repository.currentThemeMode
}

interface SetThemeModeUseCase {
    suspend operator fun invoke(mode: ThemeMode)
}

class SetThemeModeUseCaseImpl(
    private val repository: SettingsRepository,
) : SetThemeModeUseCase {
    override suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

interface ObserveAppLanguageUseCase {
    operator fun invoke(): Flow<AppLanguage>
}

class ObserveAppLanguageUseCaseImpl(
    private val repository: SettingsRepository,
) : ObserveAppLanguageUseCase {
    override operator fun invoke(): Flow<AppLanguage> = repository.language
}

interface GetCurrentAppLanguageUseCase {
    operator fun invoke(): AppLanguage
}

class GetCurrentAppLanguageUseCaseImpl(
    private val repository: SettingsRepository,
) : GetCurrentAppLanguageUseCase {
    override operator fun invoke(): AppLanguage = repository.currentLanguage
}

interface SetAppLanguageUseCase {
    suspend operator fun invoke(language: AppLanguage)
}

class SetAppLanguageUseCaseImpl(
    private val repository: SettingsRepository,
) : SetAppLanguageUseCase {
    override suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

interface ObserveSuggestionsSavingEnabledUseCase {
    operator fun invoke(): Flow<Boolean>
}

class ObserveSuggestionsSavingEnabledUseCaseImpl(
    private val repository: SettingsRepository,
) : ObserveSuggestionsSavingEnabledUseCase {
    override operator fun invoke(): Flow<Boolean> = repository.suggestionsSavingEnabled
}

interface SetSuggestionsSavingEnabledUseCase {
    suspend operator fun invoke(enabled: Boolean)
}

class SetSuggestionsSavingEnabledUseCaseImpl(
    private val repository: SettingsRepository,
) : SetSuggestionsSavingEnabledUseCase {
    override suspend operator fun invoke(enabled: Boolean) = repository.setSuggestionsSavingEnabled(enabled)
}

interface SignInUseCase {
    suspend operator fun invoke(provider: AuthProvider): AuthResult
}

class SignInUseCaseImpl(
    private val repository: UserRepository,
) : SignInUseCase {
    override suspend operator fun invoke(provider: AuthProvider): AuthResult = repository.signIn(provider)
}

interface SignOutUseCase {
    suspend operator fun invoke()
}

class SignOutUseCaseImpl(
    private val repository: UserRepository,
) : SignOutUseCase {
    override suspend operator fun invoke() = repository.signOut()
}

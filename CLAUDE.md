# Spiza — Claude project guide

Spiza is a shared shopping-list app (households, shopping items, suggestions, loyalty cards) built with
**Kotlin Multiplatform + Compose Multiplatform** for Android and iOS. Almost all code (including UI) lives in `:shared`.

Talk to the user in **Croatian**; write code, comments, commit messages and string keys in **English**.

## Commands

| What | Command |
|---|---|
| Unit tests (fast, JVM) | `./gradlew :shared:testAndroidHostTest` |
| Unit tests (iOS simulator) | `./gradlew :shared:iosSimulatorArm64Test` |
| Lint check | `./gradlew ktlintCheck` |
| Lint autofix | `./gradlew ktlintFormat` |
| Android debug build | `./gradlew :androidApp:assembleDebug` |
| iOS app | open `iosApp/` in Xcode (cannot be run from CLI here) |

Definition of done for any code change: **ktlintCheck passes, testAndroidHostTest passes, assembleDebug compiles.**
If you touched `iosMain` or a bridge, also run `:shared:iosSimulatorArm64Test` (it compiles the iOS targets).

## Module map

```
androidApp/            Android entry point only (MainActivity, SpizaApp, Firebase config, resources)
iosApp/iosApp/         iOS entry point + Swift*Bridge.swift (Firebase Auth/Firestore/Crashlytics live in Swift)
shared/src/
  commonMain/kotlin/com/mzagorscak/spiza/
    auth/              AuthService interface
    crash/             CrashLogger interface (Crashlytics on both platforms)
    data/<feature>/    RepositoryImpl, DataSource interfaces, local (Room DAO), model (DTO/Entity + mappers)
    data/di/           dataModule (Koin)
    domain/model/      pure domain models (no Compose, no Firebase, no Room)
    domain/repository/ repository interfaces
    domain/usecase/    <Feature>UseCases.kt — interface + Impl pairs
    domain/constraints ConstraintsManager (business limits)
    domain/di/         domainModule (Koin)
    navigation/        Route (sealed, @Serializable) + AppNavigation (NavDisplay, callbacks wiring)
    ui/components/     reusable design-system composables
    ui/model/          Presentable* UI models + toPresentable() mappers
    ui/screens/<feat>/ <X>Screen.kt, <X>ViewModel.kt (state + abstract VM), <X>ViewModelImpl.kt
    ui/screens/di/     viewModelModule (Koin)
    ui/theme/          colors, typography, spacing tokens (padding100..padding800), shapes
    platformDI/        appModule, initKoin, expect platformModule
  commonMain/composeResources/  values/strings.xml (EN) + values-hr/strings.xml (HR), drawables, lottie files
  androidMain/         Firestore*/Android* implementations, actuals
  iosMain/             Ios*DataSource (wraps Swift bridges), Ios*Bridge interfaces, actuals
  commonTest/          shared unit tests (kotlin-test)
```

## Architecture in one paragraph

`Screen (stateless composable)` ← `state: StateFlow` + `Callbacks` data class ← `ViewModelImpl` → `UseCase` → `Repository` (interface in
domain, Impl in data) → `DataSource` (interface in commonMain, platform impls in androidMain/iosMain, or Room DAO).
Dependencies point inward only: `ui → domain ← data`. Everything is wired with Koin in `DataModule`, `DomainModule`, `ViewModelModule`.
Navigation is wired in `AppNavigation.kt` — that is where `koinViewModel()`, `collectAsStateWithLifecycle()` and the screen's callbacks
(with haptic feedback) are created.

Detailed rules live in `.claude/rules/` and are loaded automatically. Read them before writing code in an area you have not touched yet.

## Firestore data & security rules

- `firestore.rules` (repo root) is the **source of truth** for the security rules deployed to Firebase; `firebase.json` points to it.
  The user deploys it by pasting it into Firebase console > Firestore > Rules (or `firebase deploy --only firestore:rules`).
- Collections: `households/{id}` (+ `items/{id}` subcollection), `users/{uid}`, `joinKeys/{joinKey}` (→ `{ userId }`, the key is the document id). Field names live in `HouseholdFields` / `UserFields` and are shared with Swift.
- Any change to collections, fields, document shape or who may write them **must update `firestore.rules` in the same change**, keep Android and Swift
  identical, and end with a note to the user that the rules need to be deployed (old app versions keep using the deployed rules).
- Rules reject unknown fields (`keys().hasOnly`) — adding a field without updating the rules makes every write fail with permission denied.

## Workflows (skills)

- `/new-feature` — implement a feature end-to-end through all layers
- `/add-screen` — new screen + route + ViewModel + DI + strings
- `/write-unit-tests` — tests for use cases, repositories, ViewModels, mappers
- `/fix-bug` — reproduce → failing test → minimal fix → verify
- `/add-error-handling` — harden a flow (data → repo → VM → UI error state)
- `/room-change` — Room entity/DAO/schema change, safely
- `/commit` — verify and commit in the project's `[Tag] Area - description` format

## Agents

`feature-implementer`, `test-writer`, `bug-fixer`, `error-handling-specialist`, `code-reviewer`, `kmp-platform-specialist`
(see `.claude/agents/`). Use `code-reviewer` on every non-trivial diff before committing.

## Hard rules

- Never commit or push unless the user asks. Never force-push. Never rewrite history on `main`.
- Never edit `google-services.json`, `GoogleService-Info.plist`, `local.properties`, keystores, or `.idea/`.
- Do not add or bump dependencies without asking; when approved, add them through `gradle/libs.versions.toml` only.
- Every user-visible string goes into **both** `values/strings.xml` and `values-hr/strings.xml`.
- Every new `Route` must be registered in `routeSerializersModule` (Route.kt).
- Every new UseCase/Repository/ViewModel must be registered in the matching Koin module.
- Firestore schema or permission changes ⇒ update `firestore.rules` (see above).
- Room schema is `version = 1` with `exportSchema = false`: any entity change needs a version bump **and** a migration — see `/room-change`.
- Keep changes minimal and in the existing style; do not refactor unrelated code in the same change.

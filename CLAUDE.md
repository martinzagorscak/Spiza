# Spiza — Claude project guide

Spiza is a shared shopping-list app (households, shopping items, suggestions, loyalty cards) built with
**Kotlin Multiplatform + Compose Multiplatform** for Android and iOS. Almost all code (including UI) lives in `:shared`.

Talk to the user in **Croatian**; write code, comments, commit messages and string keys in **English**.

## Commands

| What | Command |
|---|---|
| Unit tests (fast, JVM) | `./gradlew :shared:testAndroidHostTest` |
| Unit tests (iOS simulator, incl. Room database tests) | `./gradlew :shared:iosSimulatorArm64Test` |
| Test coverage report | `./gradlew :shared:koverHtmlReport` → `shared/build/reports/kover/html/index.html` |
| Lint check | `./gradlew ktlintCheck` |
| Lint autofix | `./gradlew ktlintFormat` |
| Android debug build | `./gradlew :androidApp:assembleDebug` |
| iOS app | open `iosApp/` in Xcode (cannot be run from CLI here) |

Definition of done for any code change: **ktlintCheck passes, testAndroidHostTest passes, assembleDebug compiles.**
If you touched `iosMain`, a bridge, Room, `iosTest` or `databaseTest`, also run `:shared:iosSimulatorArm64Test` (it compiles the iOS targets and runs the Room database and migration tests).
If you touched `firestore.rules`, also run `cd firestore-tests && npm test`. Before a release, run the Android device tests too (`:shared:connectedAndroidDeviceTest`).

## Module map

```
androidApp/            Android entry point only (MainActivity, SpizaApp, Firebase config, resources)
iosApp/iosApp/         iOS entry point + Swift*Bridge.swift (Firebase Auth/Firestore/Crashlytics live in Swift)
shared/src/
  commonMain/kotlin/com/mzagorscak/spiza/
    auth/              AuthService interface
    crash/             CrashLogger interface (Crashlytics on both platforms)
    data/<feature>/    RepositoryImpl, DataSource interfaces, local (Room DAO), model (DTO/Entity + mappers)
    data/local/        SpizaDatabase (shared Room DB for all features), builder, migrations
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
  commonTest/          shared unit tests + fakes/ (AssertK, Turbine) — run on JVM and iOS
  databaseTest/        Room DAO/repository + migration tests on a real database — run on iOS and Android device
  androidHostTest/     Android-only JVM tests;  androidDeviceTest/ / iosTest/  platform actuals for databaseTest
shared/schemas/        exported Room schemas (committed, never regenerated)
firestore-tests/       Firestore security rules tests (Node + emulator)
```

## Architecture in one paragraph

`Screen (stateless composable)` ← `state: StateFlow` + `Callbacks` data class ← `ViewModelImpl` → `UseCase` → `Repository` (interface in
domain, Impl in data) → `DataSource` (interface in commonMain, platform impls in androidMain/iosMain, or Room DAO).
Dependencies point inward only: `ui → domain ← data`. Everything is wired with Koin in `DataModule`, `DomainModule`, `ViewModelModule`.
Navigation is wired in `AppNavigation.kt` — that is where `koinViewModel()`, `collectAsStateWithLifecycle()` and the screen's callbacks
(with haptic feedback) are created.

Testing handbook for humans (levels, structure, commands, recipes): `docs/TESTING.md`.
Release handbook and to-dos per platform (Android / iOS checklists, release runbook, forcing app updates via Remote Config): `docs/RELEASE.md`.

Detailed rules live in `.claude/rules/` and are loaded automatically. Read them before writing code in an area you have not touched yet.

## Firestore data & security rules

- `firestore.rules` (repo root) is the **source of truth** for the security rules deployed to Firebase; `firebase.json` points to it.
  The user deploys it by pasting it into Firebase console > Firestore > Rules (or `firebase deploy --only firestore:rules`).
- Collections: `households/{id}` (+ `items/{id}` subcollection), `users/{uid}`, `joinKeys/{joinKey}` (→ `{ userId }`, the key is the document id). Field names live in `HouseholdFields` / `UserFields` and are shared with Swift.
- Any change to collections, fields, document shape or who may write them **must update `firestore.rules` in the same change**, keep Android and Swift
  identical, and end with a note to the user that the rules need to be deployed (old app versions keep using the deployed rules).
- Rules reject unknown fields (`keys().hasOnly`) — adding a field without updating the rules makes every write fail with permission denied.
- Every rules change is covered by the emulator tests in `firestore-tests/` (`cd firestore-tests && npm test`) — see `/firestore-rules-tests`.

## Workflows (skills)

- `/new-feature` — implement a feature end-to-end through all layers
- `/add-screen` — new screen + route + ViewModel + DI + strings
- `/write-unit-tests` — tests for use cases, repositories, ViewModels, mappers
- `/firestore-rules-tests` — security rules tests against the Firestore emulator
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
- Room schema (`data/local/SpizaDatabase.kt`) is `version = 3` with `exportSchema = true` (schemas committed in `shared/schemas/`): any entity change needs a version bump, a migration in `SpizaDatabaseMigrations.kt` **and** a migration test in `databaseTest` — see `/room-change`.
- Keep changes minimal and in the existing style; do not refactor unrelated code in the same change.

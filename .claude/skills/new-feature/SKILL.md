---
name: new-feature
description: Implement a new Spiza feature or enhancement end-to-end (domain → data → DI → ViewModel → screen → navigation → strings → tests). Use when the user asks to add, build or implement functionality.
---

# New feature workflow

Input: `$ARGUMENTS` (feature description). If the behaviour is ambiguous (data stored locally in Room or shared via Firestore? which screen? limits?), ask 1–3 concrete questions first.

## 1. Plan (show it to the user before coding if the feature touches more than ~5 files)
- Closest existing template: loyalty cards (Room CRUD), shopping list (Firestore streams + error/retry), settings (preferences via `SettingsRepository`).
- Storage: Room (`SpizaDatabase` → follow `/room-change`), Firestore (Android `Firestore*DataSource` + iOS bridge — use the `kmp-platform-specialist` agent), or `Settings` (multiplatform-settings).
- List: domain models, repository methods, use cases, state fields + intents, route, strings (EN + HR), tests.

## 2. Implement bottom-up (keep it compiling)
1. `domain/model/<X>.kt`, `domain/repository/<X>Repository.kt`
2. `domain/usecase/<Feature>UseCases.kt` — interface + `Impl` pairs; business rules live here
3. `data/<feature>/...` — model + mappers, DataSource/DAO, `internal class <X>RepositoryImpl`
4. Koin: `data/di/DataModule.kt`, `domain/di/DomainModule.kt` (under a `// <Feature> Use Cases` comment), platform modules if needed
5. `ui/model/Presentable<X>.kt` + `toPresentable()`
6. `/add-screen` steps for new screens, or extend an existing `<X>ViewModel` (abstract API) + `<X>ViewModelImpl` + `ViewModelModule`
7. Error handling per `.claude/rules/error-handling.md` — in the same change
8. Strings in `values/strings.xml` and `values-hr/strings.xml`

## 3. Tests
Use the `/write-unit-tests` workflow for every new use case and any non-trivial ViewModel/repository logic.

## 4. Verify
```bash
./gradlew ktlintFormat && ./gradlew ktlintCheck
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
# if iosMain/Swift touched:
./gradlew :shared:iosSimulatorArm64Test
```
Then run the `code-reviewer` agent on the diff and address blocking / should-fix items.

## 5. Hand-off
Summarise per layer what changed, test results, anything to verify manually (Xcode, Firestore rules, UI on device) and propose a commit message: `[Feature] <Area> - <description>`. Do not commit unless asked.

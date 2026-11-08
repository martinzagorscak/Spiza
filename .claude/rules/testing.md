---
paths:
  - "shared/src/commonTest/**"
  - "shared/src/androidHostTest/**"
  - "shared/src/iosTest/**"
  - "shared/src/databaseTest/**"
  - "shared/src/androidDeviceTest/**"
---
# Testing rules

How to write a test step by step: `/write-unit-tests` (`.claude/skills/write-unit-tests/`). Copy-paste templates per layer:
`templates.md`; how to write and maintain fakes: `fakes.md` (both in that skill folder).

## 1. Tools

| Tool | Use for | Notes |
|---|---|---|
| `kotlin.test` | `@Test`, `@BeforeTest`, `@AfterTest`, `@Ignore` | Annotations only — assertions come from AssertK. No JUnit-only APIs in commonTest. |
| **AssertK** (`assertk.assertThat`) | every assertion | `isEqualTo`, `containsExactly`, `isEmpty`, `isTrue`, `isNull`, `hasSize`, `prop(X::field)`, `assertFailure { }.isInstanceOf<T>()`. Readable failure messages. |
| `kotlinx-coroutines-test` | `runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle`, `backgroundScope` | virtual time — timeouts and delays are instant |
| **Turbine** (`app.cash.turbine.test`) | asserting what a `Flow` **emits** (sequence of values, completion, failure) | see "Flows and state" below |
| Hand-written **fakes** (`commonTest/.../fakes/`) | replacing repositories, Firestore data sources, platform objects | no mocking library — see `fakes.md` |
| **In-memory Room** (`expect fun inMemorySpizaDatabase()` in `databaseTest/.../data/local/`, actuals in `iosTest` / `androidDeviceTest`) | DAO SQL and Room-backed repositories | real `SpizaDatabase` with `BundledSQLiteDriver`, like production |
| **Kover** | `./gradlew :shared:koverHtmlReport` → `shared/build/reports/kover/html/index.html` | finds code no test runs; measured on Android host tests only, so the Room-backed classes (tested in `databaseTest`) are excluded in `shared/build.gradle.kts`. Not a target number. |
| **Firestore emulator** + `@firebase/rules-unit-testing` + Vitest | `firestore.rules` | separate Node project in `firestore-tests/` — see `/firestore-rules-tests` |

Why fakes and not a mocking library: fakes assert *outcomes* (the item is on the list) instead of *calls* (method X was called),
so refactors don't break tests; one fake serves every test; MockK doesn't run on iOS and compiler-plugin mocks (Mokkery) are tied to the
Kotlin version. This is also Google's recommendation for Android.

## 2. What is tested with what

| Layer | Test against | Source set |
|---|---|---|
| ViewModel | real `*UseCaseImpl` + **fake repositories** | `commonTest` |
| Use case, `ConstraintsManager`, ordering/mapping helpers, `Presentable*` mappers with logic | fake repositories / plain values | `commonTest` |
| Firestore-backed repository (`HouseholdRepositoryImpl`, `UserRepositoryImpl`) | **fake data sources** + `FakeAuthService` | `commonTest` |
| Room-backed repository (`LoyaltyCardRepositoryImpl`, `SuggestionRepositoryImpl`) and DAO queries | **real in-memory database** — never a fake DAO | `databaseTest` — written once, runs on the iOS simulator and an Android device/emulator (Room's Android build needs a `Context`; Robolectric is not recommended by Google) |
| Room migrations (`SpizaDatabaseMigrations.kt`) | `MigrationTestHelper` over the committed `shared/schemas/` (`spizaMigrationTestHelper()`) | `databaseTest` (`data/local/SpizaDatabaseMigrationTest.kt`) |
| Platform code (permissions, scanners, barcode encoders) | platform fakes (`androidHostTest/.../fakes/`) | `androidHostTest` / `iosTest` |
| Firestore security rules (`firestore.rules`) | Firestore emulator | `firestore-tests/` (`npm test`) |
| Composables, Koin modules, navigation wiring | not unit-tested | — |

Tests mirror the production package (`domain/usecase/HouseholdItemsUseCasesTest.kt`).

## 3. What every unit must prove — expected behaviour first

Every unit with its own logic proves that it does what the feature needs **when everything succeeds**:
- **ViewModel:** each public action and each observed stream → the resulting state, the use-case call it triggers (with the right arguments,
  seen through the fake) and the follow-up UI state (dialog dismissed, input reset, navigation flag). Because fakes reflect writes in their
  streams, assert what the user would see (`onAddItem("Butter")` → Butter is among the pending items).
- **Repository:** each method **with its own logic** (mapping, ordering, merging, caching, guards, limits, SQL) → what it writes/reads and
  the domain value it returns or emits. A method that only forwards gets a test only when it passes two or more arguments of the same type
  (ids that could be swapped) — one test per method.
- **Use case / `ConstraintsManager` / helpers:** the rule's allowed outcome and its rejected outcome.

Then the **error paths**: for every guarded write or stream, the resulting state and what `FakeCrashLogger` recorded
(expected reasons → `warnings`, bugs → `exceptions` whose `cause` is the original error). Error tests never replace expected-behaviour tests.

## 4. Structure of a test class

1. Fakes and the system under test as properties; `@BeforeTest`/`@AfterTest` for dispatchers or the database.
2. Private builders (`private fun viewModel() = ...`).
3. **Expected-behaviour tests**, grouped per action/method in the order of the public interface. This includes alternative successful
   branches (dedup that unchecks or moves an existing item, ignoring a second scan, a duplicate name that closes the dialog without saving).
4. `// region Error handling` … `// endregion`: thrown or failed calls, failed streams, retry after failure, rejected input
   (blank, too long, invalid), rule rejections (not the owner), limits reached.

Each test: arrange / act / assert separated by a blank line. Name: `methodOrBehaviour_expectedResult_whenCondition`.
One behaviour per test (one action and one condition), asserting the whole meaningful outcome (state + calls + what was logged).

## 5. Flows and state

- ViewModels: `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest`, `Dispatchers.resetMain()` in `@AfterTest`.
- **Current state after an action** of a ViewModel `StateFlow` that is always active: read `state().value` — simplest and not flaky.
- **A sequence of emissions, a flow that must complete or fail, or a `stateIn(WhileSubscribed)` state:** Turbine —
  `flow.test { assertThat(awaitItem()).isEqualTo(...); cancelAndIgnoreRemainingEvents() }`. Do not hand-roll
  `backgroundScope.launch { collect {} }` collectors.
- Assert a logged cause by message: `assertThat(logger.exceptions.single().cause?.message).isEqualTo(failure.message)` — coroutines'
  stack-trace recovery may copy the exception, so identity checks are fragile.
- `neverComplete…` controls leave a `viewModelScope` coroutine suspended; that is fine because it lives outside `runTest`.

## 6. How much to test

The goal is the **smallest set of tests that fails when a change breaks behaviour a user or another layer relies on** — not coverage.
- Before writing a test, name the bug it catches that no other test catches. If there is none, don't write it.
- Test logic **once, where it lives**. A shared helper (`isQuotaExceeded`, `recordError`, ordering) gets its own tests; callers get at
  most one test proving they *use* it.
- Per behaviour: the expected outcome (always) + each **distinct** failure branch. Edge cases (blank, duplicates, N-1/N/N+1) only where
  the code branches on them. Applies to every feature, core or secondary; core flows (shopping list, ordering, membership, limits) get more depth.
- Don't test: pass-through use cases (covered through the ViewModel tests), repository methods that only delegate (except swapped ids),
  behaviour that lives only in a fake, data-class defaults, a lone `hasError == false`, symmetric duplicates, Kotlin/library behaviour.
- Don't hard-code values derived from logic tested elsewhere (exact positions, limits): assert relative order or use the public constant
  (`ConstraintsManagerImpl.maxLoyaltyCardShortcuts`). Never copy a limit into a test file.
- Assert mappings against expected raw values (entity/DTO fields), not through the production inverse mapper.
- Every bug fix ships with a regression test that fails before the fix.
- Deterministic: virtual time only, no network, no real Firebase. Real infrastructure only where it is the thing under test: Room
  (in-memory; file-backed only in migration tests) and the local Firestore emulator for the rules tests.

## 7. Running

| What | Command |
|---|---|
| Fast loop (commonTest + androidHostTest on JVM) | `./gradlew :shared:testAndroidHostTest` (filter: `--tests "com.mzagorscak.spiza.domain.usecase.*"`) |
| iOS + Room database tests | `./gradlew :shared:iosSimulatorArm64Test` — required when touching `iosTest`, `databaseTest`, Room, expect/actual or iOS code |
| Android device tests (commonTest + `databaseTest` on Android) | `./gradlew :shared:connectedAndroidDeviceTest` with an emulator/device attached; compile only: `./gradlew :shared:assembleAndroidDeviceTest` |
| Firestore rules | `cd firestore-tests && npm test` (first time: `npm install`) |
| Coverage report | `./gradlew :shared:koverHtmlReport` |
| Lint | `./gradlew ktlintFormat ktlintCheck` |

# Spiza testing handbook

A practical guide to the tests in this repository: what exists, where it lives, how to run it and how to add more.
The binding rules for writing tests are in [`.claude/rules/testing.md`](../.claude/rules/testing.md); this handbook explains them.

## Contents

1. [Test levels at a glance](#1-test-levels-at-a-glance)
2. [Where the tests live](#2-where-the-tests-live)
3. [Tools](#3-tools)
4. [What is tested with what](#4-what-is-tested-with-what)
5. [Running the tests](#5-running-the-tests)
6. [Anatomy of a test class](#6-anatomy-of-a-test-class)
7. [Fakes](#7-fakes)
8. [Database tests (`databaseTest`)](#8-database-tests-databasetest)
9. [Migration tests](#9-migration-tests)
10. [Firestore security rules tests](#10-firestore-security-rules-tests)
11. [Coverage](#11-coverage)
12. [When to run what](#12-when-to-run-what)
13. [Recipes](#13-recipes)
14. [Troubleshooting](#14-troubleshooting)
15. [Why it is built this way](#15-why-it-is-built-this-way)

---

## 1. Test levels at a glance

| Level | What it covers | Runs on | Command |
|---|---|---|---|
| **1. Unit** | ViewModels, use cases, `ConstraintsManager`, Firestore-backed repositories, mappers and helpers — over fakes | JVM (Android host) **and** iOS simulator | `./gradlew :shared:testAndroidHostTest` |
| **2. Database** | Room DAOs and Room-backed repositories on a **real in-memory database**, written once | iOS simulator **and** Android device/emulator | `./gradlew :shared:iosSimulatorArm64Test` / `:shared:connectedAndroidDeviceTest` |
| **3. Migrations** | Every Room migration (1→2, 2→3, …) with realistic old data, validated against the committed schemas | iOS simulator and Android device/emulator | same as level 2 |
| **4. Firestore rules** | Who may read and write households, items, user profiles and join keys | local Firestore emulator | `cd firestore-tests && npm test` |
| Coverage | Which shared logic no unit test executes | JVM | `./gradlew :shared:koverHtmlReport` |

Not covered by automated tests (by design, for now): composables, Koin wiring, navigation wiring, Swift bridges, end-to-end UI flows.

## 2. Where the tests live

```
shared/
  schemas/com.mzagorscak.spiza.data.local.SpizaDatabase/   1.json 2.json 3.json — exported Room schemas (committed, never regenerated)
  src/
    commonTest/kotlin/com/mzagorscak/spiza/
      fakes/                 hand-written test doubles + TestData.kt builders (shared by every test)
      ui/screens/<feature>/  ViewModel tests
      ui/model/              Presentable* mapper tests
      domain/usecase/        use-case tests
      domain/...             ConstraintsManager, ordering, codes, models
      data/household|user/   Firestore-backed repository tests (over fake data sources)
      data/settings|shortcuts/
      crash/, navigation/    helpers
    databaseTest/kotlin/com/mzagorscak/spiza/data/
      local/                 InMemoryDatabase.kt, MigrationTestDatabase.kt (expect), SpizaDatabaseMigrationTest.kt
      loyaltyCard/, suggestion/   Room-backed repository tests
    iosTest/                 iOS actuals for databaseTest + iOS-only tests (barcode encoder, scanner types)
    androidDeviceTest/       Android actuals for databaseTest (instrumented, needs a device/emulator)
    androidHostTest/         Android-only JVM tests (permissions, ML Kit mapping, barcode round trip) + their fakes
firestore-tests/
  test/setup.ts              emulator setup, ids, document builders, seed()/asUser()/asSignedOut()
  test/*.test.ts             one file per collection group
```

**Source-set graph** (an arrow means "can use the code of"):

```
commonTest ◄── androidHostTest            (JVM, fast)
     ▲
     │
databaseTest ◄── iosTest                  (iOS simulator)
             ◄── androidDeviceTest        (Android device / emulator)
```

So `commonTest` runs on JVM, iOS and Android devices; `databaseTest` runs on iOS and Android devices, never on the JVM host.
Tests mirror the production package: `ui/screens/shoppingList/ShoppingListViewModelImpl.kt` → `ui/screens/shoppingList/ShoppingListViewModelImplTest.kt`.

## 3. Tools

| Tool | Used for | Notes |
|---|---|---|
| `kotlin.test` | `@Test`, `@BeforeTest`, `@AfterTest`, `@Ignore` | annotations only |
| **AssertK** | every assertion: `assertThat(x).isEqualTo(y)`, `containsExactly`, `isEmpty`, `isTrue`, `isInstanceOf<T>()`, `assertFailure { }` | readable failure messages |
| `kotlinx-coroutines-test` | `runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle`, `backgroundScope` | virtual time: timeouts and delays are instant |
| **Turbine** | asserting what a `Flow` emits: `flow.test { awaitItem(); awaitError(); cancelAndIgnoreRemainingEvents() }` | |
| Hand-written **fakes** | replacing repositories, Firestore data sources and platform objects | no mocking library |
| **Room in-memory database** + `room-testing` (`MigrationTestHelper`) | DAO SQL, Room-backed repositories, migrations | `BundledSQLiteDriver`, same SQLite as production |
| **Kover** | coverage report | measured on the JVM host tests |
| **Firestore emulator** + `@firebase/rules-unit-testing` + **Vitest** | security rules | Node project in `firestore-tests/` |

## 4. What is tested with what

| Production code | Test double / infrastructure | Source set |
|---|---|---|
| ViewModel | real `*UseCaseImpl` classes + **fake repositories** | `commonTest` |
| Use case, `ConstraintsManager`, ordering, mappers with logic | fake repositories / plain values | `commonTest` |
| Firestore-backed repository (`HouseholdRepositoryImpl`, `UserRepositoryImpl`) | **fake data sources** + `FakeAuthService` | `commonTest` |
| Room-backed repository (`LoyaltyCardRepositoryImpl`, `SuggestionRepositoryImpl`) and DAO queries | **real in-memory database** — never a fake DAO | `databaseTest` |
| Room migrations | `MigrationTestHelper` over `shared/schemas/` | `databaseTest` |
| Platform code (permissions, scanners, barcode encoders) | platform fakes | `androidHostTest` / `iosTest` |
| `firestore.rules` | Firestore emulator | `firestore-tests/` |

**Rule of thumb:** fake what the unit under test *talks to*, never the logic the test is about. If the logic lives in SQL, test it on a
real database; if it lives in a security rule, test it on the emulator.

## 5. Running the tests

### Kotlin (from the repository root)

| What | Command | Needs |
|---|---|---|
| Unit tests (fast loop) | `./gradlew :shared:testAndroidHostTest` | — |
| One class or package | `./gradlew :shared:testAndroidHostTest --tests "com.mzagorscak.spiza.domain.usecase.*"` | — |
| iOS: unit + database + migration tests | `./gradlew :shared:iosSimulatorArm64Test` | macOS with Xcode |
| Android device: unit + database + migration tests | `./gradlew :shared:connectedAndroidDeviceTest` | running emulator or connected device (`adb devices`) |
| Compile device tests only | `./gradlew :shared:assembleAndroidDeviceTest` | — |
| Coverage report | `./gradlew :shared:koverHtmlReport` → open `shared/build/reports/kover/html/index.html` | — |
| Coverage summary in the console | `./gradlew :shared:koverLog` | — |
| Lint | `./gradlew ktlintFormat ktlintCheck` | — |

Results: JVM → `shared/build/reports/tests/testAndroidHostTest/index.html`; iOS → `shared/build/reports/tests/iosSimulatorArm64Test/`;
Android device → `shared/build/outputs/androidTest-results/connected/`.

Start an emulator without Android Studio: `~/Library/Android/sdk/emulator/emulator -list-avds`, then
`~/Library/Android/sdk/emulator/emulator -avd <name> -no-snapshot &`.

From Android Studio: the gutter ▶ next to a test class runs it on the JVM; for `databaseTest` choose the iOS or Android device run configuration.

### Firestore rules (from `firestore-tests/`)

| What | Command |
|---|---|
| First time | `npm install` (needs Node ≥ 24.12 and Java) |
| Run once | `npm test` — starts the emulator, runs Vitest, stops the emulator |
| Watch mode while editing rules | `npm run test:watch` |

## 6. Anatomy of a test class

Every test class has the same shape:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelImplTest {
    // 1. Fakes and collaborators
    private val households = FakeHouseholdRepository()
    private val logger = FakeCrashLogger()

    // 2. Setup / teardown (Main dispatcher for ViewModels, database for databaseTest)
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // 3. Builder of the unit under test, with REAL use cases over the fakes
    private fun viewModel() = ShoppingListViewModelImpl(addItem = AddArticleItemUseCaseImpl(repository = households), /* ... */)

    // 4. Expected behaviour first, in the order of the public interface
    @Test
    fun addItem_showsNewItemAmongPendingItems_afterTheExistingOnes() =
        runTest {
            val viewModel = viewModel()

            viewModel.onAddItem("Butter")

            assertThat(viewModel.content().pendingItems.map { it.text }).containsExactly("Milk", "Eggs", "Butter")
        }

    // 5. Failures, rejections, limits
    // region Error handling

    @Test
    fun failedWrite_showsWriteError_untilItIsShown() = runTest { /* ... */ }

    // endregion
}
```

Conventions:
- **Name:** `methodOrBehaviour_expectedResult_whenCondition`.
- **Body:** arrange / act / assert, separated by blank lines. One behaviour per test, asserting the whole meaningful outcome.
- **Expected-behaviour section:** every successful path, including alternative successful branches (e.g. adding an item that is already
  on the list moves it instead of duplicating it).
- **`// region Error handling`:** thrown or failed calls, failed streams, retry after a failure, rejected input, rule rejections, limits.
- **Assert what the user would see**, not just that a method was called: fakes reflect writes in their streams, so after `onAddItem` the
  item is in the screen state.
- **Logged errors:** `assertThat(logger.exceptions.single().cause?.message).isEqualTo(failure.message)` — compare by message, because
  coroutines may copy the exception.
- **State:** read `state().value` for always-active `StateFlow`s; use Turbine (`state().test { }`) for `stateIn(WhileSubscribed)` states
  and for sequences of emissions.

Copy-paste templates for every layer: [`.claude/skills/write-unit-tests/templates.md`](../.claude/skills/write-unit-tests/templates.md).

## 7. Fakes

A fake is a small working in-memory implementation of an interface, shared by all tests (`commonTest/.../fakes/`).

| Fake | Replaces | Typical controls |
|---|---|---|
| `FakeHouseholdRepository` | `HouseholdRepository` (ViewModel / use-case tests) | `households`, `items`, `joinKey`, `writeFailure`, `householdsFailure`, `itemsFailure`, `neverCompleteWrites`, `addMemberResult` |
| `FakeHouseholdDataSource` | Firestore household data source (repository tests) | `households`, `items`, `joinKeys`, `writeFailure`, `neverAcknowledgeWrites` |
| `FakeAccountDataSource` | account data deletion batch (`AccountRepositoryImpl` tests) | `deleteFailure`, `gate`, `neverAcknowledge`, `onDelete`; records `deleteAttempts`, `deletedUserIds` |
| `FakeUserRepository` / `FakeUserDataSource` / `FakeAuthService` | user profile and sign-in | `signInFailure`, `signOutFailure`, `saveFailure`, `getUsersFailure` |
| `FakeLoyaltyCardRepository` | `LoyaltyCardRepository` (ViewModel / use-case tests) | `cards`, `observeFailure`, `writeFailure`, `beforeAddShortcut` |
| `FakeSuggestionRepository` | `SuggestionRepository` | `suggestions`, `observeFailure`, `writeFailure` |
| `FakeSettingsRepository` / `FakeSettings` | settings repository / key-value storage | `themeModes`, `languages`, `savingEnabled` |
| `FakePermissionRepository`, `FakeAppShortcutsPublisher` | permissions, launcher shortcuts | results and failures |
| `FakeCrashLogger` | Crashlytics | records `exceptions` and `warnings` |
| `TestData.kt` | — | `TEST_USER`, `TEST_PROFILE`, `household()`, `householdDto()`, `item()` |

The rules every fake follows (details: [`fakes.md`](../.claude/skills/write-unit-tests/fakes.md)):
1. Behaves like the real store — writes change the observed streams; ordering and filtering the real query guarantees are reproduced.
2. Contains no business logic (no trimming, dedup or limits — that is production code under test).
3. Records every call as an attempt **before** a configured failure is thrown.
4. Failures are checked when the flow is **collected**, so they can be set after the ViewModel is built.
5. One recording list per call; changes are additive; KDoc on every control.

Never fake a Room DAO — use the real database (section 8).

## 8. Database tests (`databaseTest`)

Room's logic is in SQL, so a fake DAO would test a copy of the logic, not the query. Database tests run the real SQL on a real in-memory
`SpizaDatabase` with the production driver (`BundledSQLiteDriver`).

- Write the test once in `shared/src/databaseTest/...`.
- Open the database with `inMemorySpizaDatabase()` in `@BeforeTest` and close it in `@AfterTest`.
- `inMemorySpizaDatabase()` is an `expect` function: the iOS actual needs no `Context`; the Android actual uses
  `ApplicationProvider.getApplicationContext()`.
- These tests **cannot** run as JVM host tests: Room's Android build needs a real `Context`, and Google does not recommend Robolectric
  for Room. They run on the iOS simulator (part of the definition of done) and on an Android device/emulator (before a release).

```kotlin
class LoyaltyCardRepositoryImplTest {
    private lateinit var database: SpizaDatabase
    private lateinit var repository: LoyaltyCardRepositoryImpl

    @BeforeTest
    fun setUp() {
        database = inMemorySpizaDatabase()
        repository = LoyaltyCardRepositoryImpl(dao = database.loyaltyCardDao())
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun observeAll_emitsEveryCardAsDomainModel_newestFirst() = runTest { /* add cards, assert order via Turbine */ }
}
```

## 9. Migration tests

Room compares the schema on the device with the one compiled into the app; a missing or wrong migration crashes the app for every
existing user right after an update. Migration tests catch that before release.

- `exportSchema = true` + the Room Gradle plugin write `shared/schemas/.../<version>.json` on every KSP run. **Commit every schema file**
  and never edit or regenerate one that is committed — it is the record of what is on users' devices.
  (`1.json` and `2.json` were reconstructed from git history and can't be regenerated from the current code.)
- `SpizaDatabaseMigrationTest` (in `databaseTest`) uses `MigrationTestHelper`:
  1. `helper.createDatabase(oldVersion)` creates the old schema from the committed JSON;
  2. insert rows the way that app version stored them (raw SQL);
  3. `helper.runMigrationsAndValidate(newVersion, listOf(MIGRATION_X_Y))` migrates and checks the schema matches the new version;
  4. assert the migrated data.
- A final test opens a version-1 database with the production builder (`getSpizaDatabase(...)`) and reads through the DAOs.

Changing the database: follow `/room-change` (bump the version, write `MIGRATION_N_N+1`, build to export `N+1.json`, add a migration test,
run `:shared:iosSimulatorArm64Test`, and `:shared:connectedAndroidDeviceTest` before the release).

## 10. Firestore security rules tests

`firestore.rules` decides who can read and change other people's households. A rule mistake is a security bug that no Kotlin test can catch.

- Tests run against the local emulator with the offline demo project `demo-spiza`; they never touch the real Firebase project.
- The rules are read straight from `../firestore.rules`, so the tests always check the file that gets deployed.
- `test/setup.ts`: users `OWNER`, `MEMBER`, `STRANGER`; `household()`, `item()` builders; `seed(env, db => ...)` writes with rules
  disabled to arrange state; `asUser(env, uid)` / `asSignedOut(env)` give a Firestore instance as that caller.
- Each rule gets an allowed case (`assertSucceeds`) and a case for every caller that must be refused (`assertFails`): signed out,
  stranger, member vs owner, acting for another user, unknown fields, wrong types.
- Write documents **exactly as the app does** — the same batch, `arrayUnion` / `arrayRemove`, and the same queries
  (`where("joinedMembers", "array-contains", uid)`). A rule can allow `getDoc` but reject the app's query.

```ts
it("allows a member to leave the household", async () => {
  await assertSucceeds(updateDoc(householdRef(asUser(env, MEMBER)), { joinedMembers: arrayRemove(MEMBER) }));
});
```

After changing the rules: `npm test` green → deploy (Firebase console > Firestore > Rules, or `firebase deploy --only firestore:rules`).
Old app versions keep using whatever rules are deployed. Workflow: `/firestore-rules-tests`.

## 11. Coverage

`./gradlew :shared:koverHtmlReport` shows which lines of shared logic no JVM test executes. Use it to find untested branches, not as a target.

Excluded from the report (see `kover { }` in `shared/build.gradle.kts`): composables and previews, theme, DI modules, generated code, and
the Room-backed repositories — those are tested in `databaseTest`, which runs on the iOS simulator and Android devices, where Kover
doesn't measure.

## 12. When to run what

| You changed | Run |
|---|---|
| Any Kotlin code | `./gradlew ktlintCheck :shared:testAndroidHostTest :androidApp:assembleDebug` (definition of done) |
| Room entity, DAO, migration, `shared/schemas/`, `databaseTest`, `iosTest`, `iosMain` or a Swift bridge | + `./gradlew :shared:iosSimulatorArm64Test` |
| `firestore.rules`, a Firestore collection, field or write | + `cd firestore-tests && npm test`, then deploy the rules |
| Before a release | all of the above + `./gradlew :shared:connectedAndroidDeviceTest` on an emulator, and install the `qa` build on a device |

The commit checklist (`.claude/rules/git.md`, `/commit`) enforces the same gates; a pre-commit hook runs `ktlintCheck`.

## 13. Recipes

**New screen / ViewModel**
1. Create `ui/screens/<feature>/<X>ViewModelImplTest.kt` from the ViewModel template.
2. Build the ViewModel with the real `*UseCaseImpl` classes over the existing fakes; extend a fake if a control is missing.
3. One expected-behaviour test per public action and observed stream, then one test per distinct error branch.

**New use case with a business rule**
Test the allowed outcome and each rejection (blank input, duplicates, N-1/N/N+1 limits) in `domain/usecase/<Feature>UseCasesTest.kt`.
A use case that only forwards to the repository needs no own test — the ViewModel tests cover it.

**New Firestore field or collection**
Update `firestore.rules`, the Android data source, the Swift bridge and the fake data source; add allowed and rejected rules tests; run
`npm test`; tell whoever deploys that the rules changed.

**New DAO query**
Test it in the Room-backed repository test in `databaseTest` with rows that would come out in a different order (or be filtered
differently) if the SQL were wrong.

**Bug fix**
Write a test that fails without the fix first (`/fix-bug`), then fix, then keep the test.

**A test exposes a bug you are not fixing now**
Keep the test with `@Ignore` and a `// BUG: ...` comment explaining the expected behaviour, and report it.

## 14. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Module with the Main dispatcher is missing` / ViewModel coroutines never run | missing `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest` |
| A `stateIn(WhileSubscribed)` state stays at its initial value | nothing collects it — use `state().test { }` (Turbine) |
| `assertSame` on a logged cause fails although the error is the same | coroutines' stack-trace recovery copied the exception — compare by message |
| `connectedAndroidDeviceTest`: `No connected devices!` | start an emulator or plug in a device (`adb devices`) |
| iOS migration test can't find schemas | run through Gradle (it passes the schema directory to the simulator via `SIMCTL_CHILD_SPIZA_ROOM_SCHEMA_DIR`) |
| Room: `Cannot find the schema file … <n>.json` | the schema for that version isn't committed — build once to export it and commit it |
| Room: `Migration didn't properly handle …` | the migration SQL doesn't produce what the entity declares — compare the expected and found table info in the message |
| `npm test`: `Could not start Firestore Emulator, port taken` | another emulator is running: `lsof -i :8080` and stop it |
| `npm test`: Java not found | the emulator needs Java 11+ on `PATH` |
| A rules test fails with `PERMISSION_DENIED` you didn't expect | the document under test wasn't seeded, or the write differs from what the app sends — seed with `seed()` and mirror the app's write |

## 15. Why it is built this way

- **Fakes instead of a mocking library.** Fakes assert outcomes ("the item is on the list") instead of calls ("method X was called"),
  so refactoring doesn't break tests, and one fake serves every test. This is Google's recommendation and what *Now in Android* does.
  MockK doesn't run on iOS, and compiler-plugin mocking libraries are tied to the Kotlin version.
- **Real use cases in ViewModel tests.** The screen depends on the rules in its use cases; testing them together catches wiring mistakes
  at no extra cost.
- **Real database for Room.** The behaviour is in the SQL; only a real SQLite can check it. In-memory keeps the tests fast and hermetic.
- **`databaseTest` shared by iOS and Android.** One test, two platforms — the same as *Now in Android*'s instrumented DAO tests, plus iOS.
- **Expected behaviour first.** Error tests alone don't prove a feature works; every unit first proves the successful outcome.
- **The smallest set of tests that catches real regressions.** Before writing a test, name the bug it catches that no other test does —
  every test is code to maintain.

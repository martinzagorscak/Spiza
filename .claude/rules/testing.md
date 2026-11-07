---
paths:
  - "shared/src/commonTest/**"
  - "shared/src/androidHostTest/**"
  - "shared/src/iosTest/**"
---
# Testing rules

- Shared logic tests go in `shared/src/commonTest/kotlin/com/mzagorscak/spiza/...`, **mirroring the production package**
  (e.g. `domain/usecase/HouseholdItemsUseCasesTest.kt`). Platform-only code tests go in `androidHostTest` / `iosTest`.
- Framework: `kotlin.test` (`@Test`, `assertEquals`, `assertTrue`, `assertFailsWith`...). No JUnit-only APIs in commonTest.
- Naming: `fun methodOrBehaviour_expectedResult_whenCondition()` (see `ConstraintsManagerTest`). One behaviour per test.
- **No mocking library** in the project. Use the hand-written fakes in `commonTest/.../fakes/` and extend them before creating new ones:
  `FakeCrashLogger` (records `exceptions` / `warnings`), `FakeAuthService`, `FakeUserRepository`, `FakeUserDataSource`,
  `FakeHouseholdRepository`, `FakeHouseholdDataSource`, `FakeSettingsRepository`, `FakeSuggestionRepository`, and builders in `TestData.kt`
  (`TEST_USER`, `TEST_PROFILE`, `household()`, `householdDto()`, `item()`). Fakes record calls and can be told to fail
  (`writeFailure`, `householdsFailure`, `getUsersFailure`, `saveFailure`) or never complete (`neverAcknowledgeWrites`, like Firestore offline).
- Coroutines: `kotlinx-coroutines-test` is available in commonTest. Wrap tests in `runTest { }` (virtual time, so timeouts and delays are instant).
  ViewModel tests call `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest` and `Dispatchers.resetMain()` in `@AfterTest`, then
  build the ViewModel with the real `*UseCaseImpl` classes on top of the fakes (see `ShoppingListViewModelImplTest`).
- Error paths are first-class: for every guarded write or stream, test the resulting state and what `FakeCrashLogger` recorded
  (expected reasons → `warnings`, bugs → `exceptions` with the original error as `cause`).
- Prioritise: use cases (business rules), `ConstraintsManager`, ordering/mapping helpers, repository logic with fake data sources,
  ViewModel state transitions and error paths. Do not unit-test composables or Koin wiring.

## How much to test
The goal is the **smallest set of tests that fails when a change breaks behaviour a user or another layer relies on** — not coverage.
Every test is code to maintain; a redundant test is pure cost when state shapes change.

- Before writing a test, name the bug it catches that no other test catches. If there is none, don't write it.
- Test logic **once, where it lives**. A shared helper (`isQuotaExceeded`, `recordError`, ordering) gets its own focused tests;
  callers get at most one test proving they *use* it, not a copy of its edge cases.
- Per behaviour: the main success path + each **distinct** failure branch. Add edge cases (blank, duplicates, N-1/N/N+1) only where
  the code actually branches on them (use cases, `ConstraintsManager`, ordering).
- Core flows deserve depth (shopping list, item ordering, household membership, limits). Secondary screens: one test per distinct
  branch of *their own* logic (e.g. quota vs generic error), not every action × every error.
- Don't test: pass-through use cases, default values of data classes, "no error when it succeeds" (other tests already break),
  symmetric duplicates (`onNameChange` and `onDescriptionChange` doing the same reset), Kotlin/coroutines/library behaviour.
- Prefer one test asserting a whole meaningful outcome (state + what was logged) over several tests asserting one field each.
- Every bug fix ships with a regression test that fails before the fix.
- Tests are deterministic: no real time (`delay` only under `runTest` virtual time), no network, no Firebase, no real Room DB.
- Run `./gradlew :shared:testAndroidHostTest` (fast). Run `:shared:iosSimulatorArm64Test` when the test touches expect/actual or iOS code.

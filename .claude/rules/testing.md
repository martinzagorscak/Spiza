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
  `FakeHouseholdRepository`, `FakeHouseholdDataSource`, `FakeSettingsRepository`, `FakeSuggestionLocalDataSource`, and builders in `TestData.kt`
  (`TEST_USER`, `TEST_PROFILE`, `household()`, `householdDto()`, `item()`). Fakes record calls and can be told to fail
  (`writeFailure`, `householdsFailure`, `getUsersFailure`, `saveFailure`) or never complete (`neverAcknowledgeWrites`, like Firestore offline).
- Coroutines: `kotlinx-coroutines-test` is available in commonTest. Wrap tests in `runTest { }` (virtual time, so timeouts and delays are instant).
  ViewModel tests call `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest` and `Dispatchers.resetMain()` in `@AfterTest`, then
  build the ViewModel with the real `*UseCaseImpl` classes on top of the fakes (see `ShoppingListViewModelImplTest`).
- Error paths are first-class: for every guarded write or stream, test the resulting state and what `FakeCrashLogger` recorded
  (expected reasons → `warnings`, bugs → `exceptions` with the original error as `cause`).
- Prioritise: use cases (business rules), `ConstraintsManager`, ordering/mapping helpers, repository logic with fake data sources,
  ViewModel state transitions and error paths. Do not unit-test composables or Koin wiring.
- Every bug fix ships with a regression test that fails before the fix.
- Tests are deterministic: no real time (`delay` only under `runTest` virtual time), no network, no Firebase, no real Room DB.
- Run `./gradlew :shared:testAndroidHostTest` (fast). Run `:shared:iosSimulatorArm64Test` when the test touches expect/actual or iOS code.

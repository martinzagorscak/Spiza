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
- **No mocking library** in the project. Use hand-written fakes in `commonTest/.../fakes/` (`FakeHouseholdRepository`, `FakeCrashLogger`,
  `FakeAuthService`...). Fakes record calls (`val addedItems = mutableListOf<...>()`) and can be configured to throw
  (`var failNextWrite: Throwable? = null`). Reuse existing fakes before creating new ones.
- Coroutines/Flow/ViewModel tests need `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`, `Dispatchers.setMain`).
  It is **not** a dependency yet — ask the user before adding it to `libs.versions.toml` + `commonTest.dependencies`.
  ViewModel tests must `Dispatchers.setMain(testDispatcher)` in `@BeforeTest` and `resetMain()` in `@AfterTest` (viewModelScope uses Main).
- Prioritise: use cases (business rules), `ConstraintsManager`, ordering/mapping helpers, repository logic with fake data sources,
  ViewModel state transitions and error paths. Do not unit-test composables or Koin wiring.
- Every bug fix ships with a regression test that fails before the fix.
- Tests are deterministic: no real time (`delay` only under `runTest` virtual time), no network, no Firebase, no real Room DB.
- Run `./gradlew :shared:testAndroidHostTest` (fast). Run `:shared:iosSimulatorArm64Test` when the test touches expect/actual or iOS code.

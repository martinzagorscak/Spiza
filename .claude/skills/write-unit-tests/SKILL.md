---
name: write-unit-tests
description: Write and run unit tests for Spiza shared code (use cases, constraints, mappers/ordering, repositories with fakes, ViewModel state and error paths). Use when asked to test something, add coverage, or write a regression test.
---

# Write unit tests

Target: `$ARGUMENTS` (class, file, feature, or "uncommitted changes" → use `git diff --name-only`).
Rules: `.claude/rules/testing.md`. For larger scopes delegate to the `test-writer` agent.

## Steps
1. Read the target and list behaviours: happy path · edge cases (blank/whitespace, case-insensitive duplicates, empty lists, limit N-1/N/N+1, null) · error paths (data source throws, stream fails, unauthenticated, timeout).
2. Fakes: use and extend the ones in `shared/src/commonTest/kotlin/com/mzagorscak/spiza/fakes/` (listed in `.claude/rules/testing.md`).
   Only create a new fake for an interface that has none; keep it a plain class that records calls and can be configured to fail.
3. `kotlinx-coroutines-test` is already a commonTest dependency (`runTest`, `UnconfinedTestDispatcher`, `setMain`/`resetMain`).
4. ViewModel test skeleton:
   ```kotlin
   @OptIn(ExperimentalCoroutinesApi::class)
   class XViewModelImplTest {
       @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
       @AfterTest fun tearDown() = Dispatchers.resetMain()

       @Test fun onSave_setsWriteError_whenRepositoryThrows() = runTest {
           val repo = FakeXRepository().apply { failNextWrite = IllegalStateException("boom") }
           val logger = FakeCrashLogger()
           val vm = XViewModelImpl(saveX = SaveXUseCaseImpl(repo), crashLogger = logger)
           backgroundScope.launch { vm.state().collect {} } // only needed for stateIn(WhileSubscribed) states
           vm.onSave()
           assertEquals(ErrorState.WriteError, vm.state().value.errorState)
           assertEquals(1, logger.exceptions.size)
       }
   }
   ```
5. Run `./gradlew :shared:testAndroidHostTest` (filter: `--tests "com.mzagorscak.spiza.domain.usecase.*"`), then `./gradlew ktlintCheck`.
6. Report the tests added and any bug discovered (do not silently fix production code).

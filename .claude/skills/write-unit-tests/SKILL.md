---
name: write-unit-tests
description: Write and run unit tests for Spiza shared code (use cases, constraints, mappers/ordering, repositories with fakes, ViewModel state and error paths). Use when asked to test something, add coverage, or write a regression test.
---

# Write unit tests

Target: `$ARGUMENTS` (class, file, feature, or "uncommitted changes" → use `git diff --name-only`).
Rules: `.claude/rules/testing.md`. For larger scopes delegate to the `test-writer` agent.

## Steps
1. Read the target and list behaviours: happy path · edge cases (blank/whitespace, case-insensitive duplicates, empty lists, limit N-1/N/N+1, null) · error paths (data source throws, stream fails, unauthenticated, timeout).
2. Fakes: look in `shared/src/commonTest/kotlin/com/mzagorscak/spiza/fakes/`; create missing ones there. Template:
   ```kotlin
   class FakeLoyaltyCardRepository : LoyaltyCardRepository {
       val cards = MutableStateFlow<List<LoyaltyCard>>(emptyList())
       var failNextWrite: Throwable? = null
       override fun observeAll() = cards
       override suspend fun addCard(card: LoyaltyCard) {
           failNextWrite?.let { failNextWrite = null; throw it }
           cards.update { it + card }
       }
       // ...
   }
   class FakeCrashLogger : CrashLogger {
       val exceptions = mutableListOf<Throwable>(); val warnings = mutableListOf<String>()
       override fun recordException(throwable: Throwable) { exceptions += throwable }
       override fun logWarning(message: String) { warnings += message }
   }
   ```
3. Suspend / Flow / ViewModel tests require `kotlinx-coroutines-test`. If missing from `gradle/libs.versions.toml`, ask the user, then add:
   - `[libraries] kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "<coroutines version matching the project>" }`
   - `commonTest.dependencies { implementation(libs.kotlinx.coroutines.test) }` in `shared/build.gradle.kts`
4. ViewModel test skeleton:
   ```kotlin
   @OptIn(ExperimentalCoroutinesApi::class)
   class XViewModelImplTest {
       private val dispatcher = StandardTestDispatcher()
       @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
       @AfterTest fun tearDown() = Dispatchers.resetMain()

       @Test fun onSave_setsWriteError_whenRepositoryThrows() = runTest(dispatcher) {
           val repo = FakeXRepository().apply { failNextWrite = IllegalStateException("boom") }
           val logger = FakeCrashLogger()
           val vm = XViewModelImpl(saveX = SaveXUseCaseImpl(repo), crashLogger = logger)
           backgroundScope.launch { vm.state().collect {} } // keep WhileSubscribed flows active
           vm.onSave(); advanceUntilIdle()
           assertEquals(ErrorState.WriteError, vm.state().value.errorState)
           assertEquals(1, logger.exceptions.size)
       }
   }
   ```
5. Run `./gradlew :shared:testAndroidHostTest` (filter: `--tests "com.mzagorscak.spiza.domain.usecase.*"`), then `./gradlew ktlintCheck`.
6. Report the tests added and any bug discovered (do not silently fix production code).

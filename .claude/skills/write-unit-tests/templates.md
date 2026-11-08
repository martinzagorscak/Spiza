# Test templates per layer

Copy the matching template, rename, and delete what you don't need. Rules: `.claude/rules/testing.md`.
Common imports: `assertk.assertThat`, `assertk.assertions.*`, `kotlin.test.Test`, `kotlinx.coroutines.test.runTest`.

## ViewModel (`commonTest/.../ui/screens/<feature>/<X>ViewModelImplTest.kt`)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class XViewModelImplTest {
    private val repository = FakeXRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Real use cases over fakes: the test covers the use-case logic the screen relies on.
    private fun viewModel() =
        XViewModelImpl(
            observeX = ObserveXUseCaseImpl(repository = repository),
            saveX = SaveXUseCaseImpl(repository = repository),
            crashLogger = logger,
        )

    @Test
    fun state_showsStoredItems() =
        runTest {
            repository.items.value = listOf(item(id = "a"))

            val state = viewModel().state().value

            assertThat(state.items.map { it.id }).containsExactly("a")
        }

    @Test
    fun onSave_savesTrimmedNameAndClosesDialog() =
        runTest {
            val viewModel = viewModel()
            viewModel.showDialog()

            viewModel.onSave(" Milk ")

            assertThat(repository.saved).containsExactly("Milk")
            assertThat(viewModel.state().value.items.map { it.name }).contains("Milk") // fake reflects the write
            assertThat(viewModel.state().value.isDialogVisible).isFalse()
        }

    // region Error handling

    @Test
    fun onSave_showsGenericErrorAndRecordsIt_whenSaveFails() =
        runTest {
            val failure = IllegalStateException("boom")
            repository.writeFailure = failure
            val viewModel = viewModel()

            viewModel.onSave("Milk")

            assertThat(viewModel.state().value.error).isEqualTo(ErrorState.Generic)
            assertThat(logger.exceptions.single().cause?.message).isEqualTo(failure.message)
        }

    // endregion
}
```

For a `stateIn(WhileSubscribed)` state or a sequence of states, use Turbine instead of `.value`:

```kotlin
viewModel.state().test {
    assertThat(awaitItem().isLoading).isTrue()
    repository.items.value = listOf(item(id = "a"))
    assertThat(awaitItem().items).hasSize(1)
    cancelAndIgnoreRemainingEvents()
}
```

## Use case (`commonTest/.../domain/usecase/<Feature>UseCasesTest.kt`)

```kotlin
class XUseCasesTest {
    private val repository = FakeXRepository()

    @Test
    fun addX_trimsName_andAppendsAfterTheLastItem() =
        runTest {
            AddXUseCaseImpl(repository = repository)("  butter ")

            assertThat(repository.added.single().name).isEqualTo("butter")
        }

    // region Error handling

    @Test
    fun addX_doesNotWrite_whenNameIsBlank() =
        runTest {
            AddXUseCaseImpl(repository = repository)("   ")

            assertThat(repository.added).isEmpty()
        }

    // endregion
}
```

## Firestore-backed repository (`commonTest/.../data/<feature>/<X>RepositoryImplTest.kt`)

```kotlin
class XRepositoryImplTest {
    private val dataSource = FakeXDataSource()
    private val auth = FakeAuthService(currentUser = TEST_USER)
    private val logger = FakeCrashLogger()
    private val repository = XRepositoryImpl(dataSource = dataSource, authService = auth, crashLogger = logger)

    @Test
    fun observeX_mapsDtosToDomain_sortedByName() =
        runTest {
            dataSource.items.value = listOf(xDto(name = "b"), xDto(name = "A"))

            repository.observeX().test {
                assertThat(awaitItem().map { it.name }).containsExactly("A", "b")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // region Error handling

    @Test
    fun observeX_failsAfterRetries_whenListenerKeepsFailing() =
        runTest {
            dataSource.itemsFailure = DataException(DataException.Reason.Unknown)

            repository.observeX().test {
                assertThat(awaitError()).isInstanceOf<DataException>()
            }
            assertThat(logger.warnings).isNotEmpty()
        }

    // endregion
}
```

## Room-backed repository / DAO (`databaseTest/.../data/<feature>/<X>RepositoryImplTest.kt`)

Real SQL, no fake DAO. Lives in `databaseTest` (runs on the iOS simulator and an Android device/emulator) because Room's Android build needs a `Context`.

```kotlin
class XRepositoryImplTest {
    private lateinit var database: SpizaDatabase
    private lateinit var repository: XRepositoryImpl

    @BeforeTest
    fun setUp() {
        database = inMemorySpizaDatabase()
        repository = XRepositoryImpl(dao = database.xDao())
    }

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun observeAll_emitsNewestFirst() =
        runTest {
            repository.add(x(name = "old"))
            repository.add(x(name = "new"))

            repository.observeAll().test {
                assertThat(awaitItem().map { it.name }).containsExactly("new", "old")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
```

## Pure helper / mapper

```kotlin
class XMappingTest {
    @Test
    fun toPresentable_marksTheCurrentUserAsOwner() {
        val presentable = household(ownerId = TEST_USER.id).toPresentable(currentUserId = TEST_USER.id)

        assertThat(presentable.isCurrentUserOwner).isTrue()
    }
}
```

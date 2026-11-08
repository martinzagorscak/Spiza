# Writing and maintaining fakes

A fake is a small, working in-memory implementation of an interface, shared by every test. It replaces things a unit test cannot use:
repositories (in ViewModel / use-case tests), Firestore data sources, Swift bridges, platform objects.
**Never fake a Room DAO** — use the real in-memory database (`inMemorySpizaDatabase()`).

## Where

- `shared/src/commonTest/kotlin/com/mzagorscak/spiza/fakes/Fake<Interface>.kt` — one per interface.
- Platform-only fakes (e.g. moko `PermissionsController`): `androidHostTest/.../fakes/` or `iosTest/.../fakes/`.
- Test data builders (`TEST_USER`, `TEST_PROFILE`, `household()`, `householdDto()`, `item()`): `fakes/TestData.kt`.
- No private fakes inside test files. Before writing a new fake, check whether one exists and extend it.

## Anatomy

```kotlin
/**
 * Every write is recorded as an attempt before [writeFailure] applies; successful writes are reflected in [items].
 */
class FakeXRepository : XRepository {
    // 1. State — what the real store holds; tests seed it directly.
    val items = MutableStateFlow<List<X>>(emptyList())

    // 2. Controls — make the fake fail or hang; KDoc each one.
    /** When set, [observeItems] fails with it (checked each time the stream is collected). */
    var observeFailure: Throwable? = null

    /** When set, every write throws it. */
    var writeFailure: Throwable? = null

    /** Writes never complete, like a write waiting for the server. */
    var neverCompleteWrites = false

    // 3. Recordings — one list per write, with the arguments.
    val added = mutableListOf<X>()

    // 4. Behaviour
    override fun observeItems(): Flow<List<X>> =
        flow {
            observeFailure?.let { throw it }
            emitAll(items)
        }

    override suspend fun add(item: X) {
        added += item // record the attempt first
        write()
        items.value += item // reflect the write like the real store
    }

    private suspend fun write() {
        writeFailure?.let { throw it }
        if (neverCompleteWrites) awaitCancellation()
    }
}
```

## Rules

1. **Behave like the real implementation.** Writes change the observed streams (add/update/delete/move/clear), upserts upsert, ordering
   that the real store guarantees (e.g. Firestore query `orderBy`) is reproduced. A fake that diverges makes tests pass while the app is broken.
2. **No business logic in the fake.** Trimming, dedup, limits, mapping belong to the production code under test — the fake only stores.
   If you're about to copy a rule into a fake, the test is at the wrong layer.
3. **Record attempts before failing**, so a test can assert both "it tried" and "it failed".
4. **Failures are checked on collection** (`flow { failure?.let { throw it }; emitAll(...) }`), so they can be set after the ViewModel is built.
5. **Naming of controls:** `<x>Failure` / `writeFailure` (throw), `neverCompleteWrites` / `neverCompleteSignOut` (hang, repository),
   `neverAcknowledgeWrites` (hang, Firestore data source offline), `<x>Result` (configurable return value).
6. **One recording list per call** — no parallel "simple" and "detailed" lists. Derive in the test: `added.map { it.text }`.
7. **Changes are additive.** Don't change existing defaults; other tests depend on them. If a default must change, update every user.
8. **When the interface changes**, the fake no longer compiles — that is the reminder. Update the fake in the same change as the interface.

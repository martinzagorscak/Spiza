# Kotlin style

- ktlint (official Kotlin style) is enforced and `ignoreFailures = false`. Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck` before finishing.
- Trailing commas on multi-line parameter/argument lists. Named arguments when calling functions with 2+ parameters of the same type or any boolean.
- Multi-parameter functions put each parameter on its own line (as the existing code does).
- Visibility: implementation classes are `internal` (`internal class XViewModelImpl`, `internal class XRepositoryImpl`); interfaces and models are public.
- Constants: `private const val SCREAMING_CASE` at file top, with a unit suffix when relevant (`CREATE_TIMEOUT_MS`). Convert with `.milliseconds`.
- Prefer `isX.not()` over `!isX` only where it matches surrounding code — the codebase uses both; stay consistent within a file.
- Comments explain **why** (especially Firestore/platform quirks), not what. Keep KDoc on interfaces when a parameter is non-obvious (`/** [toIndex] is ... */`).
- No `!!`. No `GlobalScope`. No `runBlocking` in production code.
- `@OptIn(ExperimentalCoroutinesApi::class)` on the function, not the file.
- Do not leave unused imports, commented-out code, or `println` (use `CrashLogger.logWarning` or Kermit for diagnostics).

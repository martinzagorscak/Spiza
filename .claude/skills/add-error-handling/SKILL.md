---
name: add-error-handling
description: Add or harden error handling for a Spiza screen or flow across data source, repository, ViewModel and UI (retry, timeouts, error states, Crashlytics logging, localized messages). Use when something fails silently, crashes on errors, or the user asks for error handling.
---

# Add error handling

Target: `$ARGUMENTS` (screen, ViewModel, repository or flow). Contract: `.claude/rules/error-handling.md`. Helpers: `launchSafely`, `catchAndRecord`, `recordError`, `DataException` (see the rules file). Reference code: `SettingsViewModelImpl` (`launchAction` + error flag), `ShoppingListViewModelImpl` (error state + `retryTrigger`) and `HouseholdRepositoryImpl` (auth guard, constraints, `retryWhen`, `withTimeoutOrNull`, non-fatal secondary lookups).

1. **Failure map** — produce a table before editing:
   | Point | Failure | Current behaviour | Desired behaviour | Layer |
2. **Data source**: make sure it throws/close()s on failure instead of returning garbage; iOS bridge errors → `BridgeError` → `toDataException()`.
3. **Repository**: auth guard, constraint checks with `logWarning`, `retryWhen` on listeners, timeout on create-style writes. Let unexpected errors propagate.
4. **ViewModel**:
   - every action through `launchSafely(crashLogger, "<context>", onError = { set error state })`
   - every stream through `.catchAndRecord(crashLogger, "<context>") { emit(Failed) }`, `retry()` via `MutableStateFlow<Int>` + `flatMapLatest`
   - error state fields + `onErrorShown()` to clear transient ones
5. **UI**: blocking → `FallbackState(title, description, lottiePath = "files/lottie_error.json", cta = CTA(retry))`; transient → snackbar + `onErrorShown()`. Strings EN + HR, never raw exception text.
6. **Tests**: for each new error path, a test with a throwing fake asserting the resulting state and that `FakeCrashLogger` recorded it.
7. **Verify**: ktlintCheck, testAndroidHostTest, assembleDebug. Report the failure map with what changed.

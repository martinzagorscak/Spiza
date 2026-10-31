# Error handling rules

The app is Firestore-backed and works offline; errors must never crash the app and never fail silently.
Everything below is built on four helpers — use them instead of hand-written try/catch:

| Helper | Where | Use for |
|---|---|---|
| `ViewModel.launchSafely(crashLogger, context, onError) { }` | `ui/screens/common/LaunchSafely.kt` | **every** ViewModel action (never call `viewModelScope.launch` directly) |
| `Flow.catchAndRecord(crashLogger, context) { emit(fallback) }` | `crash/ErrorRecording.kt` | every observed stream (ViewModel `combine`/`stateIn`, repository listeners) |
| `CrashLogger.recordError(context, error, expected)` | `crash/ErrorRecording.kt` | logging inside a manual `try/catch` (rethrow `CancellationException` first) |
| `DataException(reason)` + `Throwable.dataErrorReason()` | `crash/DataException.kt` | classifying failures as `Offline`, `PermissionDenied`, `NotFound`, `Unknown` |

`recordError` logs expected reasons as a warning and everything else as a non-fatal exception that keeps the original as cause.
`ExpectedErrors.writes` = `{Offline}` (default for `launchSafely`): a rejected write is a bug or a rules mismatch.
`ExpectedErrors.streams` = `{Offline, PermissionDenied}` (default for `catchAndRecord`): listeners lose access when membership changes.

## 1. Data sources (Firestore / Swift bridges / Room)
- **Suspend calls throw** on failure; **Flows fail with an exception** on listener errors. Data sources do not catch-and-swallow.
- Android: let `FirebaseFirestoreException` propagate — `dataErrorReason()` classifies it by code.
- iOS: bridges report `BridgeError?` (reason mapped from the Firestore code by `Error.toBridgeError()` in `SwiftHouseholdBridge.swift`).
  `Ios<X>DataSource` turns it into `error.toDataException()` in `suspendCancellableCoroutine` / `close(...)` in `callbackFlow`
  (always `awaitClose { handle.remove() }`). New Swift bridge methods must use `BridgeError`, never a plain message string.
- Multi-document changes are one `batch()` (both platforms) so they never half-succeed. Firestore never deletes subcollections — delete them in the same batch.
- Skip malformed documents instead of failing the snapshot, but log them.

## 2. Repositories
- Guard auth first: `authService.currentUser?.uid ?: recordNonAuthenticatedError("...").let { return <neutral value> }`.
- Enforce `ConstraintsManager` limits and `crashLogger.logWarning(...)` when a limit blocks an action (expected, not an exception).
- Long-lived listeners: `.retryWhen { cause, attempt -> logWarning(...); (attempt < RETRY_COUNT).also { if (it) delay(...) } }`.
- Writes that Firestore only completes on server ack (create/update flows): `withTimeoutOrNull(...)` — offline the write is applied locally and synced later, so a timeout is not a failure.
- Secondary data (profile save, member profiles, join key) must not fail the primary stream: catch it with `recordError`/`catchAndRecord` and fall back.
- Otherwise let unexpected exceptions propagate to the ViewModel.

## 3. ViewModels (the boundary)
```kotlin
override fun onSave() {
    launchSafely(
        crashLogger = crashLogger,
        context = "Saving X failed",                       // English, no user content (names, item text, keys)
        onError = { state.update { it.copy(hasError = true) } },
    ) {
        saveX(...)
    }
}

init {
    combine(observeA(), observeB()) { a, b -> state.update { ... } }
        .catchAndRecord(crashLogger = crashLogger, context = "X stream failed") { /* emit or set error state */ }
        .launchIn(viewModelScope)
}
```
- A screen with several actions wraps `launchSafely` in a private `launchAction(context) { }` that sets its error state (see `SettingsViewModelImpl`).
- Blocking stream failures emit an error state with retry (`retryTrigger: MutableStateFlow<Int>` + `flatMapLatest`, see `ShoppingListViewModelImpl`).
- Never catch `Throwable` in coroutines; never `runCatching` around suspend calls without rethrowing `CancellationException`.

## 4. UI
- Blocking failures → state `Error` → `FallbackState` with the lottie error and a retry CTA.
- Non-blocking failures → transient flag/enum in state → snackbar → `onErrorShown()` resets it. Use `Res.string.error_generic` unless a specific message helps.
- Expected outcomes (`user not found`, `limit reached`, cancelled sign-in) are explicit state, not exceptions; a user-cancelled action shows nothing.
- Never show `exception.message` to the user — always a localized string in EN and HR.

## Never
- Empty `catch {}` blocks, `printStackTrace()`, or `Flow.catch {}` that neither logs nor emits a state.
- Logging PII: user ids and household ids are OK; emails, names, item text and join keys are not.

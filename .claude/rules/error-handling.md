# Error handling rules

The app is Firestore-backed and works offline; errors must never crash the app and never fail silently.
Follow the layered contract below.

## 1. Data sources (Firestore / Swift bridges / Room)
- **Suspend calls throw** on failure; **Flows fail with an exception** on listener errors (documented on `HouseholdDataSource`). Data sources do not catch-and-swallow.
- iOS: bridges report `errorMessage: String?` in callbacks; `Ios<X>DataSource` converts them with
  `continuation.resumeWithException(IllegalStateException(error ?: "<Operation> failed"))` inside `suspendCancellableCoroutine`,
  and `close(IllegalStateException(message))` inside `callbackFlow` (always `awaitClose { handle.remove() }`).

## 2. Repositories
- Guard auth first: `authService.currentUser?.uid ?: recordNonAuthenticatedError("...").let { return <neutral value> }`.
- Enforce `ConstraintsManager` limits and `crashLogger.logWarning(...)` when a limit blocks an action (this is expected, not an exception).
- Long-lived listeners: `.retryWhen { cause, attempt -> logWarning(...); (attempt < RETRY_COUNT).also { if (it) delay(...) } }`.
- Writes that Firestore only completes on server ack (create flows): wrap in `withTimeoutOrNull(TIMEOUT.milliseconds)` and return `null` on timeout.
- Let unexpected exceptions propagate to the ViewModel — the repository is not the place to decide what the user sees.

## 3. ViewModels (the boundary)
- Every write goes through a guarded launcher like `ShoppingListViewModelImpl.write { }`:
  ```kotlin
  viewModelScope.launch {
      try { block() }
      catch (cancellation: CancellationException) { throw cancellation }   // ALWAYS rethrow
      catch (error: Exception) {
          crashLogger.recordException(Throwable("<Screen> <action> failed; ${error.message}", error))  // keep the original as cause
          _state.update { it.copy(errorState = ErrorState.WriteError) }
      }
  }
  ```
- Every observed stream ends with `.catch { crashLogger.recordException(...); emit(<Failed state>) }` and the screen offers retry
  (pattern: `retryTrigger: MutableStateFlow<Int>` + `flatMapLatest`).
- Never catch `Throwable` broadly in coroutines without rethrowing `CancellationException`. Never use `runCatching` around suspend calls
  unless you rethrow cancellation (`.onFailure { if (it is CancellationException) throw it }`).
- Pass the original exception as `cause` (not `error.cause`) so Crashlytics keeps the real stack trace.
- Do not log PII: user ids are OK, emails, names and item text are not.

## 4. UI
- Blocking failures → state `Error` → `FallbackState` with retry CTA.
- Non-blocking failures (a single write) → transient error field → snackbar/toast → `onErrorShown()` resets it.
- Expected outcomes (`user not found`, `limit reached`) are modelled as explicit state, not as exceptions.

## Never
- Empty `catch {}` blocks. `e.printStackTrace()`. Swallowing errors in `Flow.catch {}` without emitting a state.
- Showing raw `exception.message` to the user — always a localized string resource.

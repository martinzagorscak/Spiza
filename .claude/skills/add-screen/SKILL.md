---
name: add-screen
description: Add a new Spiza screen with Route, ViewModel (abstract + Impl), state, callbacks, Koin registration, AppNavigation entry and EN/HR strings. Use when a feature needs a new destination.
---

# Add a screen

Input: `$ARGUMENTS` — screen name (e.g. `PantryDetails`) and purpose. Package: `ui/screens/<camelCaseFeature>/`.

1. **State + API** — `<X>ViewModel.kt`:
   ```kotlin
   data class <X>State(val isLoading: Boolean = true, /* ... */)
   abstract class <X>ViewModel : ViewModel() {
       abstract fun state(): StateFlow<<X>State>
       abstract fun onSomethingClick()
   }
   ```
   Use a sealed state (`Loading / Error / Content`) if the screen loads remote data.
2. **Impl** — `<X>ViewModelImpl.kt`: `internal class <X>ViewModelImpl(private val observeY: ObserveYUseCase, private val crashLogger: CrashLogger, ...) : <X>ViewModel()`.
   `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), <X>State(isLoading = true))` or private `MutableStateFlow` + `asStateFlow()`.
3. **Screen** — `<X>Screen.kt`: stateless `@Composable fun <X>Screen(state, callbacks, modifier = Modifier)` using `Scaffold` + `TopBar` (back `IconButton` with `Res.drawable.ic_back`), design-system components and spacing tokens; `data class <X>ScreenCallbacks(...)` at the bottom.
4. **DI** — `ui/screens/di/ViewModelModule.kt`: `viewModel<<X>ViewModel> { <X>ViewModelImpl(observeY = get(), crashLogger = get()) }` (use `(param: Type) ->` for arguments).
5. **Route** — `navigation/Route.kt`: `@Serializable data object <X> : Route` (or `data class` with primitive args) **and** `subclass(Route.<X>::class)` in `routeSerializersModule`.
6. **Navigation** — `navigation/AppNavigation.kt`: `entry<Route.<X>> { ... koinViewModel<...>() ... collectAsStateWithLifecycle() ... remember { <X>ScreenCallbacks(onBackClick = { performHapticFeedback(hapticFeedback); backStack.popIfTop(Route.<X>) }, ...) } }`, and a `backStack.add(Route.<X>)` from the entry point screen's callbacks.
7. **Strings** — `<x_snake>_title` etc. in both `values/strings.xml` and `values-hr/strings.xml`.
8. **Verify** — ktlintFormat, ktlintCheck, testAndroidHostTest, assembleDebug. Add ViewModel tests via `/write-unit-tests` if it has logic.

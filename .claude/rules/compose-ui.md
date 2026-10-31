---
paths:
  - "shared/src/commonMain/kotlin/com/mzagorscak/spiza/ui/**"
  - "shared/src/commonMain/kotlin/com/mzagorscak/spiza/navigation/**"
  - "shared/src/commonMain/composeResources/**"
---
# Compose UI and ViewModel contract

## ViewModel files
- `<X>ViewModel.kt` contains the state type(s) and `abstract class <X>ViewModel : ViewModel()` exposing
  `abstract fun state(): StateFlow<<X>State>` plus one `abstract fun` per user intent (`onNameChange`, `retry`, `dismissDialog`, `onErrorShown`...).
- `<X>ViewModelImpl.kt` contains `internal class <X>ViewModelImpl(...) : <X>ViewModel()`; dependencies are use cases (+ `CrashLogger`).
- State is an immutable `data class` with defaults, or a `sealed class` when the screen has distinct modes (see `ShoppingListState.Body`: `Loading`, `Error`, `NoHousehold`, `Content`).
- Dialogs are modelled as state (`DialogState` enum / sealed) owned by the ViewModel, unless purely local and trivial (e.g. a delete-confirm flag in the screen).
- Transient errors are a state field (`ContentErrorState.WriteError`) that the UI shows once and clears via `onErrorShown()`.
- Derive state with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)` or a private `MutableStateFlow` + `asStateFlow()`.
- Launch work in `viewModelScope`. Never expose `MutableStateFlow`.

## Screen composables
- `@Composable fun <X>Screen(state: <X>State, callbacks: <X>ScreenCallbacks, modifier: Modifier = Modifier)` — stateless, no ViewModel, no Koin inside.
- `data class <X>ScreenCallbacks(val onBackClick: () -> Unit, ...)` declared at the bottom of the screen file.
- The ViewModel, `collectAsStateWithLifecycle()`, `remember { Callbacks(...) }` and `performHapticFeedback(hapticFeedback)` calls are wired in `AppNavigation.kt`.
- Large screens are split into files in the same package (`ShoppingListTopBar.kt`, `ShoppingListContent.kt`, ...).

## Design system
- Reuse `ui/components` (`Button` + `ButtonAppearance`, `IconButton`, `TopBar`, `Dialog` + `CTA`, `FallbackState`, `LoadingIndicator`, `TextField`, ...) before writing new UI. New generic components go in `ui/components`.
- Colors from `MaterialTheme.colorScheme`, typography from `MaterialTheme.typography`, spacing from `ui/theme/Spacing.kt` tokens (`padding200`, `padding400`, ...). No hard-coded colors or dp spacing in screens (a file-private `val XSize = 160.dp` for a specific size is fine).
- Error/empty states use `FallbackState` (with `lottie_error.json` for errors and a retry `CTA`).
- Components get light + dark `@Preview` wrapped in `TheAppTheme { }` / `TheAppTheme(darkTheme = true) { }`.

## Strings & resources
- `stringResource(Res.string.<key>)` only — no literals in UI. Keys are `snake_case`, prefixed by screen (`loyalty_card_details_delete_button`); shared words go in the "General terminology" block.
- Add every key to **both** `composeResources/values/strings.xml` and `composeResources/values-hr/strings.xml` (proper Croatian, not a copy of English).
- Icons are vector drawables in `composeResources/drawable/ic_<name>.xml`.

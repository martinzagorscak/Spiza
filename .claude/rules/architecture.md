# Architecture rules

## Layers and dependency direction
- `ui` may depend on `domain`. `data` may depend on `domain`. `domain` depends on nothing project-internal except its own packages
  (plus `kotlinx.coroutines`). No Compose, Firebase, Room, Koin or platform types in `domain/`.
- ViewModels talk to **use cases only** — never to repositories, data sources or DAOs directly.
  (Existing exception: `AddLoyaltyCardViewModelImpl` takes `PermissionRepository` because it needs a `PermissionsController` parameter. Do not copy this pattern unless the same constraint applies.)
- `CrashLogger` may be injected into ViewModels and repositories.

## Use cases
- One file per feature area: `domain/usecase/<Feature>UseCases.kt`.
- Each use case is an `interface` with `operator fun invoke(...)` (or `suspend operator fun`) plus a `class <Name>UseCaseImpl` in the same file.
- Naming: `Observe<X>UseCase` returns `Flow`, `Get<X>UseCase` is a one-shot read, `Set/Add/Update/Delete/Move/Clear<X>UseCase` are writes,
  `Can<X>UseCase` are constraint checks.
- Business rules (trimming, dedup, ordering, limits) belong in use cases / `ConstraintsManager`, not in ViewModels or composables.
- Register every use case in `domain/di/DomainModule.kt` under the matching `// <Feature> Use Cases` comment, as `single<Interface> { Impl(repository = get()) }`.

## Repositories and data sources
- Interface in `domain/repository/<X>Repository.kt`, implementation `internal class <X>RepositoryImpl` in `data/<feature>/`.
- Remote data: `data/<feature>/<X>DataSource.kt` interface in commonMain; `Firestore<X>DataSource` in androidMain; `Ios<X>DataSource` in iosMain wrapping an `Ios<X>Bridge` that Swift implements.
- Firestore field/collection names live in a `<X>Fields` object next to the data source interface and are shared with Swift via `<X>Fields.shared`.
- Local data: Room `@Dao` + `<X>Entity` in `data/<feature>/local` and `data/<feature>/model`; DAOs are exposed from `SpizaDatabase`.
- DTOs/Entities never leave the data layer: map with `toDomain()` / `toDomainModel()` / `toEntity()` extension functions.
- Register in `data/di/DataModule.kt` (`single<Interface> { Impl(...) }`, named arguments).

## UI
- Domain models are mapped to `ui/model/Presentable<X>` with `toPresentable()` before they reach state.
- See `compose-ui.md` for the screen / ViewModel contract.

## DI
- Koin modules: `dataModule`, `domainModule`, `viewModelModule`, `platformModule()` (expect/actual) — all included by `appModule`.
- Use named arguments in every Koin factory lambda: `FooImpl(repository = get(), crashLogger = get())`.
- Parameterised ViewModels use `viewModel<X> { (param: Type) -> XImpl(param = param, ...) }` and `koinViewModel<X>(parameters = { parametersOf(value) })`.

## Navigation (Navigation3)
- Add new destinations to `sealed interface Route` as `@Serializable data object` / `data class` (primitive args only, e.g. ids).
- Register each new route in `routeSerializersModule` — otherwise back-stack save/restore crashes.
- Add an `entry<Route.X>` in `AppNavigation.kt`; pop with `backStack.popIfTop(...)`, push with `backStack.add(...)`.

---
paths:
  - "shared/src/androidMain/**"
  - "shared/src/iosMain/**"
  - "iosApp/**"
  - "androidApp/**"
  - "**/platformDI/**"
---
# Platform code (Android / iOS)

- Prefer commonMain. Go platform-specific only for SDKs that have no KMP API here (Firebase Auth/Firestore/Crashlytics, credentials, device settings, scanner).
- Pattern A — **expect/actual** for small platform hooks (`Platform`, `AppLocale`, `StatusBarAppearance`, `DeviceSettingsHelpers`, `PlatformScannerContainer`, `platformModule()`, `SpizaDatabaseConstructor`). File naming: `X.kt` (expect) + `X.android.kt` / `X.ios.kt`.
- Pattern B — **interface + platform impl via Koin** for data/services: interface in commonMain, `Android*/Firestore*` in androidMain, `Ios*` in iosMain, bound in each `platformDI/PlatformModule.kt`.
- iOS Firebase lives in **Swift**: Kotlin declares `interface Ios<X>Bridge` (callback-based: `onComplete(result?, error: BridgeError?)`, listeners return `IosListenerHandle`, one handle per call),
  Swift implements `Swift<X>Bridge: NSObject, Ios<X>Bridge` in `iosApp/iosApp/`, and it is passed into `InitKoinIosKt.doInitKoinIos(...)` in `iOSApp.swift`.
  Adding a bridge method = change the Kotlin interface + the Swift class + `Ios<X>DataSource` in the same change.
- Kotlin types crossing to Swift: use primitives, `String`, `List`, DTO data classes. `Boolean` arrives as `KotlinBoolean`, objects as `<X>.shared`.
  Avoid default arguments, generics, `suspend` and `Flow` in bridge interfaces.
- Android and iOS implementations must behave identically (same field names from `<X>Fields`, same error semantics, same key generation format).
- You cannot build or run the Xcode project from the CLI here; `./gradlew :shared:iosSimulatorArm64Test` compiles the Kotlin iOS targets. Tell the user explicitly when Swift changes need to be verified in Xcode.
- Firestore document shape and permissions are defined by `firestore.rules` in the repo root. Read it before writing Firestore code and update it in the same change when collections, fields or write paths change.

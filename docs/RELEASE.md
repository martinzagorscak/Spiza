# Release guide

Handbook and open to-dos for publishing Spiza. **Status: not published yet. Android (Google Play) goes first; iOS comes later.**

How to use this file:
- Each checklist item is a to-do. Tick it (`[x]`) when it is done and keep it, so the next release can see what was set up and where.
- Code-level to-dos are tagged in the source so they can be found with grep: `TODO(ios-release)` and `#ImplementAppleAuthProvider`.
- Items marked *(recommended)* are not store requirements, but should be in place before real users depend on the app.

---

## 1. Both platforms

### Accounts, legal and policies
- [ ] **Privacy policy** published at a public URL (data collected: name, email, profile photo, households, shopping items,
      loyalty card numbers, crash reports; processors: Firebase/Google; how to request deletion). Both stores require the URL,
      and GDPR applies because users are in the EU.
- [x] **In-app account deletion** (Settings → Delete account). Both stores require it for apps that let users create an account.
      After a confirmation dialog and a fresh Google sign-in (Firebase needs a recent login), the app deletes, in this order:
      the user's `joinKeys`, households they own (with their `items`, also for the other members), their membership in the
      other households, the `users/{uid}` profile, the Firebase Auth user, and finally all local data (Room, settings).
      Code: `AccountRepositoryImpl`; rules: `allow delete` on `users/{uid}` (covered by `firestore-tests/test/accountDeletion.test.ts`).
- [ ] **Account deletion web page / instructions** at a public URL. Google Play requires the URL in the Data safety form
      (users must be able to request deletion without reinstalling the app); useful for App Store review too. The page must:
      - name the app and developer as on the store listing;
      - list the in-app steps (Settings → Delete account → confirm → sign in with Google again);
      - offer a way to request deletion without the app (e.g. email to the support address from the signed-in Google email);
      - say what is deleted (profile, name, email, photo, owned households and their items, household memberships, join keys,
        local data) and what is kept and for how long (Crashlytics crash reports, retained by Firebase for 90 days).
      For email requests, delete manually in the Firebase console in the same order as the app: the user's `joinKeys`, owned
      `households` (with `items`), the uid from other households' `joinedMembers`, `users/{uid}`, then the user in Authentication.
- [ ] **Support contact**: support email (and optionally a simple website) for the store listings.

### Firebase / backend
- [ ] **`firestore.rules` deployed** and matching the repo (`cd firestore-tests && npm test` green before deploying).
- [ ] **Remote Config parameters** created for each published platform (see section 5), default value `0`.
- [ ] **Firebase Crashlytics** receives a test crash from a release build of each platform.
- [ ] **Budget alerts** in Google Cloud billing (if the project is on the Blaze plan) so a bug or abuse cannot run up costs. *(recommended)*
- [ ] **Firebase App Check** (Play Integrity on Android, App Attest on iOS) so only the real app can call Firestore. *(recommended)*
- [ ] **Firestore backups**: scheduled exports or point-in-time recovery (Blaze plan). *(recommended)*

### App quality
- [ ] Definition of done green on the release commit: `ktlintCheck`, `:shared:testAndroidHostTest`, `:shared:iosSimulatorArm64Test`,
      device tests (`:shared:connectedAndroidDeviceTest`).
- [ ] Manual smoke test of a **release-like build** (Android `qa` build type: minified, non-debuggable): sign in, create and join a
      household, add, check, move and delete items, loyalty card scan, offline → online sync, sign out, delete account
      (with a test account: confirm with the right Google account, a different account, cancel, offline, back while deleting).

---

## 2. Android (Google Play)

### One-time setup
- [ ] **Google Play developer account** (one-time fee; identity verification can take days).
- [ ] **Closed testing first.** New *personal* developer accounts must run a closed test with at least 12 testers for 14 continuous
      days before production access is granted. Plan this time in.
- [ ] **Upload key + Play App Signing.** Create an upload keystore, store it and its passwords outside the repo (password manager plus
      a backup). Add a `release` `signingConfig` that reads the keystore path and passwords from `local.properties` or environment
      variables — never commit the keystore or passwords.
- [ ] **SHA-1 / SHA-256 fingerprints in Firebase.** Add the *upload key* and the *Play App Signing key* fingerprints (Play Console →
      Test and release → App integrity) to the Android app in Firebase console, then download the new `google-services.json`.
      Without this, **Google Sign-In fails in builds installed from Play** even though it works in debug.
- [ ] **Restrict the Android API key** in Google Cloud console to the package name + SHA-1 fingerprints. *(recommended)*
- [ ] **`android:allowBackup`** is currently `true`: decide whether Android Auto Backup may copy local data (Room, settings) to the
      user's Google Drive; if not, set it to `false` or add `dataExtractionRules`. *(recommended)*

### Play Console forms
- [ ] Store listing: app name, short and full description (EN + HR), icon 512×512, feature graphic 1024×500, phone screenshots.
- [ ] **Data safety** form (matches the privacy policy: account info, user content, crash logs; data encrypted in transit; deletion available).
- [ ] **App access**: sign-in is required, so give reviewers instructions and a test Google account.
- [ ] Content rating questionnaire, target audience (not for children), ads declaration (no ads), privacy policy URL,
      account deletion URL.
- [ ] Camera permission: used only for scanning loyalty cards (already optional via `uses-feature required=false`).

### Already in place
- [x] R8 minification and resource shrinking for `release` and `qa` builds.
- [x] Crashlytics Gradle plugin (uploads R8 mapping files for release builds so stack traces are readable).
- [x] Versioning: `versionCode = major * 10_000 + minor * 100 + patch` in `androidApp/build.gradle.kts`.

---

## 3. iOS (App Store)

### One-time setup
- [ ] **Apple Developer Program** membership (yearly fee).
- [ ] **Sign in with Apple** (App Review guideline 4.8: an app that offers Google sign-in must also offer an equivalent
      privacy-focused login, in practice Sign in with Apple). Enable the Apple provider in Firebase Auth, add the
      *Sign in with Apple* capability in Xcode, implement it in `SwiftAuthBridge.swift` and show the button on the login screen
      (search `#ImplementAppleAuthProvider`). On account deletion, Apple also requires **revoking the Apple token**
      (`Auth.auth().revokeToken(withAuthorizationCode:)`).
- [ ] **Build the account deletion bridges in Xcode** (written without a Swift compiler): `SwiftAuthBridge` (`reauthenticate`,
      `deleteCurrentUser`, user-mismatch mapping), `SwiftHouseholdBridge.getHouseholds`, `SwiftUserBridge.deleteUserProfile`;
      then check Google sign-in still works and run the delete account smoke test on a device.
- [ ] **Bundle identifier**: `Config.xcconfig` builds it as `com.mzagorscak.spiza.Spiza$(TEAM_ID)`. Decide the final id, create it in
      App Store Connect and make sure the iOS app in Firebase (and `GoogleService-Info.plist`) uses the same id.
- [ ] **App Store ID for the update screen (release-blocking).** Until it is set, the "Update" button does nothing (it only
      logs a warning), so a required update would leave iOS users stuck. The link `https://apps.apple.com/app/id<APP_ID>` needs
      Apple's numeric app id (App Store Connect → Apps → Spiza → App Information → *Apple ID*). Replace the placeholder marked
      `TODO(ios-release)` in `iosMain`.
- [ ] **Remote Config for the iOS app**: create `ios_min_supported_version` and `ios_recommended_version` (section 5).
      Until they exist, iOS uses the in-app defaults (`0` = never prompt).
- [ ] **`FirebaseRemoteConfig` linked**: open the project in Xcode, let packages resolve and build; check the product is listed under
      Target → General → Frameworks, Libraries, and Embedded Content.
- [ ] **Privacy manifest** `PrivacyInfo.xcprivacy` for the app: declare *required reason APIs* used by the app itself
      (e.g. `UserDefaults` through multiplatform-settings) and the collected data types. Firebase SDKs ship their own manifests.
- [ ] **Export compliance**: add `ITSAppUsesNonExemptEncryption = NO` to `Info.plist` (the app only uses standard HTTPS), so every
      upload does not ask about encryption.
- [ ] **Crashlytics symbols for Kotlin**: the Crashlytics run script uploads dSYMs; check that crashes inside the `shared` framework
      are symbolicated in a TestFlight build (the Kotlin framework dSYM must be uploaded too).
- [ ] Localize `NSCameraUsageDescription` into Croatian (`InfoPlist.strings`). *(recommended)*

### App Store Connect forms
- [ ] App information: name, subtitle, description and keywords (EN + HR), category, support URL, privacy policy URL.
- [ ] **App Privacy** ("nutrition labels"): contact info, user content, identifiers, crash and diagnostics data; also cover
      Firebase Analytics, which the iOS target links.
- [ ] Screenshots: iPhone 6.9" (or 6.5") and **iPad**, because the app targets iPad as well (`TARGETED_DEVICE_FAMILY = 1,2`).
      Either provide iPad screenshots and test the layout on iPad, or limit the app to iPhone.
- [ ] **App Review notes**: sign-in is required, so give reviewers a demo account or clear instructions.
- [ ] Age rating questionnaire.
- [ ] TestFlight round with real devices before submitting for review.

### Already in place
- [x] Crashlytics dSYM upload run script in the Xcode build phases.
- [x] `NSCameraUsageDescription` (English).
- [x] `MARKETING_VERSION` uses `major.minor.patch` (`iosApp/Configuration/Config.xcconfig`); the update check parses it.

---

## 4. Every release (runbook)

1. Merge everything into `main`; definition of done green (section 1, *App quality*).
2. Bump the version: Android `major/minor/patchVersion` in `androidApp/build.gradle.kts`; iOS `MARKETING_VERSION` (same number)
   and `CURRENT_PROJECT_VERSION` (+1 per upload) in `Config.xcconfig`.
3. If the release needs backend changes (`firestore.rules`, new fields), check that the **currently published** app versions keep
   working with them. If not: ship the app first, raise `android_min_supported_version` / `ios_min_supported_version`, then deploy the backend change.
4. Build: Android `./gradlew :androidApp:bundleRelease` (AAB); iOS Xcode → Product → Archive → upload.
5. Smoke test the build from the internal testing track / TestFlight.
6. Roll out gradually (Play staged rollout, e.g. 10 % → 50 % → 100 %; App Store phased release) and watch Crashlytics.
7. Tag the release commit in git (`v1.2.3`).

---

## 5. Forcing or recommending an update (Remote Config)

Each platform has its own parameters, so a value meant for Android can never block iOS users (and vice versa).
Values are numbers encoded as `major * 10_000 + minor * 100 + patch` (1.2.3 → `10203`):

| Parameter | Effect |
|---|---|
| `android_min_supported_version` / `ios_min_supported_version` | Older installs see a blocking "update required" screen. |
| `android_recommended_version` / `ios_recommended_version` | Older installs see a dismissible "update available" dialog; "Later" hides it until a newer recommended version. |

1. Raise a value only after that version is approved **and** rolled out to 100 % in the store of that platform.
2. Set the parameter's own value in the Remote Config **Parameters** tab and click **Publish changes**. Do not use a *Rollout*:
   a rollout serves its value gradually and only while it is running, which is the opposite of a hard minimum.
3. Create the parameters with data type **Number** and enter plain numbers (`10203`, not `1.2.3`). The SDK reads a non-numeric
   value as `0` without any error, so a typo silently enforces nothing.
4. The app checks on every start (from the background too). Release builds cache Remote Config for up to 1 hour, so a change can
   take that long to arrive; debug builds fetch on every check.
5. If the config cannot be fetched (offline, error), the app uses the last values it fetched successfully; on a fresh install
   without any fetched values it never blocks the user.
6. Breaking backend changes (e.g. tighter `firestore.rules`): release the compatible app → raise the min supported version → deploy the rules.

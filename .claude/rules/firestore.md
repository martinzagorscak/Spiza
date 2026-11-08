---
paths:
  - "firestore.rules"
  - "firebase.json"
  - "firestore-tests/**"
  - "shared/src/androidMain/kotlin/com/mzagorscak/spiza/data/**/Firestore*.kt"
  - "iosApp/iosApp/Swift*Bridge.swift"
---
# Firestore rules and their tests

- Any change to a collection, field, document shape or who may write it changes `firestore.rules`, the Android `Firestore*DataSource`
  and the Swift bridge in the same change (see CLAUDE.md), **and** the emulator tests in `firestore-tests/` — follow `/firestore-rules-tests`.
- Tests write documents exactly as the app does (same batch, `arrayUnion` / `arrayRemove`, same queries) — a rule that allows `get`
  but not the app's query breaks the app while a `getDoc` test still passes.
- Finish with `cd firestore-tests && npm test` green and a note to the user that the rules must be deployed.

---
name: firestore-rules-tests
description: Writes and runs the Firestore security rules tests in firestore-tests/ against the Firestore emulator. Use whenever firestore.rules, a Firestore collection, document field or write permission changes, or when asked to verify who may read or write household, item, user or join key documents.
---

# Firestore rules tests

`firestore.rules` decides who may read and write other people's households — a mistake is a **security bug** no Kotlin test can catch.
Tests live in `firestore-tests/` (Node, Vitest, `@firebase/rules-unit-testing`) and run against the local Firestore emulator,
never against the real project (`demo-spiza` is an offline demo project id).

Prerequisites: Node ≥ 24.12 and Java (for the emulator). First time: `cd firestore-tests && npm install`.

## Steps

Copy this checklist and tick it off:
```
- [ ] 1. Change firestore.rules (and the Kotlin/Swift data sources in the same change, see CLAUDE.md)
- [ ] 2. For every rule you added or changed: one allowed test + one rejected test per caller that must be refused
- [ ] 3. npm test passes
- [ ] 4. Tell the user to deploy the rules
```

1. **Files:** one test file per collection group — `households.test.ts`, `items.test.ts`, `usersAndJoinKeys.test.ts`.
   Shared helpers and ids (`OWNER`, `MEMBER`, `STRANGER`, `household()`, `item()`, `seed()`, `asUser()`, `asSignedOut()`) are in `test/setup.ts`.
2. **What to test** for each `allow` rule:
   - the caller who **is** allowed succeeds (`assertSucceeds`) with a document exactly as the app writes it (Android and iOS — iOS omits null fields);
   - every caller who must be refused fails (`assertFails`): signed out, stranger, member vs owner, someone acting for another user;
   - every `keys().hasOnly` / type check: an unknown field and a wrong type fail;
   - multi-document writes the app does in one batch (e.g. delete items + household) succeed as a batch.
3. **Structure:** `describe("<collection>: <operation>")`; allowed cases first, then `// region Error handling` … `// endregion` with rejections
   (same convention as the Kotlin tests). Arrange state with `seed(env, ...)` (rules disabled); `beforeEach` clears the emulator.
4. **Run:** `cd firestore-tests && npm test` (starts the emulator, runs Vitest, stops it). Watch mode: `npm run test:watch`.
   If a test fails, fix the rule or the test and repeat — only finish when everything passes.
5. **Report** the rules covered and remind the user: the rules must be deployed (Firebase console > Firestore > Rules, or
   `firebase deploy --only firestore:rules`); old app versions keep using whatever is deployed.

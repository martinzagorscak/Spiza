---
name: write-unit-tests
description: Writes and runs unit tests for Spiza shared code (ViewModel expected behaviour and error paths, use cases, constraints, mappers/ordering, Firestore repositories with fakes, Room repositories on a real in-memory database). Use when asked to test something, add coverage, or write a regression test.
---

# Write unit tests

Target: `$ARGUMENTS` (class, file, feature, or "uncommitted changes" → use `git diff --name-only`).
For larger scopes delegate to the `test-writer` agent.

Read first:
- `.claude/rules/testing.md` — tools, what is tested with what, structure, how much to test
- `templates.md` (this folder) — copy-paste template per layer
- `fakes.md` (this folder) — before creating or changing a fake

## Steps

1. **Pick the layer and the test double** with the table "What is tested with what" in `testing.md`
   (ViewModel → fake repositories; Firestore repository → fake data source; Room repository/DAO → real in-memory DB in `databaseTest`).
2. **List the behaviours**, in this order:
   1. **Expected behaviour** — every public action / method / stream with its own logic: the successful outcome (resulting state,
      the call it makes and with which arguments, the value it returns or emits, follow-up UI state). Mandatory, at least one test each.
   2. Each distinct error branch (data source throws, stream fails, unauthenticated, timeout, quota).
   3. Edge cases (blank/whitespace, duplicates, N-1/N/N+1) **only where the code branches on them**.
   Cut groups 2–3 with "How much to test" (`testing.md` §6). Skip pass-throughs and anything an existing test already proves.
3. **Fakes:** reuse `commonTest/.../fakes/`; extend additively following `fakes.md`. Never fake a Room DAO.
4. **Write the class** from `templates.md`: expected-behaviour tests first in public-interface order, then
   `// region Error handling` … `// endregion`. Assertions with AssertK; flow emissions with Turbine.
5. **Check each test can fail:** for a regression test, confirm it fails without the fix; for others, make sure the assertion would change
   if the behaviour broke (no assertions on values the fake itself produced).
6. **Run:** `./gradlew ktlintFormat ktlintCheck :shared:testAndroidHostTest`; add `:shared:iosSimulatorArm64Test` when you touched
   `iosTest`, `databaseTest`, Room or iOS code. Optionally `./gradlew :shared:koverHtmlReport` to find untested branches.
7. **Report** the tests added (file → cases), what was skipped and why, and any bug discovered. Do not silently fix production code —
   a test exposing a bug is kept with `@Ignore` + `// BUG: ...` and reported.

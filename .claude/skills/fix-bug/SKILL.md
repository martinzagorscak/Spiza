---
name: fix-bug
description: Fix a Spiza bug with a disciplined loop — understand, locate, reproduce with a failing test, minimal fix, verify, check for the same pattern elsewhere. Use for bug reports, crashes, stack traces, or wrong behaviour.
---

# Bug fix workflow

Bug: `$ARGUMENTS`. For complex or cross-platform bugs delegate to the `bug-fixer` agent.

1. **Clarify** expected vs actual, platform (Android / iOS / both), online/offline, signed-in state, steps. Ask only if it changes where you look.
2. **Trace** Screen → `AppNavigation` callback → `<X>ViewModelImpl` → use case → repository → data source (→ `Swift<X>Bridge` on iOS). Use `git log -p -- <file>` on suspects; recent `[Feature]`/`[Fix]` commits are frequent culprits.
3. **Root cause** — state it in one or two sentences with the evidence (file:line). Do not fix symptoms.
4. **Regression test** in commonTest that fails now (see `/write-unit-tests`). Run it and confirm it fails for the right reason. If the bug is UI/platform-only and not unit-testable, write manual repro steps instead and say so.
5. **Minimal fix** at the correct layer. Keep the diff small; no unrelated refactors or reformatting.
6. **Verify**: the regression test passes, `./gradlew :shared:testAndroidHostTest`, `./gradlew ktlintCheck`, `./gradlew :androidApp:assembleDebug` (+ `:shared:iosSimulatorArm64Test` if iOS code changed).
7. **Same pattern elsewhere**: grep for the pattern and list other occurrences — ask before fixing them.
8. Report root cause, fix, test, and propose `[Bugfix] <Area> - <description>`.

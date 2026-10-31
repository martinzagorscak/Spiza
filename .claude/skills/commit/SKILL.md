---
name: commit
description: Verify and commit the current Spiza changes using the project's "[Tag] Area - description" message format. Use only when the user asks to commit.
disable-model-invocation: true
---

# Commit

1. `git status` and `git diff` — make sure only intended files are included; never stage `google-services.json`, `GoogleService-Info.plist`, `local.properties`, `*.log`, `.idea/`, build dirs.
2. Run `./gradlew ktlintCheck` and `./gradlew :shared:testAndroidHostTest`. If either fails, stop and report (do not commit).
3. If the diff mixes unrelated changes, propose splitting it into several commits.
4. Message: `[Tag] Area - short description` — tags `[Feature] [Bugfix] [Fix] [Enhancement] [UI] [Refactor] [Test]`; area = feature/screen name (look at `git log --oneline -20` for wording). Optional body explaining why.
5. `git add <specific files>` (not `git add -A`) and `git commit`. Never push unless explicitly asked.

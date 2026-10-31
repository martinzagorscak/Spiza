# Git & commits

- Commit only when the user asks. Never push, force-push, rebase `main` or amend pushed commits unless explicitly asked.
- Message format (match history): `[Tag] Area - short description`
  - Tags: `[Feature]` new functionality, `[Bugfix]` bug fix, `[Fix]` small correction/follow-up, `[Enhancement]` improvement of existing feature,
    `[UI]` visual-only change, `[Refactor]` no behaviour change, `[Test]` tests only.
  - Area = feature or screen name: `Shopping list`, `Loyalty cards`, `Login`, `HouseholdManagementScreen`, `Suggestion saving`...
  - Example: `[Bugfix] Shopping list - add existing item`
- One logical change per commit. Before committing: `ktlintCheck` + `:shared:testAndroidHostTest` green.
- Never commit `google-services.json`, `GoogleService-Info.plist`, `local.properties`, `*.log`, `.idea/`, build outputs.

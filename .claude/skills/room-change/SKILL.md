---
name: room-change
description: Safely adds or changes a Room entity, DAO or the SpizaDatabase schema (version bump, migration, exported schema, migration test, KSP for all targets). Use whenever an @Entity, @Dao or SpizaDatabase changes.
---

# Room change

Change: `$ARGUMENTS`.

Context: `shared/.../data/local/SpizaDatabase.kt` is the single database (entities: `SuggestionEntity`, `LoyaltyCardEntity`), current `version = 3` with migrations in `SpizaDatabaseMigrations.kt`, `exportSchema = true`. The Room Gradle plugin (`room { schemaDirectory("$projectDir/schemas") }` in `shared/build.gradle.kts`) writes `shared/schemas/com.mzagorscak.spiza.data.local.SpizaDatabase/<version>.json` on every build. Users already have data on device, so **schema changes without a migration crash or wipe data**.

1. Entity in `data/<feature>/model/<X>Entity.kt` with `toDomainModel()` / `toEntity()` mappers; DAO in `data/<feature>/local/<X>Dao.kt` (`Flow` for observe, `suspend` for one-shots).
2. New entity → add to `@Database(entities = [...])` and an `abstract fun <x>Dao()`; expose the DAO in `DataModule` (`single<XDao> { get<SpizaDatabase>().xDao() }`).
3. Any change to an existing entity, a new entity, or a change in what stored values mean (e.g. renamed enum constants — Room reads enums by name and throws on an unknown one):
   - bump `version`
   - add a `Migration(from, to)` with the exact SQL and register it in `getSpizaDatabase()` (`data/local/SpizaDatabaseBuilder.kt`, shared by both platforms) via `.addMigrations(...)` before `.build()`
   - never use `fallbackToDestructiveMigration()` without the user's explicit consent
4. Build once (`./gradlew :shared:iosSimulatorArm64Test` or `:androidApp:assembleDebug`) so the new `<version>.json` is exported, and **commit it** with the change.
   Never edit or regenerate an already committed `<n>.json` — it is the record of what is on users' devices and the migration tests create old databases from it.
   `1.json` and `2.json` were reconstructed from git history (the project exported no schemas before version 3) — they can't be regenerated from the current code.
5. **A migration test is mandatory for every version bump**: add a test to `shared/src/databaseTest/.../data/local/SpizaDatabaseMigrationTest.kt` that
   - creates the database at the old version with `helper.createDatabase(old)` and inserts realistic rows with raw SQL (the old shape, old stored values),
   - runs `helper.runMigrationsAndValidate(version = new, migrations = listOf(MIGRATION_OLD_NEW))` (validates the schema against `<new>.json`),
   - asserts the migrated rows (rewritten values, defaults of new columns, untouched data), expected values written out, not derived from production code.
   Keep `productionDatabase_opensAVersion1Database_...` passing: it migrates from version 1 through `getSpizaDatabase()` and reads through the DAOs, so it catches a migration missing from the production builder.
   The test is written once in `databaseTest` and runs on both platforms (`spizaMigrationTestHelper()`, `spizaDatabaseBuilder()` are expect/actual: iOS reads `shared/schemas` via the `SPIZA_ROOM_SCHEMA_DIR` env var set on the simulator test task, Android reads them from the test APK assets that the Room plugin copies).
6. Room-backed repository and DAO logic: test against the real database (`inMemorySpizaDatabase()`) in `databaseTest` — never a fake DAO (see `.claude/rules/testing.md`).
7. KSP is configured for `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`. Run:
   - `./gradlew :androidApp:assembleDebug` — Android generated code compiles
   - `./gradlew :shared:iosSimulatorArm64Test` — iOS generated code compiles and **runs the migration + database tests** (part of the definition of done)
   - `./gradlew :shared:connectedAndroidDeviceTest` with an emulator/device attached — runs the same tests on Android (compile-only without a device: `./gradlew :shared:assembleAndroidDeviceTest`). If no device is available, say so in the report.
8. Tell the user to also test an upgrade install (old build → new build) on a device.

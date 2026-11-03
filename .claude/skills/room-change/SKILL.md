---
name: room-change
description: Safely add or change a Room entity, DAO or the SpizaDatabase schema (version bump, migration, KSP for all targets). Use whenever an @Entity, @Dao or SpizaDatabase changes.
---

# Room change

Change: `$ARGUMENTS`.

Context: `shared/.../data/suggestion/local/SpizaDatabase.kt` is the single database (entities: `SuggestionEntity`, `LoyaltyCardEntity`), current `version = 2` with migrations in `SpizaDatabaseMigrations.kt`, `exportSchema = false`. Users already have data on device, so **schema changes without a migration crash or wipe data**.

1. Entity in `data/<feature>/model/<X>Entity.kt` with `toDomainModel()` / `toEntity()` mappers; DAO in `data/<feature>/local/<X>Dao.kt` (`Flow` for observe, `suspend` for one-shots).
2. New entity → add to `@Database(entities = [...])` and an `abstract fun <x>Dao()`; expose the DAO in `DataModule` (`single<XDao> { get<SpizaDatabase>().xDao() }`).
3. Any change to an existing entity or a new entity once the app is released:
   - bump `version`
   - add a `Migration(from, to)` with the exact SQL and register it in `getSpizaDatabase()` (`data/suggestion/local/SpizaDatabaseBuilder.kt`, shared by both platforms) via `.addMigrations(...)` before `.build()`
   - never use `fallbackToDestructiveMigration()` without the user's explicit consent
   - recommend switching to `exportSchema = true` + a `room { schemaDirectory(...) }` config so future migrations can be verified — ask before changing build config.
4. KSP is configured for `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64` — run both `./gradlew :androidApp:assembleDebug` and `./gradlew :shared:iosSimulatorArm64Test` to make sure generated code compiles on all targets.
5. Test repository mapping logic with a fake DAO (no real DB in unit tests).
6. Tell the user to test an upgrade install (old build → new build) on a device.

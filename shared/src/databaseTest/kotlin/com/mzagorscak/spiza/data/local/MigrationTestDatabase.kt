package com.mzagorscak.spiza.data.local

import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper

/**
 * Absolute path of a file-backed test database called [name]. Migrations run on real files because an in-memory
 * database is gone once its connection closes, and the helper reopens the database for every step.
 */
expect fun migrationTestDatabasePath(name: String): String

/** Creates databases from the committed schemas in `shared/schemas` and validates migrations against them. */
expect fun spizaMigrationTestHelper(databasePath: String): MigrationTestHelper

/** The platform builder the app uses, pointed at [databasePath]; pass it to [getSpizaDatabase] like production does. */
expect fun spizaDatabaseBuilder(databasePath: String): RoomDatabase.Builder<SpizaDatabase>

/** Removes the database and its WAL/SHM side files, so every test starts from a missing database. */
expect fun deleteDatabaseFiles(databasePath: String)

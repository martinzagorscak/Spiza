package com.mzagorscak.spiza.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.posix.getenv

// Set by the iosSimulatorArm64Test task in shared/build.gradle.kts: the simulator runs on the host file system,
// so the helper reads the committed schemas directly from the project.
private const val SCHEMA_DIRECTORY_ENV = "SPIZA_ROOM_SCHEMA_DIR"

actual fun migrationTestDatabasePath(name: String): String = NSTemporaryDirectory() + name

@OptIn(ExperimentalForeignApi::class)
actual fun spizaMigrationTestHelper(databasePath: String): MigrationTestHelper =
    MigrationTestHelper(
        schemaDirectoryPath = requireNotNull(getenv(SCHEMA_DIRECTORY_ENV)?.toKString()) {
            "$SCHEMA_DIRECTORY_ENV is not set; run the tests through Gradle"
        },
        fileName = databasePath,
        driver = BundledSQLiteDriver(),
        databaseClass = SpizaDatabase::class,
        databaseFactory = { SpizaDatabaseConstructor.initialize() },
    )

actual fun spizaDatabaseBuilder(databasePath: String): RoomDatabase.Builder<SpizaDatabase> =
    Room.databaseBuilder<SpizaDatabase>(name = databasePath)

@OptIn(ExperimentalForeignApi::class)
actual fun deleteDatabaseFiles(databasePath: String) {
    listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
        NSFileManager.defaultManager.removeItemAtPath(path = databasePath + suffix, error = null)
    }
}

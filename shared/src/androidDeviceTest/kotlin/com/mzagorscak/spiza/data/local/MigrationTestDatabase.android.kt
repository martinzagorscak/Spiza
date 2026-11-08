package com.mzagorscak.spiza.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

private val context: Context
    get() = ApplicationProvider.getApplicationContext()

// The databases/ directory does not exist in a fresh test app, and SQLite does not create parent directories.
actual fun migrationTestDatabasePath(name: String): String = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }.absolutePath

// Reads the schemas from the test APK's assets; the Room Gradle plugin copies shared/schemas there.
actual fun spizaMigrationTestHelper(databasePath: String): MigrationTestHelper =
    MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = File(databasePath),
        driver = BundledSQLiteDriver(),
        databaseClass = SpizaDatabase::class,
        databaseFactory = { SpizaDatabaseConstructor.initialize() },
    )

actual fun spizaDatabaseBuilder(databasePath: String): RoomDatabase.Builder<SpizaDatabase> =
    Room.databaseBuilder<SpizaDatabase>(context = context, name = databasePath)

actual fun deleteDatabaseFiles(databasePath: String) {
    listOf("", "-wal", "-shm", "-journal").forEach { suffix -> File(databasePath + suffix).delete() }
}

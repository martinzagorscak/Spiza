package com.mzagorscak.spiza.data.local

/**
 * Real [SpizaDatabase] kept in memory, so DAO and Room-backed repository tests run the actual SQL on every platform.
 * Same driver and query context as production; close it in `@AfterTest`.
 */
expect fun inMemorySpizaDatabase(): SpizaDatabase

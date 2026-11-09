package com.mzagorscak.spiza.data.local

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/** Deletes every row of the local database (the schema stays), so the device holds no data of a deleted account. */
interface LocalDataCleaner {
    suspend fun clearAll()
}

internal class RoomLocalDataCleaner(
    private val database: SpizaDatabase,
) : LocalDataCleaner {
    // Room KMP has no clearAllTables() outside Android. A new table must be cleared here too.
    override suspend fun clearAll() {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.loyaltyCardDao().deleteAll()
                database.suggestionDao().deleteAll()
            }
        }
    }
}

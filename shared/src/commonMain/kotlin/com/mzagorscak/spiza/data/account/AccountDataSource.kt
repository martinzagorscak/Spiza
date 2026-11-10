package com.mzagorscak.spiza.data.account

/** Firestore data of a whole account. Field and collection names are in `HouseholdFields` and `UserFields`. */
interface AccountDataSource {
    /**
     * Reads from the server only (never the cache, so it throws when offline), then deletes in ONE batch: every `joinKeys`
     * document of [userId], every household [userId] owns together with all its `items`, [userId] from `joinedMembers` of the
     * households they only joined, and `users/{userId}`. Either everything is deleted or nothing; throws when a read or the
     * commit fails, or when the owner of one of the households can not be read.
     *
     * Idempotent, so it can be called again. The reads and the batch are not one transaction: a household the user is added
     * to (with their join key) or an item added to an owned household between the reads and the commit is not in the batch.
     * The caller runs it a second time for the first case; the second leaves an orphaned item behind (a tiny window).
     */
    suspend fun deleteAccountData(userId: String)
}

package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.repository.LoyaltyCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Every write is recorded as an attempt **before** [writeFailure] / [shortcutFailure] apply; successful writes update [cards]
 * like the Room table (a new card gets the next id when it has none and comes first, like the newest-first query).
 */
class FakeLoyaltyCardRepository : LoyaltyCardRepository {
    /** The stored cards in the order [observeAll] emits them (newest first). */
    val cards = MutableStateFlow<List<LoyaltyCard>>(emptyList())

    /** When set, [observeAll] / [observeById] fail with it (checked each time the stream is collected). */
    var observeFailure: Throwable? = null

    /** Thrown by [addCard] / [deleteCard] when set. */
    var writeFailure: Exception? = null

    /** Thrown by [addShortcut] / [removeShortcut] when set. */
    var shortcutFailure: Exception? = null

    /**
     * Runs inside [addShortcut] after the attempt is recorded and before the write, to model a concurrent change
     * (e.g. the card being deleted on another screen between the use case's read and its write).
     */
    var beforeAddShortcut: (() -> Unit)? = null

    /** Every card passed to [addCard], as it was passed in. */
    val addedCards = mutableListOf<LoyaltyCard>()
    val deletedIds = mutableListOf<Long>()

    /** Card id to the limit of every [addShortcut] call. */
    val addedShortcuts = mutableListOf<Pair<Long, Int>>()
    val removedShortcutIds = mutableListOf<Long>()

    override fun observeAll(): Flow<List<LoyaltyCard>> =
        flow {
            observeFailure?.let { throw it }
            emitAll(cards)
        }

    override fun observeById(id: Long): Flow<LoyaltyCard?> =
        flow {
            observeFailure?.let { throw it }
            emitAll(cards.map { list -> list.firstOrNull { it.id == id } })
        }

    override suspend fun getById(id: Long): LoyaltyCard? = cards.value.firstOrNull { it.id == id }

    override suspend fun addCard(card: LoyaltyCard) {
        addedCards += card
        writeFailure?.let { throw it }
        val stored = if (card.id == 0L) card.copy(id = (cards.value.maxOfOrNull { it.id } ?: 0L) + 1) else card
        cards.value = listOf(stored) + cards.value.filterNot { it.id == stored.id }
    }

    override suspend fun deleteCard(id: Long) {
        deletedIds += id
        writeFailure?.let { throw it }
        cards.value = cards.value.filterNot { it.id == id }
    }

    // Mirrors the single conditional UPDATE in LoyaltyCardDao.addShortcutWithinLimit (covered on the real database).
    override suspend fun addShortcut(
        id: Long,
        limit: Int,
    ): Boolean {
        addedShortcuts += id to limit
        shortcutFailure?.let { throw it }
        beforeAddShortcut?.invoke()
        val card = cards.value.firstOrNull { it.id == id } ?: return false
        if (card.isShortcut.not() && cards.value.count { it.isShortcut } >= limit) return false
        cards.value = cards.value.map { if (it.id == id) it.copy(isShortcut = true) else it }
        return true
    }

    override suspend fun removeShortcut(id: Long) {
        removedShortcutIds += id
        shortcutFailure?.let { throw it }
        cards.value = cards.value.map { if (it.id == id) it.copy(isShortcut = false) else it }
    }
}

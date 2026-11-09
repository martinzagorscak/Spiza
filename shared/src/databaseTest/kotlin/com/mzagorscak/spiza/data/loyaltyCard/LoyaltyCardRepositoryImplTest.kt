package com.mzagorscak.spiza.data.loyaltyCard

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.mzagorscak.spiza.data.local.SpizaDatabase
import com.mzagorscak.spiza.data.local.inMemorySpizaDatabase
import com.mzagorscak.spiza.data.loyaltyCard.model.LoyaltyCardEntity
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock

private const val COLOR_ARGB = 0xFF336699.toInt()
private const val CODE_TEXT = "4006381333931"
private const val OTHER_COLOR_ARGB = 0xFF993366.toInt()

class LoyaltyCardRepositoryImplTest {
    private lateinit var database: SpizaDatabase
    private lateinit var repository: LoyaltyCardRepositoryImpl
    private val limit = ConstraintsManagerImpl.maxLoyaltyCardShortcuts

    @BeforeTest
    fun setUp() {
        database = inMemorySpizaDatabase()
        repository = LoyaltyCardRepositoryImpl(loyaltyCardDao = database.loyaltyCardDao())
    }

    @AfterTest
    fun tearDown() = database.close()

    // Seeds rows directly, so the test controls ids and creation times.
    private suspend fun store(vararg entities: LoyaltyCardEntity) = entities.forEach { database.loyaltyCardDao().insert(it) }

    private suspend fun shortcutIds(): List<Long> =
        repository
            .observeAll()
            .first()
            .filter { it.isShortcut }
            .map { it.id }

    @Test
    fun observeAll_emitsEveryCardAsDomainModel_newestFirst() =
        runTest {
            store(
                entity(id = 1L, name = "Oldest", createdAt = 100L),
                entity(id = 2L, name = "Newest", createdAt = 300L, isShortcut = true),
                entity(id = 3L, name = "Middle", createdAt = 200L),
            )

            repository.observeAll().test {
                assertThat(awaitItem()).containsExactly(
                    card(id = 2L, name = "Newest", isShortcut = true),
                    card(id = 3L, name = "Middle"),
                    card(id = 1L, name = "Oldest"),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeById_emitsThatCard_andItsLaterChanges() =
        runTest {
            store(entity(id = 1L, name = "Other"), entity(id = 2L, name = "Wanted"))

            repository.observeById(2L).test {
                assertThat(awaitItem()).isEqualTo(card(id = 2L, name = "Wanted"))

                repository.addShortcut(id = 2L, limit = limit)

                assertThat(awaitItem()).isEqualTo(card(id = 2L, name = "Wanted", isShortcut = true))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeById_emitsNull_whenTheCardDoesNotExist() =
        runTest {
            store(entity(id = 1L))

            repository.observeById(42L).test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getById_returnsThatCard() =
        runTest {
            store(entity(id = 1L, name = "Other"), entity(id = 2L, name = "Wanted", isShortcut = true))

            val card = repository.getById(2L)

            assertThat(card).isEqualTo(card(id = 2L, name = "Wanted", isShortcut = true))
        }

    @Test
    fun addCard_storesEveryField_withAGeneratedIdAndTheCreationTime() =
        runTest {
            val before = Clock.System.now().toEpochMilliseconds()

            repository.addCard(card(id = 0L, name = "Shop", isShortcut = true))

            val after = Clock.System.now().toEpochMilliseconds()
            val stored = database
                .loyaltyCardDao()
                .getAll()
                .first()
                .single()
            assertThat(stored.id).isNotEqualTo(0L)
            assertThat(stored.createdAt).isBetween(before, after)
            assertThat(stored).isEqualTo(
                LoyaltyCardEntity(
                    id = stored.id,
                    name = "Shop",
                    colorArgb = COLOR_ARGB,
                    codeText = CODE_TEXT,
                    format = LoyaltyCardFormat.Ean13,
                    createdAt = stored.createdAt,
                    isShortcut = true,
                ),
            )
        }

    @Test
    fun updateCard_changesTheEditableFields_andKeepsCreationTimeAndShortcut() =
        runTest {
            store(
                entity(id = 1L, name = "Old", createdAt = 100L, isShortcut = true),
                entity(id = 2L, name = "Newer", createdAt = 200L),
            )

            val isUpdated = repository.updateCard(
                LoyaltyCard(
                    id = 1L,
                    name = "Renamed",
                    colorArgb = OTHER_COLOR_ARGB,
                    codeText = "96385074",
                    format = LoyaltyCardFormat.Ean8,
                ),
            )

            assertThat(isUpdated).isTrue()
            assertThat(database.loyaltyCardDao().getAll().first()).containsExactly(
                entity(id = 2L, name = "Newer", createdAt = 200L),
                LoyaltyCardEntity(
                    id = 1L,
                    name = "Renamed",
                    colorArgb = OTHER_COLOR_ARGB,
                    codeText = "96385074",
                    format = LoyaltyCardFormat.Ean8,
                    createdAt = 100L,
                    isShortcut = true,
                ),
            )
        }

    @Test
    fun updateCard_returnsFalseAndChangesNothing_whenTheCardDoesNotExist() =
        runTest {
            store(entity(id = 1L))

            val isUpdated = repository.updateCard(
                LoyaltyCard(
                    id = 2L,
                    name = "Renamed",
                    colorArgb = OTHER_COLOR_ARGB,
                    codeText = "96385074",
                    format = LoyaltyCardFormat.Ean8,
                ),
            )

            assertThat(isUpdated).isFalse()
            assertThat(database.loyaltyCardDao().getAll().first()).containsExactly(entity(id = 1L))
        }

    @Test
    fun deleteCard_removesOnlyThatCard() =
        runTest {
            store(entity(id = 1L), entity(id = 2L))

            repository.deleteCard(1L)

            assertThat(repository.observeAll().first().map { it.id }).containsExactly(2L)
        }

    @Test
    fun addShortcut_pinsTheCardAndReturnsTrue_whenOneShortcutIsLeftBelowTheLimit() =
        runTest {
            store(*Array(limit - 1) { entity(id = it + 1L, isShortcut = true) }, entity(id = 100L))

            val isAdded = repository.addShortcut(id = 100L, limit = limit)

            assertThat(isAdded).isTrue()
            assertThat(shortcutIds()).all {
                contains(100L)
                hasSize(limit)
            }
        }

    @Test
    fun addShortcut_returnsTrue_whenTheCardIsAlreadyAShortcutAtTheLimit() =
        runTest {
            store(*Array(limit) { entity(id = it + 1L, isShortcut = true) })

            val isAdded = repository.addShortcut(id = 1L, limit = limit)

            assertThat(isAdded).isTrue()
        }

    @Test
    fun removeShortcut_unpinsOnlyThatCard() =
        runTest {
            store(entity(id = 1L, isShortcut = true), entity(id = 2L, isShortcut = true))

            repository.removeShortcut(1L)

            assertThat(shortcutIds()).containsExactly(2L)
        }

    // region Error handling

    @Test
    fun addShortcut_returnsFalseAndPinsNothing_whenTheLimitIsReached() =
        runTest {
            store(*Array(limit) { entity(id = it + 1L, isShortcut = true) }, entity(id = 100L))

            val isAdded = repository.addShortcut(id = 100L, limit = limit)

            assertThat(isAdded).isFalse()
            assertThat(shortcutIds()).all {
                doesNotContain(100L)
                hasSize(limit)
            }
        }

    @Test
    fun addShortcut_returnsFalse_whenTheCardDoesNotExist() =
        runTest {
            store(entity(id = 1L))

            val isAdded = repository.addShortcut(id = 42L, limit = limit)

            assertThat(isAdded).isFalse()
        }

    // endregion

    private fun entity(
        id: Long,
        name: String = "Card",
        createdAt: Long = id,
        isShortcut: Boolean = false,
    ) = LoyaltyCardEntity(
        id = id,
        name = name,
        colorArgb = COLOR_ARGB,
        codeText = CODE_TEXT,
        format = LoyaltyCardFormat.Ean13,
        createdAt = createdAt,
        isShortcut = isShortcut,
    )

    private fun card(
        id: Long,
        name: String = "Card",
        isShortcut: Boolean = false,
    ) = LoyaltyCard(
        id = id,
        name = name,
        colorArgb = COLOR_ARGB,
        codeText = CODE_TEXT,
        format = LoyaltyCardFormat.Ean13,
        isShortcut = isShortcut,
    )
}

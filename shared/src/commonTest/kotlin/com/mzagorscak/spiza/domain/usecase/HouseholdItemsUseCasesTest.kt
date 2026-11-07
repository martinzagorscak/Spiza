package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.item
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val HOUSEHOLD_ID = "household-1"

class HouseholdItemsUseCasesTest {
    private val repository = FakeHouseholdRepository()

    @Test
    fun addItem_trimsText_andAppendsAfterTheLastPendingItem() =
        runTest {
            val currentItems =
                listOf(
                    item(id = "milk", position = 1024.0),
                    item(id = "bread", position = 2048.0),
                    item(id = "eggs", isChecked = true, position = 4096.0),
                )

            AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, "  butter ", currentItems)

            val added = repository.addedItems.single()
            assertEquals("butter", added.text)
            assertTrue(
                added.position > 2048.0 && added.position < 4096.0,
                "Position must follow the last pending item, ignoring checked ones",
            )
            assertEquals(3, added.currentItemsCount)
        }

    @Test
    fun addItem_ignoresBlankText() =
        runTest {
            AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, "   ", emptyList())

            assertTrue(repository.addedItems.isEmpty())
        }

    @Test
    fun addItem_ignoresTextAlreadyOnTheList() =
        runTest {
            AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, " milk", listOf(item(id = "1", text = "milk", position = 1.0)))

            assertTrue(repository.addedItems.isEmpty())
        }

    @Test
    fun updateText_deletesItem_whenTextIsBlank() =
        runTest {
            UpdateArticleItemTextUseCaseImpl(repository)(HOUSEHOLD_ID, "milk", "  ")

            assertEquals(listOf("milk"), repository.deletedItems)
            assertTrue(repository.updatedTexts.isEmpty())
        }

    @Test
    fun updateText_savesTrimmedText() =
        runTest {
            UpdateArticleItemTextUseCaseImpl(repository)(HOUSEHOLD_ID, "milk", " oat milk ")

            assertEquals(listOf("milk" to "oat milk"), repository.updatedTexts)
        }

    @Test
    fun moveItem_positionsItemAmongPendingItems_ignoringCheckedItemsAndItself() =
        runTest {
            val currentItems =
                listOf(
                    item(id = "a", position = 1.0),
                    item(id = "b", position = 2.0),
                    item(id = "checked", isChecked = true, position = 3.0),
                    item(id = "moved", position = 4.0),
                )

            MoveArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, itemId = "moved", toIndex = 1, currentItems = currentItems)

            assertEquals(listOf("moved" to 1.5), repository.movedItems)
        }
}

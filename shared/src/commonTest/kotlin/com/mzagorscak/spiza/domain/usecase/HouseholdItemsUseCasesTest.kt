package com.mzagorscak.spiza.domain.usecase

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.item
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

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
            assertThat(added.text).isEqualTo("butter")
            // Position must follow the last pending item, ignoring checked ones.
            assertThat(added.position).all {
                isGreaterThan(2048.0)
                isLessThan(4096.0)
            }
            assertThat(added.currentItemsCount).isEqualTo(3)
        }

    @Test
    fun addItem_movesTheExistingPendingItemAfterTheOtherPendingItems_insteadOfAddingADuplicate() =
        runTest {
            val currentItems =
                listOf(
                    item(id = "milk", text = "Milk", position = 1024.0),
                    item(id = "bread", position = 2048.0),
                    item(id = "eggs", isChecked = true, position = 4096.0),
                )

            val isNewItem = AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, " MILK ", currentItems)

            assertThat(isNewItem).isFalse()
            assertThat(repository.addedItems).isEmpty()
            // After bread, the last other pending item; the checked eggs do not count.
            assertThat(repository.movedItems).containsExactly("milk" to 3072.0)
        }

    @Test
    fun addItem_unchecksTheExistingCheckedItem_insteadOfAddingADuplicate() =
        runTest {
            val currentItems = listOf(item(id = "eggs", text = "Eggs", isChecked = true, position = 1024.0))

            val isNewItem = AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, "eggs", currentItems)

            assertThat(isNewItem).isFalse()
            assertThat(repository.addedItems).isEmpty()
            assertThat(repository.checkedChanges).containsExactly("eggs" to false)
        }

    @Test
    fun updateText_savesTrimmedText() =
        runTest {
            UpdateArticleItemTextUseCaseImpl(repository)(HOUSEHOLD_ID, "milk", " oat milk ")

            assertThat(repository.updatedTexts).containsExactly("milk" to "oat milk")
        }

    @Test
    fun updateText_deletesItem_whenTextIsBlank() =
        runTest {
            UpdateArticleItemTextUseCaseImpl(repository)(HOUSEHOLD_ID, "milk", "  ")

            assertThat(repository.deletedItems).containsExactly("milk")
            assertThat(repository.updatedTexts).isEmpty()
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

            assertThat(repository.movedItems).containsExactly("moved" to 1.5)
        }

    // region Error handling

    @Test
    fun addItem_ignoresBlankText() =
        runTest {
            AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, "   ", emptyList())

            assertThat(repository.addedItems).isEmpty()
        }

    // endregion
}

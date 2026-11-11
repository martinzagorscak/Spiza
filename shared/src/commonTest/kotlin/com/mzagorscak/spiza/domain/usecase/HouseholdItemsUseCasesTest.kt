package com.mzagorscak.spiza.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.mzagorscak.spiza.domain.model.ArticleItemOrdering
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository.CheckedChange
import com.mzagorscak.spiza.fakes.item
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val HOUSEHOLD_ID = "household-1"

class HouseholdItemsUseCasesTest {
    private val repository = FakeHouseholdRepository()

    // The checked item sits before the pending ones, so taking it into account would give a different position.
    @Test
    fun addItem_trimsText_andAddsItAboveTheFirstPendingItem() =
        runTest {
            val milk = item(id = "milk", position = 1024.0)
            val bread = item(id = "bread", position = 2048.0)
            val currentItems = listOf(item(id = "eggs", isChecked = true, position = 512.0), milk, bread)

            AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, "  butter ", currentItems)

            val added = repository.addedItems.single()
            assertThat(added.text).isEqualTo("butter")
            assertThat(added.position).isEqualTo(ArticleItemOrdering.prependPosition(listOf(milk, bread)))
            assertThat(added.currentItemsCount).isEqualTo(3)
        }

    @Test
    fun addItem_movesTheExistingPendingItemToTheTop_insteadOfAddingADuplicate() =
        runTest {
            val bread = item(id = "bread", position = 2048.0)
            // Milk is the lowest pending item and eggs lower still, so counting either would give a different position.
            val currentItems =
                listOf(
                    item(id = "eggs", isChecked = true, position = 512.0),
                    item(id = "milk", text = "Milk", position = 1024.0),
                    bread,
                )

            val isNewItem = AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, " MILK ", currentItems)

            assertThat(isNewItem).isFalse()
            assertThat(repository.addedItems).isEmpty()
            // Above bread, the other pending item; the checked eggs and milk itself do not count.
            assertThat(repository.movedItems).containsExactly("milk" to ArticleItemOrdering.prependPosition(listOf(bread)))
        }

    @Test
    fun addItem_unchecksTheExistingCheckedItem_andMovesItToTheTopOfThePendingItems() =
        runTest {
            val milk = item(id = "milk", position = 2048.0)
            val currentItems = listOf(item(id = "eggs", text = "Eggs", isChecked = true, position = 1024.0), milk)

            val isNewItem = AddArticleItemUseCaseImpl(repository)(HOUSEHOLD_ID, "eggs", currentItems)

            assertThat(isNewItem).isFalse()
            assertThat(repository.addedItems).isEmpty()
            assertThat(repository.checkedChanges).containsExactly(
                CheckedChange(itemId = "eggs", isChecked = false, position = ArticleItemOrdering.prependPosition(listOf(milk))),
            )
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
    fun setItemChecked_movesTheItemToTheTopOfTheCheckedItems_whenChecking() =
        runTest {
            val bread = item(id = "bread", isChecked = true, position = 2048.0)
            val currentItems = listOf(item(id = "eggs", position = 512.0), item(id = "milk", position = 1024.0), bread)

            SetArticleItemCheckedUseCaseImpl(repository)(HOUSEHOLD_ID, itemId = "milk", isChecked = true, currentItems = currentItems)

            assertThat(repository.checkedChanges).containsExactly(
                CheckedChange(itemId = "milk", isChecked = true, position = ArticleItemOrdering.prependPosition(listOf(bread))),
            )
        }

    @Test
    fun setItemChecked_movesTheItemToTheTopOfThePendingItems_whenUnchecking() =
        runTest {
            val eggs = item(id = "eggs", position = 2048.0)
            val currentItems =
                listOf(
                    item(id = "bread", isChecked = true, position = 512.0),
                    item(id = "milk", isChecked = true, position = 1024.0),
                    eggs,
                )

            SetArticleItemCheckedUseCaseImpl(repository)(HOUSEHOLD_ID, itemId = "milk", isChecked = false, currentItems = currentItems)

            assertThat(repository.checkedChanges).containsExactly(
                CheckedChange(itemId = "milk", isChecked = false, position = ArticleItemOrdering.prependPosition(listOf(eggs))),
            )
        }

    // Eggs is pending below milk's position, so using the wrong section would give a different position.
    @Test
    fun setItemChecked_usesTheEmptySectionPosition_whenNoOtherItemIsInTheTargetSection() =
        runTest {
            val currentItems = listOf(item(id = "eggs", position = 512.0), item(id = "milk", position = 1024.0))

            SetArticleItemCheckedUseCaseImpl(repository)(HOUSEHOLD_ID, itemId = "milk", isChecked = true, currentItems = currentItems)

            assertThat(repository.checkedChanges).containsExactly(
                CheckedChange(itemId = "milk", isChecked = true, position = ArticleItemOrdering.prependPosition(emptyList())),
            )
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

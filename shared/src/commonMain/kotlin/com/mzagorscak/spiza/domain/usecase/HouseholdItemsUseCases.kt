package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.ArticleItemOrdering
import com.mzagorscak.spiza.domain.model.findByText
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.Flow

interface ObserveArticleItemsUseCase {
    operator fun invoke(householdId: String): Flow<List<ArticleItem>>
}

class ObserveArticleItemsUseCaseImpl(
    private val repository: HouseholdRepository,
) : ObserveArticleItemsUseCase {
    override operator fun invoke(householdId: String): Flow<List<ArticleItem>> = repository.observeItems(householdId)
}

interface AddArticleItemUseCase {
    /**
     * Adds [text] at the top of the pending items. When it is already on the list (ignoring case and surrounding spaces),
     * that item goes to the top of the pending items instead (a checked one is unchecked). Returns true only when a new item
     * was added.
     */
    suspend operator fun invoke(
        householdId: String,
        text: String,
        currentItems: List<ArticleItem>,
    ): Boolean
}

class AddArticleItemUseCaseImpl(
    private val repository: HouseholdRepository,
) : AddArticleItemUseCase {
    override suspend operator fun invoke(
        householdId: String,
        text: String,
        currentItems: List<ArticleItem>,
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val existingItem = currentItems.findByText(trimmed)
        val pending = currentItems.filterNot { it.isChecked || it.id == existingItem?.id }
        val topPosition = ArticleItemOrdering.prependPosition(pending)
        when {
            existingItem == null -> {
                repository.addItem(
                    householdId = householdId,
                    text = trimmed,
                    position = topPosition,
                    currentItemsCount = currentItems.count(),
                )
                return true
            }

            existingItem.isChecked -> {
                repository.setItemChecked(
                    householdId = householdId,
                    itemId = existingItem.id,
                    isChecked = false,
                    position = topPosition,
                )
            }

            else -> {
                repository.moveItem(
                    householdId = householdId,
                    itemId = existingItem.id,
                    position = topPosition,
                )
            }
        }
        return false
    }
}

interface UpdateArticleItemTextUseCase {
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
        text: String,
    )
}

/** A blank text removes the item. */
class UpdateArticleItemTextUseCaseImpl(
    private val repository: HouseholdRepository,
) : UpdateArticleItemTextUseCase {
    override suspend operator fun invoke(
        householdId: String,
        itemId: String,
        text: String,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            repository.deleteItem(householdId, itemId)
        } else {
            repository.updateItemText(householdId, itemId, trimmed)
        }
    }
}

interface SetArticleItemCheckedUseCase {
    /** Moves the item to the top of the section it lands in: checked items or pending items. */
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
        currentItems: List<ArticleItem>,
    )
}

class SetArticleItemCheckedUseCaseImpl(
    private val repository: HouseholdRepository,
) : SetArticleItemCheckedUseCase {
    override suspend operator fun invoke(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
        currentItems: List<ArticleItem>,
    ) {
        val siblings = currentItems.filter { it.isChecked == isChecked && it.id != itemId }
        repository.setItemChecked(
            householdId = householdId,
            itemId = itemId,
            isChecked = isChecked,
            position = ArticleItemOrdering.prependPosition(siblings),
        )
    }
}

interface MoveArticleItemUseCase {
    /** [toIndex] is the target index inside the pending items, in the order currently shown. */
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
        toIndex: Int,
        currentItems: List<ArticleItem>,
    )
}

class MoveArticleItemUseCaseImpl(
    private val repository: HouseholdRepository,
) : MoveArticleItemUseCase {
    /** [toIndex] is the target index inside the pending items, in the order currently shown. */
    override suspend operator fun invoke(
        householdId: String,
        itemId: String,
        toIndex: Int,
        currentItems: List<ArticleItem>,
    ) {
        val siblings = currentItems.filter { !it.isChecked && it.id != itemId }
        repository.moveItem(householdId, itemId, ArticleItemOrdering.positionAt(siblings, toIndex))
    }
}

interface DeleteArticleItemUseCase {
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
    )
}

class DeleteArticleItemUseCaseImpl(
    private val repository: HouseholdRepository,
) : DeleteArticleItemUseCase {
    override suspend operator fun invoke(
        householdId: String,
        itemId: String,
    ) = repository.deleteItem(householdId, itemId)
}

interface ClearCheckedItemsUseCase {
    suspend operator fun invoke(householdId: String)
}

class ClearCheckedItemsUseCaseImpl(
    private val repository: HouseholdRepository,
) : ClearCheckedItemsUseCase {
    override suspend operator fun invoke(householdId: String) = repository.clearCheckedItems(householdId)
}

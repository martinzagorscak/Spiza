package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.ArticleItemOrdering
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreateHouseholdJoinKeyUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(): String? = repository.createHouseholdJoinKey()
}

class GetHouseholdJoinKeyUseCase(
    private val repository: HouseholdRepository,
) {
    operator fun invoke(): Flow<String?> = repository.getHouseholdJoinKey()
}

class AddMemberToHouseholdUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        householdId: String,
        userJoinKey: String,
    ): Boolean = repository.addMemberToHousehold(householdId, userJoinKey)
}

class RemoveMemberFromHouseholdUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        householdId: String,
        memberId: String,
        isCurrentUserOwner: Boolean,
        isCurrentUserRemovingHimself: Boolean,
    ): Boolean {
        // Only the owner can remove other members and not himself
        if (isCurrentUserOwner.not() && isCurrentUserRemovingHimself.not()) {
            return false
        }

        repository.removeMemberFromHousehold(householdId, memberId)
        return true
    }
}

class DeleteHouseholdUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        householdId: String,
        isCurrentUserOwner: Boolean,
    ): Boolean {
        // Only the owner can delete the household
        if (isCurrentUserOwner.not()) {
            return false
        }

        repository.deleteHousehold(householdId)
        return true
    }
}

class ObserveHouseholdsUseCase(
    private val repository: HouseholdRepository,
) {
    operator fun invoke(): Flow<List<Household>> = repository.observeHouseholds()
}

class ObserveArticleItemsUseCase(
    private val repository: HouseholdRepository,
) {
    operator fun invoke(householdId: String): Flow<List<ArticleItem>> = repository.observeItems(householdId)
}

class CreateHouseholdUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        name: String,
        description: String,
    ): String? {
        if (name.isBlank()) return null
        return repository.createHousehold(name = name.trim(), description = description.trim())
    }
}

class AddArticleItemUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        householdId: String,
        text: String,
        currentItems: List<ArticleItem>,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (currentItems.map { it.text }.contains(trimmed)) return
        val pending = currentItems.filterNot { it.isChecked }
        repository.addItem(
            householdId = householdId,
            text = trimmed,
            position = ArticleItemOrdering.appendPosition(pending),
            currentItemsCount = currentItems.count(),
        )
    }
}

/** A blank text removes the item. */
class UpdateArticleItemTextUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
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

class SetArticleItemCheckedUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) = repository.setItemChecked(householdId, itemId, isChecked)
}

class MoveArticleItemUseCase(
    private val repository: HouseholdRepository,
) {
    /** [toIndex] is the target index inside the pending items, in the order currently shown. */
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
        toIndex: Int,
        currentItems: List<ArticleItem>,
    ) {
        val siblings = currentItems.filter { !it.isChecked && it.id != itemId }
        repository.moveItem(householdId, itemId, ArticleItemOrdering.positionAt(siblings, toIndex))
    }
}

class DeleteArticleItemUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(
        householdId: String,
        itemId: String,
    ) = repository.deleteItem(householdId, itemId)
}

class ClearCheckedItemsUseCase(
    private val repository: HouseholdRepository,
) {
    suspend operator fun invoke(householdId: String) = repository.clearCheckedItems(householdId)
}

class ObserveSelectedHouseholdIdUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<String?> = repository.selectedHouseholdId
}

class SetSelectedHouseholdIdUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(householdId: String) = repository.setSelectedHouseholdId(householdId)
}

class CanUserCreateHouseholdUseCase(
    private val constraintsManager: ConstraintsManager,
) {
    operator fun invoke(householdCount: Int): Boolean = constraintsManager.canUserCreateOrJoinHousehold(householdCount = householdCount)
}

class CanUserJoinHouseholdUseCase(
    private val repository: HouseholdRepository,
    private val constraintsManager: ConstraintsManager,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.getCachedHouseholdCount().map { householdCount ->
            constraintsManager.canUserCreateOrJoinHousehold(householdCount = householdCount)
        }
}

class CanAddItemToHouseholdUseCase(
    private val constraintsManager: ConstraintsManager,
) {
    operator fun invoke(itemCount: Int): Boolean = constraintsManager.canAddItemToHousehold(itemCount = itemCount)
}

class CanAddMemberToHouseholdUseCase(
    private val constraintsManager: ConstraintsManager,
) {
    operator fun invoke(memberCount: Int): Boolean = constraintsManager.canAddMemberToHousehold(memberCount = memberCount)
}

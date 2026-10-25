package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

interface CreateHouseholdJoinKeyUseCase {
    suspend operator fun invoke(): String?
}

class CreateHouseholdJoinKeyUseCaseImpl(
    private val repository: HouseholdRepository,
) : CreateHouseholdJoinKeyUseCase {
    override suspend operator fun invoke(): String? = repository.createHouseholdJoinKey()
}

interface GetHouseholdJoinKeyUseCase {
    operator fun invoke(): Flow<String?>
}

class GetHouseholdJoinKeyUseCaseImpl(
    private val repository: HouseholdRepository,
) : GetHouseholdJoinKeyUseCase {
    override operator fun invoke(): Flow<String?> = repository.getHouseholdJoinKey()
}

interface RefreshHouseholdJoinKeyUseCase {
    suspend operator fun invoke()
}

class RefreshHouseholdJoinKeyUseCaseImpl(
    private val repository: HouseholdRepository,
) : RefreshHouseholdJoinKeyUseCase {
    override suspend operator fun invoke() = repository.refreshHouseholdJoinKey()
}

interface AddMemberToHouseholdUseCase {
    suspend operator fun invoke(
        householdId: String,
        userJoinKey: String,
    ): Boolean
}

class AddMemberToHouseholdUseCaseImpl(
    private val repository: HouseholdRepository,
) : AddMemberToHouseholdUseCase {
    override suspend operator fun invoke(
        householdId: String,
        userJoinKey: String,
    ): Boolean = repository.addMemberToHousehold(householdId, userJoinKey)
}

interface RemoveMemberFromHouseholdUseCase {
    suspend operator fun invoke(
        householdId: String,
        memberId: String,
        isCurrentUserOwner: Boolean,
        isCurrentUserRemovingHimself: Boolean,
    ): Boolean
}

class RemoveMemberFromHouseholdUseCaseImpl(
    private val repository: HouseholdRepository,
) : RemoveMemberFromHouseholdUseCase {
    override suspend operator fun invoke(
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

interface DeleteHouseholdUseCase {
    suspend operator fun invoke(
        householdId: String,
        isCurrentUserOwner: Boolean,
    ): Boolean
}

class DeleteHouseholdUseCaseImpl(
    private val repository: HouseholdRepository,
) : DeleteHouseholdUseCase {
    override suspend operator fun invoke(
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

interface ObserveHouseholdsUseCase {
    operator fun invoke(): Flow<List<Household>>
}

class ObserveHouseholdsUseCaseImpl(
    private val repository: HouseholdRepository,
) : ObserveHouseholdsUseCase {
    override operator fun invoke(): Flow<List<Household>> = repository.observeHouseholds()
}

interface CreateHouseholdUseCase {
    suspend operator fun invoke(
        name: String,
        description: String,
    ): String?
}

class CreateHouseholdUseCaseImpl(
    private val repository: HouseholdRepository,
) : CreateHouseholdUseCase {
    override suspend operator fun invoke(
        name: String,
        description: String,
    ): String? {
        if (name.isBlank()) return null
        return repository.createHousehold(name = name.trim(), description = description.trim())
    }
}

interface UpdateHouseholdUseCase {
    suspend operator fun invoke(
        householdId: String,
        name: String,
        description: String,
    ): Boolean
}

class UpdateHouseholdUseCaseImpl(
    private val repository: HouseholdRepository,
) : UpdateHouseholdUseCase {
    override suspend operator fun invoke(
        householdId: String,
        name: String,
        description: String,
    ): Boolean {
        if (name.isBlank()) return false
        repository.updateHousehold(householdId = householdId, name = name.trim(), description = description.trim())
        return true
    }
}

interface ObserveSelectedHouseholdIdUseCase {
    operator fun invoke(): Flow<String?>
}

class ObserveSelectedHouseholdIdUseCaseImpl(
    private val repository: SettingsRepository,
) : ObserveSelectedHouseholdIdUseCase {
    override operator fun invoke(): Flow<String?> = repository.selectedHouseholdId
}

interface SetSelectedHouseholdIdUseCase {
    suspend operator fun invoke(householdId: String)
}

class SetSelectedHouseholdIdUseCaseImpl(
    private val repository: SettingsRepository,
) : SetSelectedHouseholdIdUseCase {
    override suspend operator fun invoke(householdId: String) = repository.setSelectedHouseholdId(householdId)
}

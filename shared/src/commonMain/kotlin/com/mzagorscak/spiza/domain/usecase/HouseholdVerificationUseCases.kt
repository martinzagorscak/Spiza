package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CanUserCreateHouseholdUseCase {
    operator fun invoke(householdCount: Int): Boolean
}

class CanUserCreateHouseholdUseCaseImpl(
    private val constraintsManager: ConstraintsManager,
) : CanUserCreateHouseholdUseCase {
    override operator fun invoke(householdCount: Int): Boolean =
        constraintsManager.canUserCreateOrJoinHousehold(householdCount = householdCount)
}

interface CanUserJoinHouseholdUseCase {
    operator fun invoke(): Flow<Boolean>
}

class CanUserJoinHouseholdUseCaseImpl(
    private val repository: HouseholdRepository,
    private val constraintsManager: ConstraintsManager,
) : CanUserJoinHouseholdUseCase {
    override operator fun invoke(): Flow<Boolean> =
        repository.getCachedHouseholdCount().map { householdCount ->
            constraintsManager.canUserCreateOrJoinHousehold(householdCount = householdCount)
        }
}

interface CanAddItemToHouseholdUseCase {
    operator fun invoke(itemCount: Int): Boolean
}

class CanAddItemToHouseholdUseCaseImpl(
    private val constraintsManager: ConstraintsManager,
) : CanAddItemToHouseholdUseCase {
    override operator fun invoke(itemCount: Int): Boolean = constraintsManager.canAddItemToHousehold(itemCount = itemCount)
}

interface CanAddMemberToHouseholdUseCase {
    operator fun invoke(memberCount: Int): Boolean
}

class CanAddMemberToHouseholdUseCaseImpl(
    private val constraintsManager: ConstraintsManager,
) : CanAddMemberToHouseholdUseCase {
    override operator fun invoke(memberCount: Int): Boolean = constraintsManager.canAddMemberToHousehold(memberCount = memberCount)
}

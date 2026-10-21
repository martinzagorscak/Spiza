package com.mzagorscak.spiza.domain.constraints

interface ConstraintsManager {
    fun canUserCreateOrJoinHousehold(householdCount: Int): Boolean

    fun canAddItemToHousehold(itemCount: Int): Boolean

    fun canAddMemberToHousehold(memberCount: Int): Boolean
}

internal object ConstraintsManagerImpl : ConstraintsManager {
    private const val HOUSEHOLDS_PER_USER_CONTRAINT: Int = 3
    private const val ITEMS_PER_HOUSEHOLD_CONSTRAINT: Int = 100
    private const val MEMBERS_PER_HOUSEHOLD_CONSTRAINT: Int = 10

    override fun canUserCreateOrJoinHousehold(householdCount: Int): Boolean = householdCount < HOUSEHOLDS_PER_USER_CONTRAINT

    override fun canAddItemToHousehold(itemCount: Int): Boolean = itemCount < ITEMS_PER_HOUSEHOLD_CONSTRAINT

    override fun canAddMemberToHousehold(memberCount: Int): Boolean = memberCount < MEMBERS_PER_HOUSEHOLD_CONSTRAINT
}

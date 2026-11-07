package com.mzagorscak.spiza.domain.constraints

interface ConstraintsManager {
    fun canUserCreateOrJoinHousehold(householdCount: Int): Boolean

    fun canAddItemToHousehold(itemCount: Int): Boolean

    fun canAddMemberToHousehold(memberCount: Int): Boolean

    /** Enforced atomically by the repository write, so it is a number rather than a `can…` check. */
    val maxLoyaltyCardShortcuts: Int
}

internal object ConstraintsManagerImpl : ConstraintsManager {
    private const val HOUSEHOLDS_PER_USER_CONTRAINT: Int = 3
    private const val ITEMS_PER_HOUSEHOLD_CONSTRAINT: Int = 100
    private const val MEMBERS_PER_HOUSEHOLD_CONSTRAINT: Int = 10

    // Launchers show only ~4 shortcuts per app, so a small limit keeps every pinned card visible.
    private const val LOYALTY_CARD_SHORTCUTS_CONSTRAINT: Int = 3

    override fun canUserCreateOrJoinHousehold(householdCount: Int): Boolean = householdCount < HOUSEHOLDS_PER_USER_CONTRAINT

    override fun canAddItemToHousehold(itemCount: Int): Boolean = itemCount < ITEMS_PER_HOUSEHOLD_CONSTRAINT

    override fun canAddMemberToHousehold(memberCount: Int): Boolean = memberCount < MEMBERS_PER_HOUSEHOLD_CONSTRAINT

    override val maxLoyaltyCardShortcuts: Int = LOYALTY_CARD_SHORTCUTS_CONSTRAINT
}

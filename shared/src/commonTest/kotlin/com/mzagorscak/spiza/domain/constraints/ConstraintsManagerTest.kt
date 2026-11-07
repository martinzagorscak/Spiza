package com.mzagorscak.spiza.domain.constraints

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val HOUSEHOLDS_PER_USER_CONTRAINT: Int = 3
private const val ITEMS_PER_HOUSEHOLD_CONSTRAINT: Int = 100
private const val MEMBERS_PER_HOUSEHOLD_CONSTRAINT: Int = 10

class ConstraintsManagerTest {
    private val constraintsManager: ConstraintsManager = ConstraintsManagerImpl

    @Test
    fun canUserCreateOrJoinHousehold_isTrueOnlyBelowTheLimit() {
        assertTrue(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT - 1))
        assertFalse(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT))
        assertFalse(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT + 1))
    }

    @Test
    fun canAddItemToHousehold_isTrueOnlyBelowTheLimit() {
        assertTrue(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT - 1))
        assertFalse(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT))
        assertFalse(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT + 1))
    }

    @Test
    fun canAddMemberToHousehold_isTrueOnlyBelowTheLimit() {
        assertTrue(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT - 1))
        assertFalse(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT))
        assertFalse(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT + 1))
    }
}

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
    fun canUserCreateOrJoinHousehold_returnsTrue_whenCountIsLessThanLimit() {
        assertTrue(constraintsManager.canUserCreateOrJoinHousehold(0))
        assertTrue(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT - 1))
    }

    @Test
    fun canUserCreateOrJoinHousehold_returnsFalse_whenCountIsAtOrAboveLimit() {
        assertFalse(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT))
        assertFalse(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT + 1))
    }

    @Test
    fun canAddItemToHousehold_returnsTrue_whenCountIsLessThanLimit() {
        assertTrue(constraintsManager.canAddItemToHousehold(0))
        assertTrue(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT - 1))
    }

    @Test
    fun canAddItemToHousehold_returnsFalse_whenCountIsAtOrAboveLimit() {
        assertFalse(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT))
        assertFalse(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT + 1))
    }

    @Test
    fun canAddMemberToHousehold_returnsTrue_whenCountIsLessThanLimit() {
        assertTrue(constraintsManager.canAddMemberToHousehold(0))
        assertTrue(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT - 1))
    }

    @Test
    fun canAddMemberToHousehold_returnsFalse_whenCountIsAtOrAboveLimit() {
        assertFalse(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT))
        assertFalse(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT + 1))
    }
}

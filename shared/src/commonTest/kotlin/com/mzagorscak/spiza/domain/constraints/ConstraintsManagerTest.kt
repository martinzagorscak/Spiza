package com.mzagorscak.spiza.domain.constraints

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

private const val HOUSEHOLDS_PER_USER_CONTRAINT: Int = 3
private const val ITEMS_PER_HOUSEHOLD_CONSTRAINT: Int = 100
private const val MEMBERS_PER_HOUSEHOLD_CONSTRAINT: Int = 10

class ConstraintsManagerTest {
    private val constraintsManager: ConstraintsManager = ConstraintsManagerImpl

    @Test
    fun canUserCreateOrJoinHousehold_isTrueOnlyBelowTheLimit() {
        assertThat(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT - 1)).isTrue()
        assertThat(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT)).isFalse()
        assertThat(constraintsManager.canUserCreateOrJoinHousehold(HOUSEHOLDS_PER_USER_CONTRAINT + 1)).isFalse()
    }

    @Test
    fun canAddItemToHousehold_isTrueOnlyBelowTheLimit() {
        assertThat(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT - 1)).isTrue()
        assertThat(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT)).isFalse()
        assertThat(constraintsManager.canAddItemToHousehold(ITEMS_PER_HOUSEHOLD_CONSTRAINT + 1)).isFalse()
    }

    @Test
    fun canAddMemberToHousehold_isTrueOnlyBelowTheLimit() {
        assertThat(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT - 1)).isTrue()
        assertThat(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT)).isFalse()
        assertThat(constraintsManager.canAddMemberToHousehold(MEMBERS_PER_HOUSEHOLD_CONSTRAINT + 1)).isFalse()
    }
}

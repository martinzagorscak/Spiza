package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HouseholdManagementUseCasesTest {
    private val repository = FakeHouseholdRepository()
    private val removeMember = RemoveMemberFromHouseholdUseCaseImpl(repository)
    private val deleteHousehold = DeleteHouseholdUseCaseImpl(repository)

    @Test
    fun removeMember_isRejected_whenNonOwnerRemovesSomeoneElse() =
        runTest {
            val isRemoved = removeMember("household-1", "user-2", isCurrentUserOwner = false, isCurrentUserRemovingHimself = false)

            assertFalse(isRemoved)
            assertTrue(repository.removedMembers.isEmpty())
        }

    @Test
    fun removeMember_isAllowed_whenMemberLeavesTheHousehold() =
        runTest {
            val isRemoved = removeMember("household-1", "user-2", isCurrentUserOwner = false, isCurrentUserRemovingHimself = true)

            assertTrue(isRemoved)
            assertEquals(listOf("user-2"), repository.removedMembers)
        }

    @Test
    fun removeMember_isAllowed_whenOwnerRemovesAMember() =
        runTest {
            val isRemoved = removeMember("household-1", "user-2", isCurrentUserOwner = true, isCurrentUserRemovingHimself = false)

            assertTrue(isRemoved)
            assertEquals(listOf("user-2"), repository.removedMembers)
        }

    @Test
    fun deleteHousehold_isRejected_whenUserIsNotTheOwner() =
        runTest {
            assertFalse(deleteHousehold("household-1", isCurrentUserOwner = false))
            assertTrue(repository.deletedHouseholds.isEmpty())
        }

    @Test
    fun deleteHousehold_isAllowed_forTheOwner() =
        runTest {
            assertTrue(deleteHousehold("household-1", isCurrentUserOwner = true))
            assertEquals(listOf("household-1"), repository.deletedHouseholds)
        }
}

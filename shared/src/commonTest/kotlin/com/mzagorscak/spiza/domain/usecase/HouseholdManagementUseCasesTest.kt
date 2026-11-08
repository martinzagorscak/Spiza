package com.mzagorscak.spiza.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class HouseholdManagementUseCasesTest {
    private val repository = FakeHouseholdRepository()
    private val removeMember = RemoveMemberFromHouseholdUseCaseImpl(repository)
    private val deleteHousehold = DeleteHouseholdUseCaseImpl(repository)
    private val createHousehold = CreateHouseholdUseCaseImpl(repository)
    private val updateHousehold = UpdateHouseholdUseCaseImpl(repository)

    @Test
    fun removeMember_isAllowed_whenMemberLeavesTheHousehold() =
        runTest {
            val isRemoved = removeMember("household-1", "user-2", isCurrentUserOwner = false, isCurrentUserRemovingHimself = true)

            assertThat(isRemoved).isTrue()
            assertThat(repository.removedMembers).containsExactly("user-2")
        }

    @Test
    fun removeMember_isAllowed_whenOwnerRemovesAMember() =
        runTest {
            val isRemoved = removeMember("household-1", "user-2", isCurrentUserOwner = true, isCurrentUserRemovingHimself = false)

            assertThat(isRemoved).isTrue()
            assertThat(repository.removedMembers).containsExactly("user-2")
        }

    @Test
    fun deleteHousehold_isAllowed_forTheOwner() =
        runTest {
            assertThat(deleteHousehold("household-1", isCurrentUserOwner = true)).isTrue()
            assertThat(repository.deletedHouseholds).containsExactly("household-1")
        }

    @Test
    fun createHousehold_createsTrimmedHousehold_andReturnsItsId() =
        runTest {
            val id = createHousehold(name = "  Home ", description = " Our flat  ")

            assertThat(id).isEqualTo("household-new-1")
            assertThat(repository.createdHouseholds).containsExactly("Home" to "Our flat")
        }

    @Test
    fun updateHousehold_savesTrimmedFields_andReturnsTrue() =
        runTest {
            val isUpdated = updateHousehold(householdId = "household-1", name = " Home ", description = " Our flat ")

            assertThat(isUpdated).isTrue()
            assertThat(repository.updatedHouseholds).containsExactly(
                FakeHouseholdRepository.UpdatedHousehold(householdId = "household-1", name = "Home", description = "Our flat"),
            )
        }

    // region Error handling

    @Test
    fun removeMember_isRejected_whenNonOwnerRemovesSomeoneElse() =
        runTest {
            val isRemoved = removeMember("household-1", "user-2", isCurrentUserOwner = false, isCurrentUserRemovingHimself = false)

            assertThat(isRemoved).isFalse()
            assertThat(repository.removedMembers).isEmpty()
        }

    @Test
    fun deleteHousehold_isRejected_whenUserIsNotTheOwner() =
        runTest {
            assertThat(deleteHousehold("household-1", isCurrentUserOwner = false)).isFalse()
            assertThat(repository.deletedHouseholds).isEmpty()
        }

    @Test
    fun createHousehold_returnsNullWithoutWriting_whenNameIsBlank() =
        runTest {
            assertThat(createHousehold(name = "   ", description = "Our flat")).isNull()
            assertThat(repository.createdHouseholds).isEmpty()
        }

    @Test
    fun updateHousehold_returnsFalseWithoutWriting_whenNameIsBlank() =
        runTest {
            assertThat(updateHousehold(householdId = "household-1", name = "   ", description = "Our flat")).isFalse()
            assertThat(repository.updatedHouseholds).isEmpty()
        }

    // endregion
}

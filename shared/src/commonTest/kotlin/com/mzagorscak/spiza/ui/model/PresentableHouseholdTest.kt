package com.mzagorscak.spiza.ui.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.model.UserProfile
import kotlin.test.Test

class PresentableHouseholdTest {
    private val owner = UserProfile(id = "owner", name = "Ana", email = "ana@example.com", photoUrl = "https://photo")
    private val member = UserProfile(id = "member", name = null, email = "ivo@example.com", photoUrl = null)
    private val household =
        Household(id = "household-1", name = "Home", description = "Flat", ownerId = "owner", members = listOf(owner, member))

    @Test
    fun toPresentable_marksOwnerAndCurrentUser_andFallsBackToEmailForMembersWithoutName() {
        val presentable = household.toPresentable(currentUserId = "member", canAddMemberToHousehold = true)

        assertThat(presentable.members).containsExactly(
            PresentableHouseholdMember(id = "owner", name = "Ana", photoUrl = "https://photo", isOwner = true, isCurrentUser = false),
            PresentableHouseholdMember(id = "member", name = "ivo@example.com", photoUrl = null, isOwner = false, isCurrentUser = true),
        )
        assertThat(presentable.isCurrentUserOwner).isFalse()
    }
}

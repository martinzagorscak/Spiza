package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.auth.AuthUser
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.model.UserProfile

val TEST_USER = AuthUser(uid = "user-1", displayName = "Ana", email = "ana@example.com", photoUrl = null)
val TEST_PROFILE = UserProfile(id = TEST_USER.uid, name = TEST_USER.displayName, email = TEST_USER.email, photoUrl = null)

fun householdDto(
    id: String = "household-1",
    name: String = "Home",
    ownerId: String = TEST_USER.uid,
    members: List<String> = listOf(TEST_USER.uid),
) = HouseholdDto(id = id, name = name, description = "", ownerId = ownerId, joinedMembers = members)

fun household(
    id: String = "household-1",
    name: String = "Home",
    ownerId: String = TEST_USER.uid,
) = Household(id = id, name = name, description = "", ownerId = ownerId, members = listOf(TEST_PROFILE))

fun item(
    id: String,
    text: String = id,
    isChecked: Boolean = false,
    position: Double,
) = ArticleItem(id = id, text = text, isChecked = isChecked, position = position)

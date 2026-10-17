package com.mzagorscak.spiza.ui.model

import com.mzagorscak.spiza.domain.model.UserProfile

data class PresentableUserProfile(
    val name: String?,
    val email: String?,
    val photoUrl: String?,
    val joinKey: String?,
)

fun UserProfile.toPresentable(joinKey: String?) = PresentableUserProfile(name = name, email = email, photoUrl = photoUrl, joinKey = joinKey)

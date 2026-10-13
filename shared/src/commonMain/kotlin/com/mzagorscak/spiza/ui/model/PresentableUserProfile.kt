package com.mzagorscak.spiza.ui.model

import com.mzagorscak.spiza.domain.model.UserProfile

data class PresentableUserProfile(
    val name: String?,
    val email: String?,
    val photoUrl: String?,
    val key: String,
)

fun UserProfile.toPresentable() = PresentableUserProfile(name = name, email = email, photoUrl = photoUrl, key = id)

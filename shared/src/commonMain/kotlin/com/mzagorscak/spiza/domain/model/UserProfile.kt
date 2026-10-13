package com.mzagorscak.spiza.domain.model

/** [id] is the key other household owners use to invite this user. */
data class UserProfile(
    val id: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?,
)

package com.mzagorscak.spiza.domain.model

data class Household(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val members: List<UserProfile>,
)

/** The user's households and the one the shopping list shows ([current] is null only when there are no households). */
data class HouseholdSelection(
    val households: List<Household>,
    val current: Household?,
)

/** Items of one household live in a single list; [isChecked] decides whether it is pending or done. */
data class ArticleItem(
    val id: String,
    val text: String,
    val isChecked: Boolean,
    val position: Double,
)

sealed interface CreateHouseholdResult {
    /** [householdId] is null when the server did not acknowledge in time: the household is created locally and synced later. */
    data class Created(
        val householdId: String?,
    ) : CreateHouseholdResult

    data class LimitReached(
        val limit: Int,
    ) : CreateHouseholdResult

    data object NotSignedIn : CreateHouseholdResult

    data object InvalidName : CreateHouseholdResult
}

package com.mzagorscak.spiza.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Login : Route

    @Serializable
    data class CreateHousehold(
        val householdId: String? = null,
        val householdName: String? = null,
        val householdDescription: String? = null,
    ) : Route

    @Serializable
    data object ShoppingList : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object HouseholdManagement : Route

    @Serializable
    data object SuggestionsSavingSettings : Route

    @Serializable
    data object LoyaltyCards : Route

    @Serializable
    data class LoyaltyCardDetails(
        val cardId: Long,
    ) : Route

    @Serializable
    data object AddLoyaltyCard : Route
}

// Every new Route must be registered here so the back stack can be saved/restored on all platforms.
val routeSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Login::class)
            subclass(Route.CreateHousehold::class)
            subclass(Route.ShoppingList::class)
            subclass(Route.Settings::class)
            subclass(Route.HouseholdManagement::class)
            subclass(Route.SuggestionsSavingSettings::class)
            subclass(Route.LoyaltyCards::class)
            subclass(Route.LoyaltyCardDetails::class)
            subclass(Route.AddLoyaltyCard::class)
        }
    }

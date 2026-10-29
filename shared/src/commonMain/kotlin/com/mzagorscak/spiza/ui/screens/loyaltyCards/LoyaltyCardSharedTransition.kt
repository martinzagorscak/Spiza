package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class LoyaltyCardSharedTransition(
    private val sharedTransitionScope: SharedTransitionScope,
    private val animatedVisibilityScope: AnimatedVisibilityScope,
) {
    @Composable
    fun modifierFor(cardId: Long): Modifier =
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "loyalty-card-$cardId"),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
}

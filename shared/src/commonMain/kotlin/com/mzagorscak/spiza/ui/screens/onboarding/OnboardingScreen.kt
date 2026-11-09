package com.mzagorscak.spiza.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.StatusBarAppearance
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.padding600
import com.mzagorscak.spiza.ui.theme.padding800
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.error_generic
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.onboarding_back
import spiza.shared.generated.resources.onboarding_get_started
import spiza.shared.generated.resources.onboarding_households_description
import spiza.shared.generated.resources.onboarding_households_title
import spiza.shared.generated.resources.onboarding_loyalty_cards_description
import spiza.shared.generated.resources.onboarding_loyalty_cards_title
import spiza.shared.generated.resources.onboarding_next
import spiza.shared.generated.resources.onboarding_page_indicator
import spiza.shared.generated.resources.onboarding_shopping_list_description
import spiza.shared.generated.resources.onboarding_shopping_list_title
import spiza.shared.generated.resources.onboarding_skip
import spiza.shared.generated.resources.onboarding_suggestions_description
import spiza.shared.generated.resources.onboarding_suggestions_title

private val TopRowHeight = 56.dp
private val ButtonHeight = 56.dp
private val DotSize = 8.dp
private val SelectedDotWidth = 24.dp

private enum class OnboardingPage(
    val title: StringResource,
    val description: StringResource,
) {
    Households(title = Res.string.onboarding_households_title, description = Res.string.onboarding_households_description),
    ShoppingList(title = Res.string.onboarding_shopping_list_title, description = Res.string.onboarding_shopping_list_description),
    Suggestions(title = Res.string.onboarding_suggestions_title, description = Res.string.onboarding_suggestions_description),
    LoyaltyCards(title = Res.string.onboarding_loyalty_cards_title, description = Res.string.onboarding_loyalty_cards_description),
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    callbacks: OnboardingScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) callbacks.onFinished()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(Res.string.error_generic)
    LaunchedEffect(state.hasError) {
        if (state.hasError) {
            snackbarHostState.showSnackbar(errorMessage)
            callbacks.onErrorShown()
        }
    }

    StatusBarAppearance(useLightIcons = MaterialTheme.colorScheme.background.luminance() < 0.5f)

    val pages = OnboardingPage.entries
    // The current page is pure UI state: it only decides which buttons show, so it lives in the pager, not the ViewModel.
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = padding600),
        ) {
            OnboardingTopRow(
                isReplay = state.isReplay,
                isSkipVisible = isLastPage.not(),
                isEnabled = state.canFinish,
                callbacks = callbacks,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { index ->
                OnboardingPageContent(page = pages[index])
            }
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(vertical = padding600),
            )
            Button(
                label = stringResource(if (isLastPage) Res.string.onboarding_get_started else Res.string.onboarding_next),
                onClick = {
                    if (isLastPage) {
                        callbacks.onGetStartedClick()
                    } else {
                        callbacks.onNextClick()
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                isEnabled = state.canFinish,
                appearance = ButtonAppearance(variant = ButtonVariant.Primary, size = ButtonSize.Large),
                modifier = Modifier
                    .padding(horizontal = padding600)
                    .fillMaxWidth()
                    .heightIn(min = ButtonHeight),
            )
        }
    }
}

@Composable
private fun OnboardingTopRow(
    isReplay: Boolean,
    isSkipVisible: Boolean,
    isEnabled: Boolean,
    callbacks: OnboardingScreenCallbacks,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TopRowHeight)
            .padding(horizontal = padding200),
    ) {
        if (isReplay) {
            val backDescription = stringResource(Res.string.onboarding_back)
            IconButton(
                resource = Res.drawable.ic_back,
                onClick = callbacks.onBackClick,
                appearance = IconButtonAppearance(
                    variant = IconButtonVariant.IconBased,
                    size = IconButtonSize.Large,
                    tintColor = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier.semantics { contentDescription = backDescription },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isSkipVisible) {
            Button(
                label = stringResource(Res.string.onboarding_skip),
                onClick = callbacks.onSkipClick,
                isEnabled = isEnabled,
                appearance = ButtonAppearance(variant = ButtonVariant.Tertiary, size = ButtonSize.Medium),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = padding800),
    ) {
        // Decorative: the title and description say it all, so the sample checkboxes, labels and card are not announced.
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            when (page) {
                OnboardingPage.Households -> HouseholdsIllustration()
                OnboardingPage.ShoppingList -> ShoppingListIllustration()
                OnboardingPage.Suggestions -> SuggestionsIllustration()
                OnboardingPage.LoyaltyCards -> LoyaltyCardsIllustration()
            }
        }
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = padding800)
                .semantics { heading() },
        )
        Text(
            text = stringResource(page.description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = padding400),
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.onboarding_page_indicator, currentPage + 1, pageCount)
    Row(
        horizontalArrangement = Arrangement.spacedBy(padding200),
        modifier = modifier.semantics { contentDescription = description },
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(targetValue = if (isSelected) SelectedDotWidth else DotSize, label = "dotWidth")
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .size(height = DotSize, width = width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

data class OnboardingScreenCallbacks(
    val onBackClick: () -> Unit,
    val onSkipClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onGetStartedClick: () -> Unit,
    val onFinished: () -> Unit,
    val onErrorShown: () -> Unit,
)

private val PreviewCallbacks = OnboardingScreenCallbacks(
    onBackClick = {},
    onSkipClick = {},
    onNextClick = {},
    onGetStartedClick = {},
    onFinished = {},
    onErrorShown = {},
)

@Preview
@Composable
private fun OnboardingScreenPreview() {
    TheAppTheme {
        OnboardingScreen(state = OnboardingState(), callbacks = PreviewCallbacks)
    }
}

@Preview
@Composable
private fun OnboardingScreenReplayDarkPreview() {
    TheAppTheme(darkTheme = true) {
        OnboardingScreen(state = OnboardingState(isReplay = true), callbacks = PreviewCallbacks)
    }
}

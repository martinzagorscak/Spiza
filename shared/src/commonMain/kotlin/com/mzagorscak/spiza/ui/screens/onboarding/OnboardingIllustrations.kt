package com.mzagorscak.spiza.ui.screens.onboarding

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.ui.components.Avatar
import com.mzagorscak.spiza.ui.components.AvatarSize
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.LoyaltyCard
import com.mzagorscak.spiza.ui.components.TextChip
import com.mzagorscak.spiza.ui.components.barcode.Barcode
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.theme.CardColorBlue
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding300
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.padding600
import com.mzagorscak.spiza.ui.theme.roundedShape300
import com.mzagorscak.spiza.ui.theme.roundedShape400
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.app_name
import spiza.shared.generated.resources.ic_add_household
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.onboarding_sample_item_bread
import spiza.shared.generated.resources.onboarding_sample_item_eggs
import spiza.shared.generated.resources.onboarding_sample_item_milk
import spiza.shared.generated.resources.onboarding_sample_suggestion_bananas
import spiza.shared.generated.resources.onboarding_sample_suggestion_coffee
import spiza.shared.generated.resources.onboarding_sample_suggestion_yogurt
import spiza.shared.generated.resources.shopping_list_done_section
import spiza.shared.generated.resources.shopping_list_suggestions_title
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Every illustration is a timeline over one loop: its parts appear, act, hold, and fade out at FADE_OUT_START before restarting.
// The loop progress is read only in layer, offset and draw lambdas, so the animation never recomposes per frame.
private const val FADE_OUT_START = 0.88f
private const val FADE_OUT_DURATION = 0.12f
private const val POP_DURATION = 0.1f
private const val PRESS_DURATION = 0.08f
private const val PRESS_DEPTH = 0.12f
private const val SCRIM_ALPHA = 0.32f
private const val SINGLE_LINE = 1

// Same share of the width as the real suggestions drawer on the shopping list.
private const val DRAWER_FRACTION = 0.75f
private const val CARD_CAMERA_DISTANCE = 12f
private const val HALF_TURN_DEGREES = 180f

private val IllustrationSize = 240.dp
private val HouseSize = 88.dp
private val HouseIconSize = 44.dp
private val MemberOrbitRadius = 92.dp
private val MemberLineWidth = 2.dp
private val ListRowHeight = 40.dp

// The suggestions page is a miniature of a real 360dp-wide phone screen (cropped to the part the drawer needs), scaled so it
// fills the illustration: 360dp * 2/3 = 240dp. List text is enlarged inside it so it reads like the shopping list page (~12sp).
private val VirtualPhoneWidth = 360.dp
private val VirtualPhoneHeight = 360.dp
private const val MINIATURE_SCALE = 2f / 3f
private const val MINIATURE_ROW_ENLARGEMENT = 1.3f
private val RealListRowHeight = 56.dp
private val RealFabSize = 56.dp
private val CardHeight = 150.dp
private val CardBarcodeHeight = 64.dp

private const val SAMPLE_BARCODE = "3850123456782"

@Composable
internal fun HouseholdsIllustration(modifier: Modifier = Modifier) {
    val progress = rememberLoopProgress(durationMillis = 4_500)
    val lineColor = MaterialTheme.colorScheme.outline
    val memberAngles = remember { listOf(-150f, -30f, 90f).map { it * PI.toFloat() / HALF_TURN_DEGREES } }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(IllustrationSize).loopFade(progress)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            memberAngles.forEachIndexed { index, angle ->
                val reach = phase(progress = progress(), start = 0.1f + index * 0.15f, duration = POP_DURATION)
                val end = Offset(x = cos(angle), y = sin(angle)) * (MemberOrbitRadius.toPx() * reach)
                drawLine(color = lineColor, start = center, end = center + end, strokeWidth = MemberLineWidth.toPx())
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(HouseSize).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_add_household),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(HouseIconSize),
            )
        }
        memberAngles.forEachIndexed { index, angle ->
            Avatar(
                photoUrl = null,
                size = AvatarSize.Medium,
                modifier = Modifier
                    .offset(x = MemberOrbitRadius * cos(angle), y = MemberOrbitRadius * sin(angle))
                    .popIn(progress = progress, start = 0.2f + index * 0.15f),
            )
        }
    }
}

@Composable
internal fun ShoppingListIllustration(modifier: Modifier = Modifier) {
    val progress = rememberLoopProgress(durationMillis = 5_000)
    val checked by remember { derivedStateOf { progress() > 0.4f } }
    // The bought row moves below the divider while the other rows and the divider move up by one row.
    val move = { FastOutSlowInEasing.transform(phase(progress = progress(), start = 0.5f, duration = 0.15f)) }
    val remainingItems = listOf(Res.string.onboarding_sample_item_eggs, Res.string.onboarding_sample_item_bread)

    Box(modifier = modifier.size(IllustrationSize).padding(top = padding400).loopFade(progress)) {
        remainingItems.forEachIndexed { listIndex, item ->
            val index = listIndex + 1
            ChecklistRow(
                text = stringResource(item),
                checked = false,
                modifier = Modifier
                    .offset { rowOffset(rows = lerp(index.toFloat(), index - 1f, move())) }
                    .popIn(progress = progress, start = index * 0.08f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(padding200),
            modifier = Modifier
                .height(ListRowHeight)
                .offset { rowOffset(rows = lerp(3f, 2f, move())) }
                .popIn(progress = progress, start = 0.24f),
        ) {
            Text(
                text = stringResource(Res.string.shopping_list_done_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        // Ends one row below the divider (rows 0-1 are the remaining items, row 2 the divider).
        ChecklistRow(
            text = stringResource(Res.string.onboarding_sample_item_milk),
            checked = checked,
            modifier = Modifier
                .offset { rowOffset(rows = lerp(0f, 3f, move())) }
                .popIn(progress = progress, start = 0f)
                .graphicsLayer { alpha = lerp(1f, 0.5f, move()) },
        )
    }
}

/**
 * Mirrors the shopping list: the FAB opens the suggestions drawer from the left, and a tapped suggestion lands in the list.
 * Drawn like a miniature screenshot: laid out at a real phone size with the real components, then scaled down as a whole.
 */
@Composable
internal fun SuggestionsIllustration(modifier: Modifier = Modifier) {
    val progress = rememberLoopProgress(durationMillis = 5_500)

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(IllustrationSize).loopFade(progress)) {
        // No device outline; the clip only bounds the sliding drawer and the scrim.
        Box(
            modifier = Modifier
                .size(width = VirtualPhoneWidth * MINIATURE_SCALE, height = VirtualPhoneHeight * MINIATURE_SCALE)
                .clip(roundedShape400),
        ) {
            ShoppingListMiniature(
                progress = progress,
                modifier = Modifier.miniature(width = VirtualPhoneWidth, height = VirtualPhoneHeight, scale = MINIATURE_SCALE),
            )
        }
    }
}

/** The shopping list with its suggestions drawer at real size; [SuggestionsIllustration] scales it down. */
@Composable
private fun ShoppingListMiniature(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val drawer = {
        FastOutSlowInEasing.transform(phase(progress = progress(), start = 0.2f, duration = 0.12f)) -
            FastOutSlowInEasing.transform(phase(progress = progress(), start = 0.52f, duration = 0.1f))
    }
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val suggestions = listOf(
        Res.string.onboarding_sample_suggestion_bananas,
        Res.string.onboarding_sample_suggestion_yogurt,
        Res.string.onboarding_sample_suggestion_coffee,
    )

    Box(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = padding400)) {
            ChecklistRow(
                text = stringResource(Res.string.onboarding_sample_item_bread),
                checked = false,
                rowHeight = RealListRowHeight,
                contentScale = MINIATURE_ROW_ENLARGEMENT,
            )
            ChecklistRow(
                text = stringResource(Res.string.onboarding_sample_item_eggs),
                checked = false,
                rowHeight = RealListRowHeight,
                contentScale = MINIATURE_ROW_ENLARGEMENT,
            )
            ChecklistRow(
                text = stringResource(Res.string.onboarding_sample_suggestion_yogurt),
                checked = false,
                rowHeight = RealListRowHeight,
                contentScale = MINIATURE_ROW_ENLARGEMENT,
                modifier = Modifier.popIn(progress = progress, start = 0.62f),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(padding400)
                .pressPulse(progress = progress, start = 0.1f)
                .size(RealFabSize)
                .clip(roundedShape400)
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(padding600),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = drawer() }
                .background(scrimColor),
        )
        // Same layout as ShoppingListSuggestionsDrawer.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = DRAWER_FRACTION)
                .graphicsLayer { translationX = -(1f - drawer()) * size.width }
                .background(MaterialTheme.colorScheme.background)
                .padding(padding600),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = padding200),
            ) {
                IconButton(
                    resource = Res.drawable.ic_close,
                    onClick = {},
                    isEnabled = false,
                    appearance = IconButtonAppearance(
                        variant = IconButtonVariant.IconBased,
                        size = IconButtonSize.Medium,
                        tintColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Text(
                    text = stringResource(Res.string.shopping_list_suggestions_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = SINGLE_LINE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = padding600),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(padding200)) {
                suggestions.forEachIndexed { index, suggestion ->
                    TextChip(
                        text = stringResource(suggestion),
                        trailingContent = {
                            IconButton(
                                resource = Res.drawable.ic_plus,
                                onClick = {},
                                isEnabled = false,
                                appearance = IconButtonAppearance(
                                    variant = IconButtonVariant.IconBased,
                                    size = IconButtonSize.Small,
                                    tintColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        },
                        modifier = if (index == 1) Modifier.pressPulse(progress = progress, start = 0.4f) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoyaltyCardsIllustration(modifier: Modifier = Modifier) {
    val progress = rememberLoopProgress(durationMillis = 5_500)
    val flip = { FastOutSlowInEasing.transform(phase(progress = progress(), start = 0.35f, duration = 0.2f)) * HALF_TURN_DEGREES }
    val isBackVisible by remember { derivedStateOf { flip() > HALF_TURN_DEGREES / 2 } }
    val cardModifier = Modifier.fillMaxWidth().height(CardHeight)

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(IllustrationSize).loopFade(progress)) {
        Box(
            modifier = Modifier.graphicsLayer {
                val slide = FastOutSlowInEasing.transform(phase(progress = progress(), start = 0f, duration = 0.2f))
                translationY = (1f - slide) * IllustrationSize.toPx()
                alpha = slide
                rotationY = flip()
                cameraDistance = CARD_CAMERA_DISTANCE * density
            },
        ) {
            if (isBackVisible) {
                // Counter-rotated so the back side is not mirrored. The barcode needs a white surface like on a real card.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = cardModifier
                        .graphicsLayer { rotationY = HALF_TURN_DEGREES }
                        .clip(roundedShape400)
                        .background(CardColorBlue)
                        .padding(padding400),
                ) {
                    Surface(color = Color.White, shape = roundedShape300, modifier = Modifier.fillMaxWidth()) {
                        Barcode(
                            content = SAMPLE_BARCODE,
                            format = LoyaltyCardFormat.Ean13,
                            modifier = Modifier.padding(padding300).fillMaxWidth().height(CardBarcodeHeight),
                        )
                    }
                }
            } else {
                LoyaltyCard(
                    card = PresentableLoyaltyCard(
                        name = stringResource(Res.string.app_name),
                        color = CardColorBlue,
                        codeText = "",
                        format = LoyaltyCardFormat.Ean13,
                    ),
                    modifier = cardModifier,
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    rowHeight: Dp = ListRowHeight,
    contentScale: Float = 1f,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.height(rowHeight)) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .padding(horizontal = padding200 * contentScale)
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                },
        )
        Text(
            text = text,
            style = textStyle.copy(fontSize = textStyle.fontSize * contentScale),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = SINGLE_LINE,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = padding200),
        )
    }
}

/** Returns a reader of the loop progress (0..1), to be called only from layer, offset and draw lambdas or `derivedStateOf`. */
@Composable
private fun rememberLoopProgress(durationMillis: Int): () -> Float {
    val loop = rememberInfiniteTransition(label = "onboardingLoop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "onboardingLoopProgress",
    )
    return remember(loop) { { loop.value } }
}

/** How far (0..1) the step that runs from [start] for [duration] of the loop has got at [progress]. */
private fun phase(
    progress: Float,
    start: Float,
    duration: Float,
): Float = ((progress - start) / duration).coerceIn(0f, 1f)

private fun Density.rowOffset(rows: Float): IntOffset = IntOffset(x = 0, y = (ListRowHeight.toPx() * rows).roundToInt())

private fun Modifier.popIn(
    progress: () -> Float,
    start: Float,
): Modifier =
    graphicsLayer {
        val pop = EaseOutBack.transform(phase(progress = progress(), start = start, duration = POP_DURATION))
        scaleX = pop
        scaleY = pop
        alpha = pop.coerceIn(0f, 1f)
    }

/** A short "tap": shrinks and springs back between [start] and [start] + [PRESS_DURATION]. */
private fun Modifier.pressPulse(
    progress: () -> Float,
    start: Float,
): Modifier =
    graphicsLayer {
        val press = phase(progress = progress(), start = start, duration = PRESS_DURATION)
        val scale = 1f - PRESS_DEPTH * sin(press * PI.toFloat())
        scaleX = scale
        scaleY = scale
    }

/**
 * Lays the content out at a fixed [width] x [height] and draws it scaled by [scale] from the top-start corner, reporting the
 * scaled size so parents place it like any other child of that size.
 */
private fun Modifier.miniature(
    width: Dp,
    height: Dp,
    scale: Float,
): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(Constraints.fixed(width = width.roundToPx(), height = height.roundToPx()))
        layout(width = (placeable.width * scale).roundToInt(), height = (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(x = 0, y = 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
            }
        }
    }

private fun Modifier.loopFade(progress: () -> Float): Modifier =
    graphicsLayer { alpha = 1f - phase(progress = progress(), start = FADE_OUT_START, duration = FADE_OUT_DURATION) }

@Composable
private fun OnboardingIllustrationsPreviewContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(padding400),
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        HouseholdsIllustration()
        ShoppingListIllustration()
        SuggestionsIllustration()
        LoyaltyCardsIllustration()
    }
}

@Preview
@Composable
private fun OnboardingIllustrationsPreviewLightMode() {
    TheAppTheme {
        OnboardingIllustrationsPreviewContent()
    }
}

@Preview
@Composable
private fun OnboardingIllustrationsPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        OnboardingIllustrationsPreviewContent()
    }
}

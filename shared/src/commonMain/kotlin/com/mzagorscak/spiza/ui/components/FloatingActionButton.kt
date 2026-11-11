package com.mzagorscak.spiza.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_plus
import androidx.compose.material3.FloatingActionButton as MaterialFloatingActionButton

private const val PRESSED_SCALE = 0.85f
private const val PRESS_DURATION_MS = 70
private const val HIDDEN_SCALE = 0f

// Bouncy, so the button overshoots a little: the "pop".
private val PopSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

/**
 * The app's FAB: pops in when it appears (also on the first composition), shrinks away when [isVisible] turns false and
 * bounces on every click.
 */
@Composable
fun FloatingActionButton(
    icon: DrawableResource,
    onClick: () -> Unit,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Starts hidden so the button also pops in when the screen first shows it.
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(isVisible) { visibleState.targetState = isVisible }
    val scope = rememberCoroutineScope()
    val clickScale = remember { Animatable(1f) }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = scaleIn(animationSpec = PopSpring, initialScale = HIDDEN_SCALE) + fadeIn(),
        exit = scaleOut(targetScale = HIDDEN_SCALE) + fadeOut(),
        modifier = modifier,
    ) {
        MaterialFloatingActionButton(
            onClick = {
                scope.launch {
                    clickScale.animateTo(PRESSED_SCALE, animationSpec = tween(durationMillis = PRESS_DURATION_MS))
                    clickScale.animateTo(1f, animationSpec = PopSpring)
                }
                onClick()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.graphicsLayer {
                scaleX = clickScale.value
                scaleY = clickScale.value
            },
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FloatingActionButtonPreviewLightMode() {
    TheAppTheme {
        FloatingActionButton(icon = Res.drawable.ic_plus, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FloatingActionButtonPreviewDarkMode() {
    TheAppTheme(darkTheme = true) {
        FloatingActionButton(icon = Res.drawable.ic_plus, onClick = {})
    }
}

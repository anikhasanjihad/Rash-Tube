/*
 * Rash Tube — liquid glass design language.
 * Part of Rash Tube by AHR, a modified version of Flow (A-EDev), licensed under GPL-3.0.
 */
package com.ahr.rashtube.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Flip to false to fall back to the plain Material 3 surfaces. */
const val LIQUID_GLASS_ENABLED = true

private const val TwoPi = 6.2831855f

/**
 * Keeps the palette the user picked but turns the page and every container into glass:
 * the page is transparent (the aurora backdrop shows through) and containers are translucent.
 */
internal fun ColorScheme.toLiquidGlass(): ColorScheme =
    copy(
        background = Color.Transparent,
        surface = Color.Transparent,
        surfaceContainerLowest = surfaceContainerLowest.copy(alpha = 0.42f),
        surfaceContainerLow = surfaceContainerLow.copy(alpha = 0.50f),
        surfaceContainer = surfaceContainer.copy(alpha = 0.58f),
        surfaceContainerHigh = surfaceContainerHigh.copy(alpha = 0.66f),
        surfaceContainerHighest = surfaceContainerHighest.copy(alpha = 0.72f),
        surfaceVariant = surfaceVariant.copy(alpha = 0.55f),
    )

/** Slowly drifting colour fields behind the whole app; this is what the glass refracts. */
@Composable
internal fun LiquidBackdrop(
    base: Color,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val one = scheme.primary
    val two = scheme.tertiary
    val three = scheme.secondary
    val phase =
        rememberInfiniteTransition(label = "liquidBackdrop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 45_000, easing = LinearEasing), RepeatMode.Restart),
            label = "liquidPhase",
        )
    Box(
        modifier =
            modifier.fillMaxSize().drawBehind {
                drawRect(base)
                val t = phase.value * TwoPi
                val radius = max(size.width, size.height) * 0.75f
                fun blob(color: Color, cx: Float, cy: Float, alpha: Float) {
                    val center = Offset(size.width * cx, size.height * cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = radius),
                        radius = radius,
                        center = center,
                    )
                }
                blob(one, 0.25f + 0.20f * sin(t), 0.20f + 0.15f * cos(t), 0.30f)
                blob(two, 0.80f + 0.15f * cos(t + 1.7f), 0.55f + 0.20f * sin(t + 0.9f), 0.24f)
                blob(three, 0.45f + 0.25f * sin(t + 3.4f), 0.95f - 0.15f * cos(t + 2.2f), 0.22f)
            },
    )
}

/** Specular rim and top sheen: the thin bright edge that makes a translucent panel read as glass. */
fun Modifier.glassEdge(shape: Shape = RectangleShape): Modifier =
    if (!LIQUID_GLASS_ENABLED) {
        this
    } else {
        drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            val sheenHeight = minOf(size.height, 56.dp.toPx())
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), endY = sheenHeight),
            )
            drawOutline(
                outline = outline,
                brush =
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.18f)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }

/** A glass card/panel: translucent container colour plus the rim. */
@Composable
fun Modifier.liquidGlass(shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this.background(MaterialTheme.colorScheme.surfaceContainer, shape).glassEdge(shape)

/** Wraps the app in the glass backdrop. */
@Composable
internal fun LiquidGlassHost(
    base: Color,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(base)) {
        LiquidBackdrop(base = base)
        content()
    }
}

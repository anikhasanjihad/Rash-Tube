package com.ahr.rashtube.ui.startup

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashBackground = Color(0xFF0B0B10)
private val SplashRed = Color(0xFFE50914)
private val SmoothOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private const val MIN_SHOW_MS = 1_900L
private const val MAX_SHOW_MS = 3_000L

// One clock drives every part of the intro, so a frame costs a single animation tick.
private const val TIMELINE_MS = 1_800f
private const val LETTER_STAGGER_MS = 60f
private const val LETTER_MS = 700f
private const val BAR_START_MS = 600f
private const val BAR_MS = 700f
private const val CREDIT_START_MS = 900f
private const val CREDIT_MS = 700f

private fun segment(
    nowMs: Float,
    startMs: Float,
    durationMs: Float,
): Float = SmoothOut.transform(((nowMs - startMs) / durationMs).coerceIn(0f, 1f))

/**
 * Animated "RASH TUBE" wordmark that plays over the app on start. It is pure text and drawing, so it
 * is always sharp. [onShown] fires once the first frame is composed so the platform splash can hand
 * over to it, and [contentReady] lets it wait (up to a cap) for the first real screen.
 *
 * Every moving value is read inside a graphicsLayer or draw block, never in composition, so the
 * animation runs on the render thread's layer properties and does not recompose anything per frame
 * while the app is busy starting up behind it.
 */
@Composable
fun RashTextSplash(
    contentReady: () -> Boolean,
    onShown: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val letters = remember { "RASH TUBE".toList() }
    val timeline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val started = SystemClock.uptimeMillis()
        onShown()
        launch { timeline.animateTo(1f, tween(TIMELINE_MS.toInt(), easing = LinearEasing)) }
        delay(MIN_SHOW_MS)
        while (!contentReady() && SystemClock.uptimeMillis() - started < MAX_SHOW_MS) delay(50)
        visible = false
    }

    AnimatedVisibility(visible = visible, exit = fadeOut(tween(450))) {
        val glow = rememberInfiniteTransition(label = "splashGlow")
        val pulse by glow.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
            label = "glow",
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(SplashBackground)
                    .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center,
        ) {
            // The glow only changes alpha, which a layer animates without redrawing the gradient.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = pulse }
                        .drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(SplashRed.copy(alpha = 0.28f), Color.Transparent),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * 0.75f,
                                ),
                            )
                        },
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier.graphicsLayer {
                        val settle = segment(timeline.value * TIMELINE_MS, 0f, TIMELINE_MS)
                        val k = 1.06f - 0.06f * settle
                        scaleX = k
                        scaleY = k
                    },
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    letters.forEachIndexed { index, char ->
                        val tint = if (index < 4) Color.White else SplashRed
                        Text(
                            text = if (char == ' ') "\u00A0" else char.toString(),
                            style =
                                TextStyle(
                                    color = tint,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp,
                                ),
                            modifier =
                                Modifier.graphicsLayer {
                                    val p = segment(timeline.value * TIMELINE_MS, index * LETTER_STAGGER_MS, LETTER_MS)
                                    alpha = p
                                    translationY = (1f - p) * 30.dp.toPx()
                                    val scale = 0.88f + 0.12f * p
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .size(width = 160.dp, height = 4.dp)
                            .graphicsLayer { scaleX = segment(timeline.value * TIMELINE_MS, BAR_START_MS, BAR_MS) }
                            .clip(RoundedCornerShape(2.dp))
                            .background(SplashRed),
                )
            }
            Text(
                text = "DEVELOPED BY AHR",
                style =
                    TextStyle(
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                    ),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 56.dp)
                        .graphicsLayer { alpha = segment(timeline.value * TIMELINE_MS, CREDIT_START_MS, CREDIT_MS) },
            )
        }
    }
}

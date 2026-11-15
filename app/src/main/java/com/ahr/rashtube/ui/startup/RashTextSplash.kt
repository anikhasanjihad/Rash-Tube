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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashTop = Color(0xFF07070B)
private val SplashBottom = Color(0xFF160A12)
private val SplashRed = Color(0xFFE50914)
private val SplashRedBright = Color(0xFFFF3B47)
private val SplashRedDeep = Color(0xFFA8050E)
private val SplashViolet = Color(0xFF6C2BD9)

private val SmoothOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val Overshoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private const val MIN_SHOW_MS = 2_300L
private const val MAX_SHOW_MS = 3_200L

// One clock drives the whole intro; every moving part reads it in a layer or draw block only.
private const val TIMELINE_MS = 2_200f
private const val BADGE_START_MS = 0f
private const val BADGE_MS = 800f
private const val RIPPLE_START_MS = 450f
private const val RIPPLE_MS = 1_100f
private const val LETTER_START_MS = 550f
private const val LETTER_STAGGER_MS = 55f
private const val LETTER_MS = 650f
private const val SHINE_START_MS = 1_250f
private const val SHINE_MS = 800f
private const val LINE_START_MS = 1_000f
private const val LINE_MS = 700f
private const val CREDIT_START_MS = 1_400f
private const val CREDIT_MS = 700f

private fun progress(
    nowMs: Float,
    startMs: Float,
    durationMs: Float,
    easing: CubicBezierEasing = SmoothOut,
): Float = easing.transform(((nowMs - startMs) / durationMs).coerceIn(0f, 1f))

/**
 * The premium "RASH TUBE" intro: a glass-red play badge springs in with a ripple, the wordmark
 * rises letter by letter, a light sweeps across it and a gradient line draws underneath, all over
 * a slowly breathing aurora. [onShown] fires once the first frame is composed so the platform
 * splash can hand over, and [contentReady] lets it wait (up to a cap) for the first real screen.
 *
 * Nothing here recomposes while it plays: every moving value is read inside a graphicsLayer or a
 * draw block, so the animation stays smooth even while the app is busy starting behind it.
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

    AnimatedVisibility(visible = visible, exit = fadeOut(tween(500))) {
        val aurora = rememberInfiniteTransition(label = "splashAurora")
        val drift by aurora.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(5200), RepeatMode.Reverse),
            label = "drift",
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(SplashTop, SplashBottom)))
                    .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center,
        ) {
            // Two soft glows that drift against each other; only layer transforms animate.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = (drift - 0.5f) * size.width * 0.18f
                            translationY = (0.5f - drift) * size.height * 0.05f
                            alpha = 0.55f + 0.45f * drift
                        }.drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(SplashRed.copy(alpha = 0.30f), Color.Transparent),
                                    center = Offset(size.width * 0.5f, size.height * 0.46f),
                                    radius = size.minDimension * 0.85f,
                                ),
                            )
                        },
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = (0.5f - drift) * size.width * 0.22f
                            alpha = 0.35f + 0.35f * (1f - drift)
                        }.drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(SplashViolet.copy(alpha = 0.26f), Color.Transparent),
                                    center = Offset(size.width * 0.78f, size.height * 0.64f),
                                    radius = size.minDimension * 0.7f,
                                ),
                            )
                        },
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // The ripple that leaves the badge.
                    Canvas(modifier = Modifier.size(240.dp)) {
                        val p = progress(timeline.value * TIMELINE_MS, RIPPLE_START_MS, RIPPLE_MS)
                        if (p > 0f && p < 1f) {
                            drawCircle(
                                color = SplashRedBright.copy(alpha = 0.5f * (1f - p)),
                                radius = size.minDimension / 2f * (0.30f + 0.70f * p),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                    // The badge: a glass-red disc with a white play mark.
                    Canvas(
                        modifier =
                            Modifier
                                .size(84.dp)
                                .graphicsLayer {
                                    val p = progress(timeline.value * TIMELINE_MS, BADGE_START_MS, BADGE_MS, Overshoot)
                                    val scale = 0.55f + 0.45f * p
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = progress(timeline.value * TIMELINE_MS, BADGE_START_MS, BADGE_MS * 0.6f)
                                },
                    ) {
                        val r = size.minDimension / 2f
                        val c = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            brush = Brush.linearGradient(listOf(SplashRedBright, SplashRedDeep)),
                            radius = r,
                            center = c,
                        )
                        drawCircle(
                            brush =
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                                    endY = size.height * 0.62f,
                                ),
                            radius = r,
                            center = c,
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = r - 0.75.dp.toPx(),
                            center = c,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                        val play =
                            Path().apply {
                                moveTo(c.x - r * 0.20f, c.y - r * 0.36f)
                                lineTo(c.x + r * 0.42f, c.y)
                                lineTo(c.x - r * 0.20f, c.y + r * 0.36f)
                                close()
                            }
                        drawPath(play, Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // The wordmark, with a band of light that sweeps across the letters only.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val p = progress(timeline.value * TIMELINE_MS, SHINE_START_MS, SHINE_MS)
                                if (p > 0f && p < 1f) {
                                    val band = size.width * 0.22f
                                    val x = -band + (size.width + band * 2f) * p
                                    drawRect(
                                        brush =
                                            Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
                                                startX = x - band,
                                                endX = x + band,
                                            ),
                                        blendMode = BlendMode.SrcAtop,
                                    )
                                }
                            },
                ) {
                    letters.forEachIndexed { index, char ->
                        Text(
                            text = if (char == ' ') "\u00A0" else char.toString(),
                            style =
                                TextStyle(
                                    color = if (index < 4) Color.White else SplashRedBright,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp,
                                ),
                            modifier =
                                Modifier.graphicsLayer {
                                    val p = progress(timeline.value * TIMELINE_MS, LETTER_START_MS + index * LETTER_STAGGER_MS, LETTER_MS)
                                    alpha = p
                                    translationY = (1f - p) * 22.dp.toPx()
                                },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier =
                        Modifier
                            .size(width = 150.dp, height = 3.dp)
                            .graphicsLayer {
                                scaleX = progress(timeline.value * TIMELINE_MS, LINE_START_MS, LINE_MS)
                            }.clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(SplashViolet, SplashRedBright))),
                )
            }

            Text(
                text = "DEVELOPED BY AHR",
                style =
                    TextStyle(
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                    ),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 56.dp)
                        .graphicsLayer { alpha = progress(timeline.value * TIMELINE_MS, CREDIT_START_MS, CREDIT_MS) },
            )
        }
    }
}

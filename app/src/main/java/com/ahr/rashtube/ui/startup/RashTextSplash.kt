package com.ahr.rashtube.ui.startup

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
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
private const val MIN_SHOW_MS = 2_500L
private const val MAX_SHOW_MS = 3_800L
private const val LETTER_STAGGER_MS = 70L

/**
 * Animated "RASH TUBE" wordmark that plays over the app on start. It is pure text and drawing, so it
 * is always sharp. [onShown] fires once the first frame is composed so the platform splash can hand
 * over to it, and [contentReady] lets it wait (up to a cap) for the first real screen.
 */
@Composable
fun RashTextSplash(
    contentReady: () -> Boolean,
    onShown: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val letters = remember { "RASH TUBE".toList() }
    val progress = remember { letters.map { Animatable(0f) } }
    val bar = remember { Animatable(0f) }
    val credit = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val started = SystemClock.uptimeMillis()
        onShown()
        progress.forEachIndexed { index, anim ->
            launch {
                delay(index * LETTER_STAGGER_MS)
                anim.animateTo(1f, tween(900, easing = SmoothOut))
            }
        }
        launch { settle.animateTo(1f, tween(2200, easing = SmoothOut)) }
        launch {
            delay(letters.size * LETTER_STAGGER_MS + 150)
            bar.animateTo(1f, tween(900, easing = SmoothOut))
        }
        launch {
            delay(letters.size * LETTER_STAGGER_MS + 500)
            credit.animateTo(1f, tween(900, easing = SmoothOut))
        }
        delay(MIN_SHOW_MS)
        while (!contentReady() && SystemClock.uptimeMillis() - started < MAX_SHOW_MS) delay(50)
        visible = false
    }

    AnimatedVisibility(visible = visible, exit = fadeOut(tween(650)) + scaleOut(targetScale = 1.05f, animationSpec = tween(650))) {
        val pulse by rememberInfiniteTransition(label = "splashGlow").animateFloat(
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
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(SplashRed.copy(alpha = 0.28f * pulse), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.75f,
                            ),
                        )
                    }.pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier.graphicsLayer {
                        val k = 1.06f - 0.06f * settle.value
                        scaleX = k
                        scaleY = k
                    },
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    letters.forEachIndexed { index, char ->
                        val p = progress[index].value
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
                            .height(4.dp)
                            .width(160.dp * bar.value)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SplashRed),
                )
            }
            Text(
                text = "DEVELOPED BY AHR",
                style =
                    TextStyle(
                        color = Color.White.copy(alpha = 0.62f * credit.value),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                    ),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
            )
        }
    }
}

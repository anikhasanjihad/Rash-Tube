package com.ahr.rashtube.ui.components.musicplayer.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * The progress bar the way iOS draws it: a thin capsule that thickens under the finger, with no
 * thumb. Touch only reads and writes through [onValueChange] and [onValueChangeFinished]; the
 * fill and thickness are read while drawing, so a position tick redraws instead of re-measuring.
 */
@Composable
internal fun IosProgressBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val end = valueRange.endInclusive.coerceAtLeast(1f)
    var pressed by remember { mutableStateOf(false) }
    val thickness by animateDpAsState(
        targetValue = if (pressed) 12.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iosProgressThickness",
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val fillColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)
    val fraction = (value / end).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(end) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        latestChange((down.position.x / size.width).coerceIn(0f, 1f) * end)
                        drag(down.id) { change ->
                            change.consume()
                            latestChange((change.position.x / size.width).coerceIn(0f, 1f) * end)
                        }
                        pressed = false
                        latestFinished()
                    }
                }.drawBehind {
                    val h = thickness.toPx()
                    val top = (size.height - h) / 2f
                    val radius = CornerRadius(h / 2f, h / 2f)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(0f, top),
                        size = Size(size.width, h),
                        cornerRadius = radius,
                    )
                    val filled = size.width * fraction
                    if (filled > 0f) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(0f, top),
                            size = Size(filled.coerceAtLeast(h), h),
                            cornerRadius = radius,
                        )
                    }
                },
    )
}

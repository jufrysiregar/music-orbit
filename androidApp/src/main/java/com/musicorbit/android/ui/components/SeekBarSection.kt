package com.musicorbit.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.musicorbit.android.ui.util.formatDuration

@Composable
fun SeekBarSection(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0L) position.toFloat() / duration.toFloat() else 0f
    var isDragging by remember { mutableStateOf(false) }
    var dragValue  by remember { mutableFloatStateOf(progress) }

    val trackColor     = MaterialTheme.colorScheme.surfaceVariant
    val progressStart  = MaterialTheme.colorScheme.primary
    val progressEnd    = MaterialTheme.colorScheme.tertiary
    val thumbColor     = MaterialTheme.colorScheme.primary

    val displayProgress = if (isDragging) dragValue else progress

    Column(modifier = modifier.fillMaxWidth()) {
        // Custom seek bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0L) {
                            val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((newProgress * duration).toLong())
                        }
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragValue = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (duration > 0L) {
                                onSeek((dragValue * duration).toLong())
                            }
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragValue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            val trackHeight = 7.dp.toPx()
            val thumbRadius = 9.dp.toPx()
            val centerY = size.height / 2f
            val trackStart = thumbRadius
            val trackWidth = size.width - (thumbRadius * 2)

            // Background track
            drawRoundRect(
                color      = trackColor,
                topLeft    = Offset(trackStart, centerY - trackHeight / 2f),
                size       = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            // Progress track with gradient
            val progressWidth = (displayProgress * trackWidth).coerceAtLeast(0f)
            if (progressWidth > 0f) {
                drawRoundRect(
                    brush        = Brush.horizontalGradient(
                        colors = listOf(progressStart, progressEnd),
                        startX = trackStart,
                        endX   = trackStart + trackWidth
                    ),
                    topLeft      = Offset(trackStart, centerY - trackHeight / 2f),
                    size         = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            // Thumb
            val thumbX = trackStart + progressWidth
            // Glow halo behind the thumb
            drawCircle(
                brush   = Brush.radialGradient(
                    colors = listOf(thumbColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(thumbX, centerY),
                    radius = thumbRadius * 2.6f
                ),
                radius  = thumbRadius * 2.6f,
                center  = Offset(thumbX, centerY)
            )
            drawCircle(
                color   = thumbColor,
                radius  = thumbRadius,
                center  = Offset(thumbX, centerY)
            )
            // Thumb inner highlight
            drawCircle(
                color   = Color.White,
                radius  = thumbRadius * 0.4f,
                center  = Offset(thumbX, centerY)
            )
        }

        // Time labels
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = formatDuration(if (isDragging) (dragValue * duration).toLong() else position),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

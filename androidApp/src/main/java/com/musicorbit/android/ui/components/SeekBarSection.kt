package com.musicorbit.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value            = if (isDragging) dragValue else progress,
            onValueChange    = { dragValue = it; isDragging = true },
            onValueChangeFinished = {
                onSeek((dragValue * duration).toLong())
                isDragging = false
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = formatDuration(if (isDragging) (dragValue * duration).toLong() else position),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text  = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

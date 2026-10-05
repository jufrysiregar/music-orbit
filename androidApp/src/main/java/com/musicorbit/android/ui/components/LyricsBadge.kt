package com.musicorbit.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small "[LIRIK]" badge shown on song list items that have saved lyrics. */
@Composable
fun LyricsBadge(modifier: Modifier = Modifier) {
    Text(
        text     = "[LIRIK]",
        fontSize = 10.sp,
        color    = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .border(
                width  = 1.dp,
                color  = MaterialTheme.colorScheme.primary,
                shape  = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

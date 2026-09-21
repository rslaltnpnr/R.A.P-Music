package com.ozin.music.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compact tap-to-rate 5-star row. Tapping a star sets the rating to that
 * star's position (1-5); tapping the currently-set star again clears it back
 * to 0 (unrated).
 */
@Composable
fun RatingStars(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            val filled = star <= rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "$star star" + if (star != 1) "s" else "",
                tint = if (filled) filledColor else emptyColor,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onRatingChange(if (star == rating) 0 else star) },
            )
        }
    }
}

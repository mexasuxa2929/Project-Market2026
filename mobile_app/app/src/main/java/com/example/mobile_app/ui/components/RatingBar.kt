package com.example.mobile_app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.ui.theme.StarColor
import com.example.mobile_app.ui.theme.TextSecondary

/**
 * Yulduzli reyting ko'rsatgich.
 * showCount=true bo'lsa "(850)" ko'rinishida review soni ham ko'rsatiladi.
 */
@Composable
fun RatingBar(
    rating: Double,
    reviewCount: Long = 0,
    starSize: Dp = 14.dp,
    fontSize: TextUnit = 12.sp,
    showCount: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // 5 ta yulduz
        (1..5).forEach { i ->
            val icon = when {
                i <= rating.toInt()      -> Icons.Default.Star
                i - 0.5 <= rating       -> Icons.AutoMirrored.Filled.StarHalf
                else                    -> Icons.Outlined.StarBorder
            }
            Icon(imageVector = icon,
                contentDescription = null,
                tint = if (rating > 0) StarColor else Color(0xFFD1D5DB),
                modifier = Modifier.size(starSize),
            )
        }

        if (rating > 0) {
            Text(
                " %.1f".format(rating),
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827),
            )
        }
        if (showCount && reviewCount > 0) {
            Text(
                "($reviewCount)",
                fontSize = (fontSize.value - 1).sp,
                color = TextSecondary,
            )
        }
    }
}

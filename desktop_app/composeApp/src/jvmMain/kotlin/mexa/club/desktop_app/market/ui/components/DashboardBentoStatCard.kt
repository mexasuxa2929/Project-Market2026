package mexa.club.desktop_app.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DashboardBentoStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    badgeText: String,
    badgeBackground: Color,
    badgeForeground: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .background(iconBackground, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBackground,
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeForeground,
                    )
                }
            }
            Column {
                Text(
                    label.uppercase(),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.onSurfaceVariant,
                )
                Text(
                    value,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = MexaWarehouseColors.onSurface,
                )
            }
        }
    }
}

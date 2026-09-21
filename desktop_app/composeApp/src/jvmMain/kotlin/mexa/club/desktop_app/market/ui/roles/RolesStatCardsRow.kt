package mexa.club.desktop_app.market.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.model.RoleScreenStats
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun RolesStatCardsRow(stats: RoleScreenStats, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val twoColumns = maxWidth < 700.dp
        val gap = 12.dp
        if (twoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    RoleStatCard(
                        value = stats.totalRoles.toString(),
                        label = "Jami rollar",
                        icon = Icons.Default.Shield,
                        iconTint = MexaWarehouseColors.indigoAccent,
                        iconBg = MexaWarehouseColors.statIndigoBg,
                        modifier = Modifier.weight(1f),
                    )
                    RoleStatCard(
                        value = stats.totalUsers.toString(),
                        label = "Jami foydalanuvchilar",
                        icon = Icons.Default.Group,
                        iconTint = MexaWarehouseColors.statCyanFg,
                        iconBg = MexaWarehouseColors.statCyanBg,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    RoleStatCard(
                        value = stats.activeUsers.toString(),
                        label = "Faol foydalanuvchilar",
                        icon = Icons.Default.CheckCircle,
                        iconTint = MexaWarehouseColors.statusActiveFg,
                        iconBg = MexaWarehouseColors.statusActiveBg,
                        modifier = Modifier.weight(1f),
                    )
                    RoleStatCard(
                        value = stats.blockedUsers.toString(),
                        label = "Bloklangan",
                        icon = Icons.Default.Warning,
                        iconTint = MexaWarehouseColors.danger,
                        iconBg = MexaWarehouseColors.statusBlockedBg,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                RoleStatCard(
                    value = stats.totalRoles.toString(),
                    label = "Jami rollar",
                    icon = Icons.Default.Shield,
                    iconTint = MexaWarehouseColors.indigoAccent,
                    iconBg = MexaWarehouseColors.statIndigoBg,
                    modifier = Modifier.weight(1f),
                )
                RoleStatCard(
                    value = stats.totalUsers.toString(),
                    label = "Jami foydalanuvchilar",
                    icon = Icons.Default.Group,
                    iconTint = MexaWarehouseColors.statCyanFg,
                    iconBg = MexaWarehouseColors.statCyanBg,
                    modifier = Modifier.weight(1f),
                )
                RoleStatCard(
                    value = stats.activeUsers.toString(),
                    label = "Faol foydalanuvchilar",
                    icon = Icons.Default.CheckCircle,
                    iconTint = MexaWarehouseColors.statusActiveFg,
                    iconBg = MexaWarehouseColors.statusActiveBg,
                    modifier = Modifier.weight(1f),
                )
                RoleStatCard(
                    value = stats.blockedUsers.toString(),
                    label = "Bloklangan",
                    icon = Icons.Default.Warning,
                    iconTint = MexaWarehouseColors.danger,
                    iconBg = MexaWarehouseColors.statusBlockedBg,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RoleStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MexaWarehouseColors.textMuted,
            )
            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                color = MexaWarehouseColors.textPrimary,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

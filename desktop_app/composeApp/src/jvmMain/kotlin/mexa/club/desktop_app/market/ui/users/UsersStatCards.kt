package mexa.club.desktop_app.market.ui.users

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun UsersStatCardsRow(
    totalCount: Int,
    adminCount: Int,
    warehouseCount: Int,
    activeSessions: Int,
    modifier: Modifier = Modifier,
) {
    val lang = appLanguage()

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val twoColumns = maxWidth < 700.dp
        val gap = 16.dp
        if (twoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    StatCard(
                        title = AppStrings.usersStatTotal(lang),
                        icon = Icons.Default.Group,
                        count = totalCount,
                        subtitle = AppStrings.usersStatTotalSub(lang),
                        iconTint = MexaWarehouseColors.indigoAccent,
                        iconBg = MexaWarehouseColors.statIndigoBg,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = AppStrings.usersStatAdmins(lang),
                        icon = Icons.Default.Shield,
                        count = adminCount,
                        subtitle = AppStrings.usersStatAdminsSub(lang),
                        iconTint = MexaWarehouseColors.primary,
                        iconBg = MexaWarehouseColors.infoBadgeBg,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    StatCard(
                        title = AppStrings.usersStatWarehouse(lang),
                        icon = Icons.Default.Warehouse,
                        count = warehouseCount,
                        subtitle = AppStrings.usersStatWarehouseSub(lang),
                        iconTint = androidx.compose.ui.graphics.Color(0xFF0369A1),
                        iconBg = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = AppStrings.usersStatSessions(lang),
                        icon = Icons.Default.AccessTime,
                        count = activeSessions,
                        subtitle = AppStrings.usersStatSessionsSub(lang),
                        iconTint = androidx.compose.ui.graphics.Color(0xFFD97706),
                        iconBg = MexaWarehouseColors.amberBadgeBg,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                StatCard(
                    title = AppStrings.usersStatTotal(lang),
                    icon = Icons.Default.Group,
                    count = totalCount,
                    subtitle = AppStrings.usersStatTotalSub(lang),
                    iconTint = MexaWarehouseColors.indigoAccent,
                    iconBg = MexaWarehouseColors.statIndigoBg,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = AppStrings.usersStatAdmins(lang),
                    icon = Icons.Default.Shield,
                    count = adminCount,
                    subtitle = AppStrings.usersStatAdminsSub(lang),
                    iconTint = MexaWarehouseColors.primary,
                    iconBg = MexaWarehouseColors.infoBadgeBg,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = AppStrings.usersStatWarehouse(lang),
                    icon = Icons.Default.Warehouse,
                    count = warehouseCount,
                    subtitle = AppStrings.usersStatWarehouseSub(lang),
                    iconTint = androidx.compose.ui.graphics.Color(0xFF0369A1),
                    iconBg = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = AppStrings.usersStatSessions(lang),
                    icon = Icons.Default.AccessTime,
                    count = activeSessions,
                    subtitle = AppStrings.usersStatSessionsSub(lang),
                    iconTint = androidx.compose.ui.graphics.Color(0xFFD97706),
                    iconBg = MexaWarehouseColors.amberBadgeBg,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    icon: ImageVector,
    count: Int,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = MexaWarehouseColors.indigoAccent,
    iconBg: androidx.compose.ui.graphics.Color = MexaWarehouseColors.statIndigoBg,
) {
    Row(
        modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textMuted)
            Text(count.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, color = MexaWarehouseColors.textPrimary)
            Text(subtitle, fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
        }
        Box(
            Modifier.size(44.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
    }
}

package mexa.club.desktop_app.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.HorizontalDivider
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
import mexa.club.desktop_app.market.navigation.AdminNavScreen
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

data class AdminSidebarItem(
    val title: String,
    val screen: AdminNavScreen,
    val icon: ImageVector,
)

@Composable
fun AdminSidebar(
    items: List<AdminSidebarItem>,
    selected: AdminNavScreen,
    onSelect: (AdminNavScreen) -> Unit,
    title: String,
    subtitle: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight(),
        color = MexaWarehouseColors.sidebarBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
    Column(Modifier.fillMaxHeight()) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text(
                title,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.3).sp,
            )
            Text(
                subtitle.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MexaWarehouseColors.sidebarSubtitle.copy(alpha = 0.7f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        HorizontalDivider(color = MexaWarehouseColors.outlineVariant.copy(alpha = 0.2f))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            items.forEach { item ->
                val active = item.screen == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .background(
                            if (active) MexaWarehouseColors.activeNavBg else Color.Transparent,
                        )
                        .clickable { onSelect(item.screen) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .background(
                                if (active) MexaWarehouseColors.primaryContainer else Color.Transparent,
                            ),
                    )
                    Row(
                        Modifier.padding(start = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fg = when {
                            active -> MexaWarehouseColors.primaryFixedDim
                            else -> MexaWarehouseColors.sidebarNavMuted
                        }
                        Icon(item.icon, contentDescription = null, tint = fg)
                        Text(
                            item.title,
                            color = if (active) Color.White else MexaWarehouseColors.sidebarNavMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MexaWarehouseColors.outlineVariant.copy(alpha = 0.2f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MexaWarehouseColors.sidebarNavMuted)
            Text(
                "Chiqish",
                color = MexaWarehouseColors.sidebarNavMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
}

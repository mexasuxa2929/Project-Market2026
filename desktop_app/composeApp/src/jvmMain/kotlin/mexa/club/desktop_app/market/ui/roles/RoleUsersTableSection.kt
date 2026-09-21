package mexa.club.desktop_app.market.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.model.AuthProvider
import mexa.club.desktop_app.market.model.RoleDefinition
import mexa.club.desktop_app.market.model.RoleUserRow
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.ui.roles.common.RolePillBadge
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.users.common.UserAvatarCircle

@Composable
fun RoleUsersTableSection(
    users: List<RoleUserRow>,
    roles: List<RoleDefinition>,
    tabFilter: String?,
    onTabChange: (String?) -> Unit,
    tabCount: (String?) -> Int,
    onReassignRole: (RoleUserRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            RoleTabBar(tabFilter, onTabChange, tabCount, roles)
        }
        RoleTableHeader()
        if (users.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Foydalanuvchilar topilmadi", color = MexaWarehouseColors.textMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.height((users.size.coerceAtMost(8) * 56).dp.coerceAtLeast(168.dp))) {
                items(users, key = { it.id }) { user ->
                    RoleUserTableRow(user, onReassign = { onReassignRole(user) })
                }
            }
        }
    }
}

@Composable
private fun RoleTabBar(
    selected: String?,
    onSelect: (String?) -> Unit,
    tabCount: (String?) -> Int,
    roles: List<RoleDefinition>,
) {
    Row(
        Modifier
            .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // "Hammasi" tab — har doim birinchi
        RoleTabChip(
            label = "Hammasi",
            count = tabCount(null),
            active = selected == null,
            onClick = { onSelect(null) },
        )
        // Dinamik tablar — DB dan olingan rollar
        roles.forEach { role ->
            val label = role.name.ifBlank { role.code.removePrefix("ROLE_") }
            RoleTabChip(
                label = label,
                count = tabCount(role.code),
                active = selected == role.code,
                onClick = { onSelect(role.code) },
            )
        }
    }
}

@Composable
private fun RoleTabChip(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clickable { onClick() }
            .background(
                if (active) MexaWarehouseColors.indigoAccent else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            "$label ($count)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) Color.White else MexaWarehouseColors.textMuted,
        )
    }
}


@Composable
private fun RoleTableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MexaWarehouseColors.inputBg)
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("FOYDALANUVCHI", Modifier.weight(2.2f))
        HeaderCell("ROL", Modifier.weight(1f))
        HeaderCell("PROVIDER", Modifier.weight(1f))
        HeaderCell("HOLAT", Modifier.weight(1f))
        HeaderCell("AMALLAR", Modifier.width(72.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MexaWarehouseColors.textCaption,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun RoleUserTableRow(user: RoleUserRow, onReassign: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val btnInteraction = remember { MutableInteractionSource() }
    val btnHovered by btnInteraction.collectIsHoveredAsState()

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .hoverable(interaction)
                .background(if (hovered) MexaWarehouseColors.inputBg else Color.Transparent)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UserAvatarCircle(user.fullName, size = 32.dp)
                Column {
                    Text(user.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text(user.email, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                }
            }
            Box(Modifier.weight(1f)) { RolePillBadge(user.role) }
            Box(Modifier.weight(1f)) { ProviderCell(user.provider) }
            Box(Modifier.weight(1f)) { StatusCell(user.status) }
            Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onReassign,
                    modifier = Modifier
                        .hoverable(btnInteraction)
                        .size(32.dp)
                        .background(
                            if (btnHovered) MexaWarehouseColors.statIndigoBg else Color.Transparent,
                            CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Rolni o'zgartirish",
                        tint = if (btnHovered) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.textCaption,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.backgroundPage))
    }
}

@Composable
private fun ProviderCell(provider: AuthProvider) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        when (provider) {
            AuthProvider.LOCAL -> Icon(Icons.Default.Computer, contentDescription = null, tint = MexaWarehouseColors.textCaption, modifier = Modifier.size(16.dp))
            AuthProvider.GOOGLE -> Text("G", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
        }
        Text(
            if (provider == AuthProvider.GOOGLE) "Google" else "Local",
            fontSize = 13.sp,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun StatusCell(status: UserStatus) {
    val (dotColor, label, textColor) = when (status) {
        UserStatus.ACTIVE -> Triple(MexaWarehouseColors.statusActiveFg, "Faol", MexaWarehouseColors.statusActiveFg)
        UserStatus.BLOCKED -> Triple(MexaWarehouseColors.danger, "Bloklangan", MexaWarehouseColors.danger)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(dotColor, CircleShape))
        Text(label, fontSize = 13.sp, color = textColor)
    }
}

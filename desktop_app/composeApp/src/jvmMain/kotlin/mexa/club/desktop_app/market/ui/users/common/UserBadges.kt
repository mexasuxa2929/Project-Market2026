package mexa.club.desktop_app.market.ui.users.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.model.AuthProvider
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.model.backendRoleToUserRole
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import kotlin.math.abs

@Composable
fun RoleBadge(role: UserRole, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (role) {
        UserRole.SUPER_ADMIN -> Triple("SUPER ADMIN", MexaWarehouseColors.roleSuperAdminBg, Color.White)
        UserRole.ADMIN -> Triple("ADMIN", MexaWarehouseColors.roleAdminBg, MexaWarehouseColors.roleAdminFg)
        UserRole.WAREHOUSE -> Triple("WAREHOUSE", MexaWarehouseColors.roleWarehouseBg, MexaWarehouseColors.roleWarehouseFg)
        UserRole.COURIER -> Triple("COURIER", MexaWarehouseColors.roleCourierBg, MexaWarehouseColors.roleCourierFg)
        UserRole.USER -> Triple("USER", MexaWarehouseColors.roleUserBg, MexaWarehouseColors.roleUserFg)
    }
    Box(
        modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Xom rol kodi bilan ishlaydi — tanilgan rollar uchun rang schemasi,
 * custom (noma'lum) rollar uchun indigo badge.
 * Masalan: "ROLE_MANAGER" → "MANAGER" badge.
 */
@Composable
fun RoleBadge(rawRoleName: String, modifier: Modifier = Modifier) {
    val known = backendRoleToUserRole(rawRoleName)
    if (known != null) {
        RoleBadge(known, modifier)
    } else {
        val label = rawRoleName.removePrefix("ROLE_").uppercase()
        Box(
            modifier
                .background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(label, color = MexaWarehouseColors.indigoAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatusBadge(status: UserStatus, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (status) {
        UserStatus.ACTIVE -> Triple("ACTIVE", MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg)
        UserStatus.BLOCKED -> Triple("BLOCKED", MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg)
    }
    Box(
        modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthBadge(provider: AuthProvider, modifier: Modifier = Modifier) {
    val label = when (provider) {
        AuthProvider.LOCAL -> "LOCAL"
        AuthProvider.GOOGLE -> "GOOGLE"
    }
    Box(
        modifier
            .background(MexaWarehouseColors.tableHeaderBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, color = MexaWarehouseColors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun UserAvatarCircle(fullName: String, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val initials = fullName.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.firstOrNull()?.uppercaseChar()?.toString() ?: "" }
        .ifBlank { "?" }
    val bgColor = remember(fullName) { avatarColorFromName(fullName) }
    Box(
        modifier.then(Modifier.size(size)).background(bgColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = Color.White,
            fontSize = (size.value * 0.35f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

fun avatarColorFromName(name: String): Color {
    val colors = listOf(
        Color(0xFF6366F1),
        Color(0xFF0EA5E9),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
    )
    return colors[kotlin.math.abs(name.hashCode()) % colors.size]
}

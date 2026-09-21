package mexa.club.desktop_app.market.ui.roles.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

/**
 * Rol badge'i — raw backend kodi (masalan "ROLE_ADMIN", "ROLE_MANAGER") ni qabul qiladi.
 * Tanilgan 5 ta standart rol uchun ranglar saqlanadi; noma'lum rollar uchun indigo badge.
 */
@Composable
fun RolePillBadge(roleCode: String, modifier: Modifier = Modifier) {
    val (label, bg, fg, border) = when (roleCode) {
        "ROLE_SUPER_ADMIN" -> Quad("SUPER ADMIN",
            MexaWarehouseColors.roleSuperAdminBg, Color.White,
            MexaWarehouseColors.roleSuperAdminBg)
        "ROLE_ADMIN" -> Quad("ADMIN",
            MexaWarehouseColors.roleAdminBg, MexaWarehouseColors.roleAdminFg,
            MexaWarehouseColors.roleAdminBorder)
        "ROLE_WAREHOUSE", "ROLE_WAREHOUSE_MANAGER" -> Quad("WAREHOUSE",
            MexaWarehouseColors.roleWarehouseBg, MexaWarehouseColors.roleWarehouseFg,
            MexaWarehouseColors.roleWarehouseBorder)
        "ROLE_COURIER" -> Quad("COURIER",
            MexaWarehouseColors.roleCourierBg, MexaWarehouseColors.roleCourierFg,
            MexaWarehouseColors.roleCourierBorder)
        "ROLE_USER" -> Quad("USER",
            MexaWarehouseColors.roleUserBg, MexaWarehouseColors.roleUserFg,
            MexaWarehouseColors.roleUserBorder)
        else -> {
            // Custom / noma'lum rollar → "ROLE_" prefiksini olib tashlab, indigo rang
            val displayLabel = roleCode.removePrefix("ROLE_").uppercase()
            Quad(displayLabel,
                MexaWarehouseColors.infoBadgeBg, MexaWarehouseColors.indigoAccent,
                MexaWarehouseColors.roleAdminBorder)
        }
    }
    Box(
        modifier
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private data class Quad(val label: String, val bg: Color, val fg: Color, val border: Color)

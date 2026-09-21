package mexa.club.desktop_app.market.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mexa.club.desktop_app.market.model.RoleDefinition
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Grid ─────────────────────────────────────────────────────────────────────

@Composable
fun RoleCardsGrid(
    roles: List<RoleDefinition>,
    showEmpty: Boolean,
    onClearFilters: () -> Unit,
    onEditRole: (RoleDefinition) -> Unit = {},
    onDeleteRole: (RoleDefinition) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedRole by remember { mutableStateOf<RoleDefinition?>(null) }

    // Permissions dialog
    selectedRole?.let { role ->
        RolePermissionsDialog(
            role = role,
            onDismiss = { selectedRole = null },
        )
    }

    if (showEmpty) {
        RolesEmptyState(onClearFilters = onClearFilters, modifier = modifier.fillMaxWidth())
        return
    }

    val regular   = roles.filter { !it.fullWidth }
    val fullWidth  = roles.filter {  it.fullWidth }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth < 600.dp  -> 1
            maxWidth < 1100.dp -> 2
            else               -> 3
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            regular.chunked(columns).forEach { rowRoles ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowRoles.forEach { role ->
                        RoleCard(
                            role         = role,
                            modifier     = Modifier.weight(1f),
                            onClick      = { selectedRole = role },
                            onEditRole   = { onEditRole(role) },
                            onDeleteRole = { onDeleteRole(role) },
                        )
                    }
                    repeat(columns - rowRoles.size) { Box(Modifier.weight(1f)) }
                }
            }
            fullWidth.forEach { role ->
                RoleCard(
                    role         = role,
                    modifier     = Modifier.fillMaxWidth(),
                    onClick      = { selectedRole = role },
                    onEditRole   = { onEditRole(role) },
                    onDeleteRole = { onDeleteRole(role) },
                )
            }
        }
    }
}

// ─── Card ──────────────────────────────────────────────────────────────────────

@Composable
private fun RoleCard(
    role: RoleDefinition,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEditRole: () -> Unit,
    onDeleteRole: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val (icon, iconBg, iconTint) = roleIconStyle(role.code)

    Column(
        modifier
            .hoverable(interaction)
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (hovered) MexaWarehouseColors.cardHoverBorder else MexaWarehouseColors.borderSubtle,
                RoundedCornerShape(12.dp),
            )
            .background(
                if (hovered) MexaWarehouseColors.cardHoverBg else MexaWarehouseColors.surfaceLowest,
                RoundedCornerShape(12.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Header: icon + name/code + user count ─────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    Modifier.size(32.dp).background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            role.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        if (role.isSystemRole) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Tizim roli",
                                tint = MexaWarehouseColors.textCaption,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    Text(
                        role.code,
                        fontSize = 11.sp,
                        color = MexaWarehouseColors.textCaption,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(
                Modifier
                    .border(1.dp, MexaWarehouseColors.roleAdminBorder, RoundedCornerShape(12.dp))
                    .background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "${role.userCount} ta",
                    fontSize = 12.sp,
                    color = MexaWarehouseColors.indigoAccent,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ── Description (qisqa) ───────────────────────────────────────────────
        if (role.description.isNotBlank()) {
            Text(
                role.description,
                fontSize = 12.sp,
                color = MexaWarehouseColors.textMuted,
                fontStyle = if (role.fullWidth) FontStyle.Italic else FontStyle.Normal,
                lineHeight = 18.sp,
                maxLines = 2,
            )
        }

        // ── Permissions hint ──────────────────────────────────────────────────
        if (role.permissions.isNotEmpty()) {
            Text(
                "${role.permissions.size} ta ruxsat • bosing ko'rish uchun",
                fontSize = 11.sp,
                color = MexaWarehouseColors.indigoAccent,
                fontWeight = FontWeight.Medium,
            )
        }

        // ── Action buttons ────────────────────────────────────────────────────
        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Edit button — visible for all roles (dialog shows read-only for system)
            IconButton(
                onClick = {
                    onEditRole()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Tahrirlash",
                    tint = MexaWarehouseColors.indigoAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
            // Delete button — disabled for system roles
            if (!role.isSystemRole) {
                IconButton(
                    onClick = {
                        onDeleteRole()
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "O'chirish",
                        tint = MexaWarehouseColors.danger,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ─── Permissions Dialog ────────────────────────────────────────────────────────

@Composable
private fun RolePermissionsDialog(
    role: RoleDefinition,
    onDismiss: () -> Unit,
) {
    val (icon, iconBg, iconTint) = roleIconStyle(role.code)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(440.dp),
            shape = RoundedCornerShape(14.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 8.dp,
        ) {
            Column {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(36.dp).background(iconBg, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                role.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.textPrimary,
                            )
                            Text(
                                role.code,
                                fontSize = 11.sp,
                                color = MexaWarehouseColors.textCaption,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Yopish",
                            tint = MexaWarehouseColors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Body ──────────────────────────────────────────────────────
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

                    // Description
                    if (role.description.isNotBlank()) {
                        Text(
                            role.description,
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.textMuted,
                            lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // Permissions list
                    Text(
                        "Ruxsatlar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textPrimary,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (role.permissions.size > 6)
                                    Modifier.height(240.dp)
                                else
                                    Modifier.wrapContentHeight()
                            )
                            .background(MexaWarehouseColors.inputBg, RoundedCornerShape(10.dp))
                            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                    ) {
                        if (role.permissions.isEmpty()) {
                            Text(
                                "Bu rol uchun maxsus ruxsatlar belgilanmagan.",
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(role.permissions) { perm ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MexaWarehouseColors.indigoAccent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            perm,
                                            fontSize = 13.sp,
                                            color = MexaWarehouseColors.textPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer ────────────────────────────────────────────────────
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MexaWarehouseColors.indigoAccent,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MexaWarehouseColors.indigoAccent,
                        ),
                    ) {
                        Spacer(Modifier.width(2.dp))
                        Text("Yopish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun RolesEmptyState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.ShieldMoon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFCBD5E1),
        )
        Text(
            "Rollar topilmadi",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MexaWarehouseColors.textPrimary,
        )
        Text(
            "Qidiruv shartlariga mos rol mavjud emas yoki hali belgilanmagan.",
            fontSize = 14.sp,
            color = MexaWarehouseColors.textMuted,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onClearFilters,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.indigoAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.indigoAccent),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Filtrni tozalash", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun roleIconStyle(code: String): Triple<ImageVector, Color, Color> = when (code) {
    "ROLE_SUPER_ADMIN"                         -> Triple(Icons.Default.Star, MexaWarehouseColors.statIndigoBg, MexaWarehouseColors.indigoAccent)
    "ROLE_ADMIN"                               -> Triple(Icons.Default.Shield, MexaWarehouseColors.statIndigoBg, MexaWarehouseColors.indigoAccent)
    "ROLE_WAREHOUSE", "ROLE_WAREHOUSE_MANAGER" -> Triple(Icons.Default.Warehouse, MexaWarehouseColors.roleWarehouseBg, MexaWarehouseColors.roleWarehouseFg)
    "ROLE_COURIER"                             -> Triple(Icons.Default.TwoWheeler, MexaWarehouseColors.roleWarehouseBg, MexaWarehouseColors.roleWarehouseFg)
    "ROLE_USER"                                -> Triple(Icons.Default.Person, MexaWarehouseColors.roleUserBg, MexaWarehouseColors.roleUserFg)
    else                                       -> Triple(Icons.Default.Shield, MexaWarehouseColors.statIndigoBg, MexaWarehouseColors.indigoAccent)
}

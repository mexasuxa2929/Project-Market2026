package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.model.userRoleToBackendName
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.users.common.AuthBadge
import mexa.club.desktop_app.market.ui.users.common.RoleBadge
import mexa.club.desktop_app.market.ui.users.common.StatusBadge
import mexa.club.desktop_app.market.ui.users.common.UserAvatarCircle
import mexa.club.desktop_app.market.users.lastActivityLabel

private val pageSizeOptions = listOf(5, 10, 20, 50)

@Composable
fun UsersTableCard(
    users: List<AdminUser>,
    currentPage: Int,
    totalPages: Int,
    totalCount: Int,
    pageSize: Int,
    isLoading: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onViewUser: (AdminUser) -> Unit,
    onEditUser: (AdminUser) -> Unit,
    onBlockUser: (AdminUser) -> Unit,
    currentUsername: String?,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Products / Couriers / Orders andozasi: Surface Rounded 12dp + border
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
    ) {
        Column {
            UsersTableHeader()
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)

            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.primary, modifier = Modifier.size(36.dp))
                }
                users.isEmpty() -> UsersEmptyState(onClearFilters = onClearFilters, modifier = Modifier.height(280.dp).fillMaxWidth())
                else -> {
                    users.forEachIndexed { index, user ->
                        UserTableRow(
                            user = user,
                            isSelf = user.username == currentUsername,
                            onView = { onViewUser(user) },
                            onEdit = { onEditUser(user) },
                            onBlock = { onBlockUser(user) },
                        )
                        if (index < users.lastIndex) {
                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }
                }
            }

            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            UsersPaginationBar(
                currentPage = currentPage,
                totalPages = totalPages,
                totalCount = totalCount,
                pageSize = pageSize,
                displayedCount = users.size,
                onPageSizeChange = onPageSizeChange,
                onPrev = onPrevPage,
                onNext = onNextPage,
                onGoToPage = onGoToPage,
            )
        }
    }
}

@Composable
private fun UsersTableHeader() {
    val lang = appLanguage()
    Row(
        Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell(AppStrings.usersColUser(lang), Modifier.weight(2f))
        HeaderCell(AppStrings.usersColRole(lang), Modifier.weight(1.5f))
        HeaderCell(AppStrings.usersColStatus(lang), Modifier.weight(1f))
        HeaderCell(AppStrings.usersColVerified(lang), Modifier.weight(1f))
        HeaderCell(AppStrings.usersColLastActivity(lang), Modifier.weight(1f))
        Box(Modifier.width(120.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MexaWarehouseColors.outline,
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun UserTableRow(
    user: AdminUser,
    isSelf: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onBlock: () -> Unit,
) {
    val lang = appLanguage()
    val isSuperAdmin = user.roles.contains(UserRole.SUPER_ADMIN)
    val canEdit = !isSelf
    val canBlock = !isSelf && !isSuperAdmin
    val rowBg = if (user.status == UserStatus.BLOCKED) MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.18f) else Color.Transparent

    Row(
        Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAvatarCircle(user.fullName, size = 36.dp)
            Column {
                val displayName = user.fullName.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                if (!displayName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MexaWarehouseColors.textPrimary)
                        Box(
                            Modifier.size(8.dp).background(if (user.online) Color(0xFF22C55E) else Color(0xFFCBD5E1), CircleShape),
                        )
                    }
                }
                Text("@${user.username}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                AuthBadge(user.authProvider, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val displayCodes = user.rawRoleNames.ifEmpty { user.roles.map { r -> userRoleToBackendName(r) } }
            displayCodes.take(3).forEach { code -> RoleBadge(code) }
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (user.status == UserStatus.ACTIVE) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MexaWarehouseColors.statusActiveFg, modifier = Modifier.size(14.dp))
            }
            StatusBadge(user.status)
        }
        Text(
            if (user.isVerified) AppStrings.verifiedYes(lang) else AppStrings.verifiedPending(lang),
            Modifier.weight(1f),
            fontSize = 13.sp,
            color = MexaWarehouseColors.textPrimary,
        )
        Text(
            lastActivityLabel(user.lastActiveAt, user.online, lang),
            Modifier.weight(1f),
            fontSize = 13.sp,
            color = if (user.online) MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.textMuted,
            fontWeight = if (user.online) FontWeight.SemiBold else FontWeight.Normal,
        )
        Row(Modifier.width(120.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onView, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEdit, enabled = canEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = if (canEdit) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.textCaption, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onBlock, enabled = canBlock, modifier = Modifier.size(32.dp)) {
                if (user.status == UserStatus.BLOCKED) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = if (canBlock) MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.textCaption, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Block, contentDescription = null, tint = if (canBlock) MexaWarehouseColors.danger else MexaWarehouseColors.textCaption, modifier = Modifier.size(16.dp))
                }
            }
            if (isSelf || isSuperAdmin) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MexaWarehouseColors.textCaption, modifier = Modifier.size(12.dp).padding(start = 2.dp, top = 10.dp))
            }
        }
    }
}

@Composable
fun UsersEmptyState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    val lang = appLanguage()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MexaWarehouseColors.outlineVariant)
        Text(AppStrings.usersEmptyTitle(lang), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MexaWarehouseColors.textPrimary, modifier = Modifier.padding(top = 16.dp))
        Text(AppStrings.usersEmptyBody(lang), color = MexaWarehouseColors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.width(360.dp).padding(top = 8.dp))
        OutlinedButton(onClick = onClearFilters, modifier = Modifier.padding(top = 16.dp), shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.primary)
            Text(AppStrings.usersClearFilters(lang), modifier = Modifier.padding(start = 6.dp), color = MexaWarehouseColors.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UsersPaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalCount: Int,
    pageSize: Int,
    displayedCount: Int,
    onPageSizeChange: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoToPage: (Int) -> Unit,
) {
    val lang = appLanguage()
    var sizeMenuOpen by remember { mutableStateOf(false) }
    val from = if (displayedCount == 0) 0 else (currentPage - 1) * pageSize + 1
    val to = if (displayedCount == 0) 0 else from + displayedCount - 1

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(AppStrings.usersShowing(lang, from, to, totalCount), color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("QATORLAR:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
                Box {
                    Surface(
                        onClick = { sizeMenuOpen = true },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                        color = MexaWarehouseColors.surfaceLowest,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(pageSize.toString(), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = MexaWarehouseColors.textMuted)
                        }
                    }
                    DropdownMenu(expanded = sizeMenuOpen, onDismissRequest = { sizeMenuOpen = false }) {
                        pageSizeOptions.forEach { size ->
                            DropdownMenuItem(text = { Text(size.toString(), fontSize = 13.sp) }, onClick = { sizeMenuOpen = false; if (size != pageSize) onPageSizeChange(size) })
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PageNavBtn("⏮", currentPage > 1) { onGoToPage(1) }
                PageNavBtn("‹", currentPage > 1) { onPrev() }
                val half = 2
                val start = (currentPage - 1 - half).coerceAtLeast(0)
                val end = (currentPage - 1 + half).coerceAtMost(totalPages - 1)
                if (start > 0) {
                    PageNumBtn(1, currentPage == 1) { onGoToPage(1) }
                    if (start > 1) EllipsisLabel()
                }
                for (p in start..end) {
                    PageNumBtn(p + 1, p + 1 == currentPage) { onGoToPage(p + 1) }
                }
                if (end < totalPages - 1) {
                    if (end < totalPages - 2) EllipsisLabel()
                    PageNumBtn(totalPages, currentPage == totalPages) { onGoToPage(totalPages) }
                }
                PageNavBtn("›", currentPage < totalPages) { onNext() }
                PageNavBtn("⏭", currentPage < totalPages) { onGoToPage(totalPages) }
            }
        }
    }
}

@Composable
private fun PageNavBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.height(32.dp)) {
        Text(label, fontSize = 14.sp, color = if (enabled) MexaWarehouseColors.textPrimary else MexaWarehouseColors.textCaption)
    }
}

@Composable
private fun PageNumBtn(number: Int, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Surface(shape = RoundedCornerShape(6.dp), color = MexaWarehouseColors.primary) {
            Text(number.toString(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(6.dp)) {
            Text(number.toString(), fontSize = 12.sp, color = MexaWarehouseColors.textPrimary)
        }
    }
}

@Composable
private fun EllipsisLabel() {
    Text("…", fontSize = 14.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(horizontal = 4.dp))
}

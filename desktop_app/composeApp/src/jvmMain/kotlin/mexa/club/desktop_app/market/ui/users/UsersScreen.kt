package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.auth.AdminRoleItem
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.session.MarketSessionGate
import mexa.club.desktop_app.market.users.UsersScreenModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun UsersScreen(modifier: Modifier = Modifier) {
    val lang = appLanguage()
    val vm: UsersScreenModel = koinInject { parametersOf(AppStrings.authNetwork(lang)) }
    val ui by vm.uiState.collectAsState()

    LaunchedEffect(vm) {
        MarketSessionGate.awaitReady()
        vm.loadRoles()
        vm.loadUsers()
    }

    var snackMessage by remember { mutableStateOf<String?>(null) }
    var detailTarget by remember { mutableStateOf<AdminUser?>(null) }
    var blockTarget by remember { mutableStateOf<AdminUser?>(null) }
    var unblockTarget by remember { mutableStateOf<AdminUser?>(null) }
    var editRoleTarget by remember { mutableStateOf<AdminUser?>(null) }
    var editRoleSaving by remember { mutableStateOf(false) }
    var editRoleError by remember { mutableStateOf<String?>(null) }
    var blockSaving by remember { mutableStateOf(false) }
    var blockError by remember { mutableStateOf<String?>(null) }
    var unblockSaving by remember { mutableStateOf(false) }
    var unblockError by remember { mutableStateOf<String?>(null) }

    val currentUsername = AuthSession.username

    editRoleTarget?.let { target ->
        EditRoleDialog(
            user = target,
            availableRoles = ui.assignableRoles,
            onDismiss = {
                editRoleTarget = null
                editRoleError = null
            },
            onSave = { roleId ->
                editRoleSaving = true
                editRoleError = null
                vm.setUserRoleById(
                    userId = target.id,
                    roleId = roleId,
                    onSuccess = {
                        editRoleSaving = false
                        editRoleTarget = null
                        snackMessage = AppStrings.usersRoleUpdatedMessage(lang, target.username)
                    },
                    onError = { err ->
                        editRoleSaving = false
                        editRoleError = err
                    },
                )
            },
            isSaving = editRoleSaving,
            error = editRoleError,
            isCurrentUser = target.username == currentUsername,
        )
    }

    blockTarget?.let { target ->
        BlockUserDialog(
            user = target,
            onDismiss = {
                blockTarget = null
                blockError = null
            },
            onConfirmed = { reason ->
                blockSaving = true
                blockError = null
                vm.blockUser(
                    userId = target.id,
                    reason = reason,
                    onSuccess = {
                        blockSaving = false
                        blockTarget = null
                        snackMessage = AppStrings.usersBlockedMessage(lang, target.username)
                    },
                    onError = { err ->
                        blockSaving = false
                        blockError = err
                    },
                )
            },
            isSaving = blockSaving,
            error = blockError,
            isSelf = target.username == currentUsername,
        )
    }

    unblockTarget?.let { target ->
        UnblockUserDialog(
            user = target,
            onDismiss = {
                unblockTarget = null
                unblockError = null
            },
            onConfirmed = {
                unblockSaving = true
                unblockError = null
                vm.unblockUser(
                    userId = target.id,
                    onSuccess = {
                        unblockSaving = false
                        unblockTarget = null
                        snackMessage = AppStrings.usersUnblockedMessage(lang, target.username)
                    },
                    onError = { err ->
                        unblockSaving = false
                        unblockError = err
                    },
                )
            },
            isSaving = unblockSaving,
            error = unblockError,
        )
    }

    detailTarget?.let { target ->
        UserDetailDialog(user = target, onDismiss = { detailTarget = null })
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header (Warehouses / Orders / Couriers andozasida) ───────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(AppStrings.usersScreenTitle(lang), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text(AppStrings.usersScreenSubtitle(lang), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                }
            }
        }

        // ── Stat cards — endi yuqorida (Warehouses/Orders kabi) ─────────────
        UsersStatCardsRow(
            totalCount = ui.totalCount,
            adminCount = ui.adminCount,
            warehouseCount = ui.warehouseCount,
            activeSessions = ui.activeSessions,
        )

        // ── Filter panel — ProductsScreen andozasida ─────────────────────────
        UsersFilterBar(
            searchQuery = ui.searchQuery,
            onSearchChange = vm::updateSearch,
            roleFilter = ui.roleFilter,
            assignableRoles = ui.assignableRoles,
            onRoleFilterChange = vm::updateRoleFilter,
            statusFilter = ui.statusFilter,
            onStatusFilterChange = vm::updateStatusFilter,
            onClearFilters = vm::clearFilters,
        )

        ui.error?.let { err ->
            Text(err, color = MexaWarehouseColors.error, fontSize = 13.sp)
        }
        snackMessage?.let { msg ->
            Text(msg, color = MexaWarehouseColors.statusActiveFg, fontSize = 13.sp)
        }

        UsersTableCard(
            users = ui.users,
            currentPage = ui.currentPage,
            totalPages = ui.totalPages,
            totalCount = ui.totalCount,
            pageSize = ui.pageSize,
            isLoading = ui.isLoading,
            onPrevPage = { vm.goToPage(ui.currentPage - 1) },
            onNextPage = { vm.goToPage(ui.currentPage + 1) },
            onGoToPage = vm::goToPage,
            onPageSizeChange = vm::setPageSize,
            onViewUser = { user -> detailTarget = user },
            onEditUser = { user ->
                editRoleTarget = user
                editRoleError = null
                editRoleSaving = false
            },
            onBlockUser = { user ->
                when {
                    user.username == currentUsername ->
                        snackMessage = AppStrings.selfBlockWarning(lang)
                    user.roles.contains(UserRole.SUPER_ADMIN) ->
                        snackMessage = AppStrings.superAdminProtectedWarning(lang)
                    user.status == UserStatus.BLOCKED -> unblockTarget = user
                    else -> blockTarget = user
                }
            },
            currentUsername = currentUsername,
            onClearFilters = vm::clearFilters,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    roleFilter: String?,
    assignableRoles: List<AdminRoleItem>,
    onRoleFilterChange: (String?) -> Unit,
    statusFilter: String?,
    onStatusFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    val lang = appLanguage()
    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val knownRoles = listOf(
        "ROLE_SUPER_ADMIN",
        "ROLE_ADMIN",
        "ROLE_WAREHOUSE",
        "ROLE_COURIER",
    )
    val roleOptions: List<Pair<String?, String>> = remember(assignableRoles) {
        val fromServer = assignableRoles
            .distinctBy { it.name }
            .map { it.name to (it.displayName?.takeIf { d -> d.isNotBlank() } ?: it.name) }
        val base = if (fromServer.isNotEmpty()) fromServer
        else knownRoles.map { it to it }
        listOf(null to AppStrings.usersFilterAllRoles(lang)) + base
    }
    val roleLabel = roleFilter?.let { code ->
        roleOptions.firstOrNull { it.first == code }?.second ?: code
    } ?: AppStrings.usersFilterAllRoles(lang)
    val statusOptions = listOf(AppStrings.usersFilterAllStatus(lang), "ACTIVE", "BLOCKED")
    val statusLabel = statusFilter ?: AppStrings.usersFilterAllStatus(lang)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            val compact = maxWidth < 700.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("QIDIRUV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 5.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(AppStrings.usersSearchPlaceholder(lang), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.textMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MexaWarehouseColors.outlineVariant, focusedBorderColor = MexaWarehouseColors.primary),
                            textStyle = TextStyle(fontSize = 14.sp),
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("ROL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 5.dp))
                            ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                                OutlinedTextField(
                                    value = roleLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MexaWarehouseColors.outlineVariant, focusedBorderColor = MexaWarehouseColors.primary),
                                    textStyle = TextStyle(fontSize = 13.sp),
                                )
                                ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                    roleOptions.forEach { (code, label) ->
                                        DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { onRoleFilterChange(code); roleExpanded = false })
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("HOLAT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 5.dp))
                            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                                OutlinedTextField(
                                    value = statusLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MexaWarehouseColors.outlineVariant, focusedBorderColor = MexaWarehouseColors.primary),
                                    textStyle = TextStyle(fontSize = 13.sp),
                                )
                                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                    statusOptions.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt, fontSize = 13.sp) }, onClick = { onStatusFilterChange(opt); statusExpanded = false })
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = onClearFilters,
                            modifier = Modifier.size(48.dp).border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp)).background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp)),
                        ) {
                            Icon(Icons.Default.FilterAltOff, contentDescription = "Filtrni tozalash", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1.6f)) {
                        Text("QIDIRUV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 5.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(AppStrings.usersSearchPlaceholder(lang), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.textMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MexaWarehouseColors.outlineVariant, focusedBorderColor = MexaWarehouseColors.primary),
                            textStyle = TextStyle(fontSize = 14.sp),
                        )
                    }
                    Column(Modifier.width(190.dp)) {
                        Text("ROL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 5.dp))
                        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                            OutlinedTextField(
                                value = roleLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MexaWarehouseColors.outlineVariant, focusedBorderColor = MexaWarehouseColors.primary),
                                textStyle = TextStyle(fontSize = 13.sp),
                            )
                            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                roleOptions.forEach { (code, label) ->
                                    DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { onRoleFilterChange(code); roleExpanded = false })
                                }
                            }
                        }
                    }
                    Column(Modifier.width(170.dp)) {
                        Text("HOLAT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 5.dp))
                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                            OutlinedTextField(
                                value = statusLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MexaWarehouseColors.outlineVariant, focusedBorderColor = MexaWarehouseColors.primary),
                                textStyle = TextStyle(fontSize = 13.sp),
                            )
                            ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                statusOptions.forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt, fontSize = 13.sp) }, onClick = { onStatusFilterChange(opt); statusExpanded = false })
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = onClearFilters,
                        modifier = Modifier.size(48.dp).border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp)).background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp)),
                    ) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = "Filtrni tozalash", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

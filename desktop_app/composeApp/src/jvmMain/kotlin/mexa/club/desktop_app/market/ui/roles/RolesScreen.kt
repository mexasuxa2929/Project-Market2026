package mexa.club.desktop_app.market.ui.roles

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.session.MarketSessionGate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.roles.RolesScreenModel
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun RolesScreen(modifier: Modifier = Modifier) {
    val lang = appLanguage()
    val vm: RolesScreenModel = koinInject { parametersOf(AppStrings.authNetwork(lang)) }
    val ui by vm.uiState.collectAsState()

    // Sessiya tayyor bo'lgach, barcha ma'lumotlarni yuklash (init'da token null bo'lishi mumkin)
    LaunchedEffect(vm) {
        MarketSessionGate.awaitReady()
        vm.loadPermissions()   // ruxsatlar faqat bir marta yuklanadi, shuning uchun shu yerda ham chaqiramiz
        vm.load()
        GatewayRealtimeHub.dashboardRefresh.collect {
            vm.onRealtimeRefreshTick()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var snackMessage     by remember { mutableStateOf<String?>(null) }
    var showEmptyRoles   by remember { mutableStateOf(false) }

    // ── Create dialog ─────────────────────────────────────────────────────────
    if (showCreateDialog) {
        CreateRoleDialog(
            availablePermissions = ui.availablePermissions,
            isSaving             = ui.isCreating,
            error                = ui.createError,
            onDismiss = {
                showCreateDialog = false
                vm.clearCreateError()
            },
            onSave = { form ->
                vm.createRole(
                    form = form,
                    onSuccess = {
                        showCreateDialog = false
                        snackMessage = "«${form.name}» roli muvaffaqiyatli yaratildi"
                    },
                    onError = { /* error ui.createError orqali ko'rinadi */ },
                )
            },
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    ui.editingRole?.let { role ->
        EditRoleDialog(
            role                 = role,
            availablePermissions = ui.availablePermissions,
            isSaving             = ui.isUpdating,
            error                = ui.updateError,
            onDismiss            = { vm.clearEditRole() },
            onSave = { form ->
                vm.updateRole(
                    roleId    = role.id,
                    roleCode  = role.code,
                    form      = form,
                    onSuccess = {
                        snackMessage = "«${role.name}» roli yangilandi"
                    },
                    onError   = { /* error ui.updateError orqali ko'rinadi */ },
                )
            },
        )
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────────
    ui.deletingRole?.let { role ->
        DeleteRoleConfirmDialog(
            role       = role,
            isDeleting = ui.isDeleting,
            error      = ui.deleteError,
            onDismiss  = { vm.clearDeleteRole() },
            onConfirm  = {
                vm.deleteRole(
                    roleId    = role.id,
                    onSuccess = {
                        snackMessage = "«${role.name}» roli o'chirildi"
                    },
                    onError   = { /* error ui.deleteError orqali ko'rinadi */ },
                )
            },
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 700.dp
            if (compact) {
                // Narrow: title on top, actions below
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Rollar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                        Text(
                            "Tizim rollari va foydalanuvchilar bo'yicha taqsimot",
                            fontSize = 14.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .border(1.dp, MexaWarehouseColors.roleAdminBorder, RoundedCornerShape(20.dp))
                                .background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(14.dp))
                            Text("Tizim rollari", fontSize = 11.sp, color = MexaWarehouseColors.indigoAccent)
                        }
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Yangi rol")
                        }
                    }
                }
            } else {
                // Wide: title left, actions right
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Rollar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                        Text(
                            "Tizim rollari va foydalanuvchilar bo'yicha taqsimot",
                            fontSize = 14.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier
                                .border(1.dp, MexaWarehouseColors.roleAdminBorder, RoundedCornerShape(20.dp))
                                .background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(16.dp))
                            Text("Rollar tizim tomonidan belgilangan", fontSize = 12.sp, color = MexaWarehouseColors.indigoAccent)
                        }
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Yangi rol")
                        }
                    }
                }
            }
        }

        ui.error?.let { err ->
            Text(err, color = MexaWarehouseColors.error, fontSize = 13.sp)
        }
        snackMessage?.let { msg ->
            Text(msg, color = MexaWarehouseColors.statusActiveFg, fontSize = 13.sp)
        }

        if (ui.isLoading && ui.roles.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
        } else {
            RolesStatCardsRow(stats = ui.stats)
            RoleCardsGrid(
                roles          = ui.roles,
                showEmpty      = showEmptyRoles,
                onClearFilters = { showEmptyRoles = false },
                onEditRole     = { vm.startEditRole(it) },
                onDeleteRole   = { vm.startDeleteRole(it) },
            )
            RoleUsersTableSection(
                users          = ui.filteredUsers,
                roles          = ui.roles,
                tabFilter      = ui.tabFilter,
                onTabChange    = vm::setTab,
                tabCount       = vm::tabCount,
                onReassignRole = { snackMessage = "Rolni o'zgartirish: ${it.fullName} (tez orada)" },
            )
        }
    }
}

package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mexa.club.desktop_app.auth.AdminRoleItem
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.users.common.UserAvatarCircle
import mexa.club.desktop_app.localization.appLanguage

// ─── Dialog ───────────────────────────────────────────────────────────────────

/**
 * Rol tahrirlash dialogi.
 *
 * @param availableRoles  Backenddan kelgan rollar ro'yxati.
 *                        Bo'sh bo'lsa yuklanish indikatori ko'rsatiladi.
 * @param onSave          Tanlangan rolning UUID (id) si bilan chaqiriladi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoleDialog(
    user: AdminUser,
    availableRoles: List<AdminRoleItem>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,         // role UUID (id)
    isSaving: Boolean = false,
    error: String? = null,
    isCurrentUser: Boolean = false,
) {
    val lang = appLanguage()
    // ── Initial selection: prefer rawRoleNames (handles custom roles like ROLE_MANAGER)
    val initialRole: AdminRoleItem? = remember(user.id, availableRoles) {
        val rawCode = user.rawRoleNames.firstOrNull()
            ?: user.roles.firstOrNull()?.let { roleToCode(it) }
        availableRoles.firstOrNull { it.name == rawCode } ?: availableRoles.firstOrNull()
    }

    var selectedRole by remember(user.id, availableRoles) { mutableStateOf(initialRole) }
    var expanded by remember { mutableStateOf(false) }

    // O'z-o'zini himoya qilish: joriy foydalanuvchi SUPER_ADMIN bo'lsa va yangi rol SUPER_ADMIN emas
    val isSelfSuperAdmin = isCurrentUser && user.rawRoleNames.contains("ROLE_SUPER_ADMIN")
    val selfDemote = isSelfSuperAdmin && selectedRole != null && selectedRole!!.name != "ROLE_SUPER_ADMIN"

    // ── Biriktirma tekshiruvi ──────────────────────────────────────────
    // Rol almashtirishdan oldin: user qaysidir rolda qayerdadir biriktirilganmi?
    val currentCode = remember(user.id) {
        user.rawRoleNames.firstOrNull() ?: user.roles.firstOrNull()?.let { roleToCode(it) }
    }
    var bindingsLoading by remember(user.id) { mutableStateOf(true) }
    var bindingsFailed by remember(user.id) { mutableStateOf(false) }
    var courierName by remember(user.id) { mutableStateOf<String?>(null) }
    var courierWarehouse by remember(user.id) { mutableStateOf<String?>(null) }
    var courierActiveDeliveries by remember(user.id) { mutableStateOf(0) }
    var adminWarehouses by remember(user.id) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(user.id) {
        bindingsLoading = true
        bindingsFailed = false
        runCatching {
            withContext(Dispatchers.IO) {
                // 1) Kuryer biriktirmasi
                val courierData = runCatching {
                    val text = ApiClient.get("/api/admin/couriers/by-user?userId=${user.id}")
                    ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) } as? JsonObject
                }.getOrNull()
                if (courierData != null) {
                    courierName = courierData.stringField("name").ifEmpty { null }
                    val activeCount = courierData.stringField("currentDeliveries").toIntOrNull() ?: 0
                    courierActiveDeliveries = activeCount
                    val warehouseId = courierData.stringField("warehouseId")
                    courierWarehouse = if (warehouseId.isNotBlank()) {
                        // Ombor nomini aniqlaymiz (topilmasa ID qisqartmasi)
                        runCatching {
                            val whText = ApiClient.get("/api/warehouses?page=0&size=100")
                            val whData = ApiClient.parseJsonObject(whText)?.let { ApiClient.dataObject(it) }
                            val items = (whData?.get("items") as? JsonArray)
                                ?: (whData?.get("content") as? JsonArray)
                                ?: JsonArray(emptyList())
                            items.mapNotNull { it as? JsonObject }
                                .firstOrNull { it.stringField("id") == warehouseId }
                                ?.stringField("name")?.ifEmpty { null }
                        }.getOrNull() ?: warehouseId.take(8)
                    } else null
                }
                // 2) Admin biriktirmalari (omborlar)
                val adminText = runCatching {
                    ApiClient.get("/api/warehouses/by-admin?userId=${user.id}")
                }.getOrNull()
                val adminArr = adminText?.let { ApiClient.parseJsonObject(it)?.get("data") as? JsonArray }
                    ?: JsonArray(emptyList())
                adminWarehouses = adminArr.mapNotNull { el ->
                    (el as? JsonObject)?.stringField("name")?.ifEmpty { null }
                }
            }
        }.onFailure { bindingsFailed = true }
        bindingsLoading = false
    }

    val roleChanged = selectedRole != null && selectedRole!!.name != currentCode
    val courierRisk = roleChanged && currentCode == "ROLE_COURIER" && courierName != null
    val adminRisk = roleChanged && adminWarehouses.isNotEmpty()
    val hasRisk = courierRisk || adminRisk
    // Rol tanlovi o'zgarsa tasdiq qayta so'raladi
    var acknowledged by remember(user.id, selectedRole) { mutableStateOf(false) }

    // Backenddan kelgan real permission kodlari ko'rsatiladi (masalan "USER_VIEW").
    // Local fake catalog fallback ishlatilmaydi — rolga ruxsat bo'lmasa bo'sh blok ko'rsatiladi.
    val permissions: List<String> = selectedRole?.permissions ?: emptyList()

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 340.dp, max = 520.dp).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(12.dp),
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
                    Text(
                        AppStrings.editRoleDialogTitle(lang),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Yopish",
                            tint = MexaWarehouseColors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

                    // ── User info card ────────────────────────────────────────
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F3FF), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        UserAvatarCircle(user.fullName, size = 40.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                user.fullName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MexaWarehouseColors.textPrimary,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    AppStrings.editRoleCurrentRole(lang),
                                    fontSize = 12.sp,
                                    color = MexaWarehouseColors.textMuted,
                                )
                                // Current role — rawRoleNames orqali aniq kod olinadi (custom rollar uchun ham)
                                val currentDisplay = run {
                                    val rawCode = user.rawRoleNames.firstOrNull()
                                        ?: user.roles.firstOrNull()?.let { roleToCode(it) }
                                    rawCode?.let { code ->
                                        availableRoles.firstOrNull { it.name == code }
                                            ?.displayName?.takeIf { it.isNotBlank() }
                                            ?: code
                                    } ?: "—"
                                }
                                Text(
                                    currentDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MexaWarehouseColors.indigoAccent,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Role dropdown ─────────────────────────────────────────
                    Text(
                        AppStrings.editRoleNewRole(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MexaWarehouseColors.textPrimary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    if (availableRoles.isEmpty()) {
                        // Loading
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MexaWarehouseColors.indigoAccent,
                            )
                            Text(
                                AppStrings.editRoleLoading(lang),
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { if (!isSaving) expanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedRole?.let {
                                    it.displayName?.takeIf { n -> n.isNotBlank() }
                                        ?: it.name
                                } ?: AppStrings.editRoleChoosePlaceholder(lang),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MexaWarehouseColors.borderSubtle,
                                    focusedBorderColor = MexaWarehouseColors.indigoAccent,
                                    unfocusedContainerColor = MexaWarehouseColors.inputBg,
                                    focusedContainerColor = MexaWarehouseColors.inputBg,
                                    unfocusedTextColor = MexaWarehouseColors.textPrimary,
                                    focusedTextColor = MexaWarehouseColors.textPrimary,
                                ),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                availableRoles.forEach { roleItem ->
                                    val label = roleItem.displayName
                                        ?.takeIf { it.isNotBlank() }
                                        ?: roleItem.name
                                    DropdownMenuItem(
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    label,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MexaWarehouseColors.textPrimary,
                                                )
                                                val desc = roleItem.description
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?: ""
                                                if (desc.isNotBlank()) {
                                                    Text(
                                                        desc,
                                                        fontSize = 11.sp,
                                                        color = MexaWarehouseColors.textMuted,
                                                        lineHeight = 15.sp,
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedRole = roleItem
                                            expanded = false
                                        },
                                        modifier = if (selectedRole?.name == roleItem.name)
                                            Modifier.background(Color(0xFFF5F3FF))
                                        else
                                            Modifier,
                                    )
                                }
                            }
                        }
                    }

                    // ── Permissions block ─────────────────────────────────────
                    if (selectedRole != null) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            AppStrings.editRolePermissionsTitle(lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MexaWarehouseColors.textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (permissions.size > 5)
                                        Modifier.height(200.dp)
                                    else
                                        Modifier.wrapContentHeight()
                                )
                                .background(Color(0xFFF5F3FF), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE0D9FF), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                        ) {
                            if (permissions.isEmpty()) {
                                Text(
                                    AppStrings.editRolePermissionsEmpty(lang),
                                    fontSize = 13.sp,
                                    color = MexaWarehouseColors.textMuted,
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(permissions) { perm ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MexaWarehouseColors.indigoAccent,
                                                modifier = Modifier.size(17.dp),
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

                    // ── Biriktirma ogohlantirishi ─────────────────────────
                    if (bindingsLoading) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MexaWarehouseColors.textMuted,
                            )
                            Text(
                                "Biriktirmalar tekshirilmoqda...",
                                fontSize = 12.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        }
                    } else if (bindingsFailed) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Biriktirmalarni tekshirib bo'lmadi — rolni ehtiyotkorlik bilan almashtiring",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    } else if (hasRisk) {
                        Spacer(Modifier.height(12.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "⚠ Diqqat: biriktirma topildi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                            )
                            if (courierRisk) {
                                Text(
                                    "Bu foydalanuvchi “${courierName}” sifatida " +
                                        (if (courierWarehouse != null) "“$courierWarehouse” omboriga kuryer qilib biriktirilgan" else "kuryer qilingan") + ". " +
                                        if (courierActiveDeliveries > 0) "Hozir ${courierActiveDeliveries} ta faol buyurtmasi bor — avval ularni Kuryerlar bo'limidan boshqa kuryerga o'tkazing yoki kuryerdan oling. " else "",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 17.sp,
                                )
                            }
                            if (adminRisk) {
                                Text(
                                    "Bu foydalanuvchi admin sifatida omborga biriktirilgan: ${adminWarehouses.joinToString(", ")}. " +
                                        "Rol almashtirilsa, ombor nazoratsiz qolishi mumkin — avval Warehouses bo'limidan adminlikdan bo'shating.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 17.sp,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = acknowledged,
                                    onCheckedChange = { if (!isSaving) acknowledged = it },
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = MexaWarehouseColors.indigoAccent,
                                    ),
                                )
                                Text(
                                    "Tushundim, baribir almashtirish",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E),
                                )
                            }
                        }
                    }

                    // ── Self-demote warning ─────────────────────────────────
                    if (selfDemote) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            AppStrings.editRoleSelfDemoteError(lang),
                            color = MexaWarehouseColors.error,
                            fontSize = 12.sp,
                        )
                    }

                    // ── Error message ─────────────────────────────────────────
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = MexaWarehouseColors.error,
                            fontSize = 12.sp,
                        )
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer actions ────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (!isSaving) onDismiss() },
                        enabled = !isSaving,
                    ) {
                        Text(AppStrings.cancel(lang))
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val role = selectedRole ?: return@Button
                            if (selfDemote) return@Button
                            if (hasRisk && !acknowledged) return@Button
                            onSave(role.id)   // UUID ni to'g'ridan-to'g'ri uzatamiz
                        },
                        enabled = !isSaving && selectedRole != null && availableRoles.isNotEmpty() && !selfDemote && (!hasRisk || acknowledged),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MexaWarehouseColors.indigoAccent,
                        ),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(AppStrings.editRoleSave(lang), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun roleToCode(role: UserRole): String = when (role) {
    UserRole.SUPER_ADMIN -> "ROLE_SUPER_ADMIN"
    UserRole.ADMIN       -> "ROLE_ADMIN"
    UserRole.WAREHOUSE   -> "ROLE_WAREHOUSE"
    UserRole.COURIER     -> "ROLE_COURIER"
    UserRole.USER        -> "ROLE_USER"
}

package mexa.club.desktop_app.market.ui.roles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mexa.club.desktop_app.auth.PermissionGroup
import mexa.club.desktop_app.market.model.RoleDefinition
import mexa.club.desktop_app.market.model.UpdateRoleForm
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun EditRoleDialog(
    role: RoleDefinition,
    availablePermissions: List<PermissionGroup> = emptyList(),
    isSaving: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (UpdateRoleForm) -> Unit,
) {
    // Pre-fill from existing role
    var name        by remember { mutableStateOf(role.name) }
    var description by remember { mutableStateOf(role.description) }
    // Pre-select existing permissions
    val selectedPerms    = remember {
        mutableStateMapOf<String, Boolean>().also { map ->
            role.permissions.forEach { map[it] = true }
        }
    }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    availablePermissions.forEach { group ->
        if (!expandedSections.containsKey(group.category)) {
            expandedSections[group.category] = false
        }
    }
    val selectedCount = selectedPerms.values.count { it }
    val isReadOnly    = role.isSystemRole
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 360.dp, max = 600.dp).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(14.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.heightIn(max = 680.dp)) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    "Rolni tahrirlash",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MexaWarehouseColors.textPrimary,
                                )
                                if (isReadOnly) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Tizim roli",
                                        tint = MexaWarehouseColors.textMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Text(
                                role.code,
                                fontSize = 12.sp,
                                color = MexaWarehouseColors.textCaption,
                            )
                        }
                    }
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
                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                // ── System role read-only banner ───────────────────────────────
                if (isReadOnly) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MexaWarehouseColors.infoBadgeBg)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MexaWarehouseColors.indigoAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "Bu tizim roli. Uning nomi, kodi va ruxsatlari o'zgartirish uchun mavjud emas.",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.indigoAccent,
                            lineHeight = 18.sp,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                }
                // ── Scrollable body ───────────────────────────────────────────
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // ── Display name ──────────────────────────────────────────
                    EditFormField(
                        label       = "ROL NOMI",
                        placeholder = "Ko'rsatiladigan nom",
                        value       = name,
                        onChange    = { name = it },
                        enabled     = !isSaving && !isReadOnly,
                    )
                    // ── Role code (read-only always) ──────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "ROL KODI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textMuted,
                            letterSpacing = 0.5.sp,
                        )
                        OutlinedTextField(
                            value = role.code,
                            onValueChange = {},
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = editFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                        )
                        Text(
                            "Rol kodi o'zgartirish mumkin emas",
                            fontSize = 11.sp,
                            color = MexaWarehouseColors.textCaption,
                        )
                    }
                    // ── Ruxsatlar ─────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "RUXSATLAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.textMuted,
                                letterSpacing = 0.5.sp,
                            )
                            if (selectedCount > 0) {
                                Box(
                                    Modifier
                                        .background(MexaWarehouseColors.indigoAccent, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        "$selectedCount tanlangan",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        if (availablePermissions.isEmpty()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                                    .background(MexaWarehouseColors.inputBg, RoundedCornerShape(8.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MexaWarehouseColors.indigoAccent,
                                )
                                Text(
                                    "Ruxsatlar yuklanmoqda…",
                                    fontSize = 13.sp,
                                    color = MexaWarehouseColors.textMuted,
                                )
                            }
                        } else {
                            availablePermissions.forEach { group ->
                                val expanded    = expandedSections[group.category] == true
                                val groupChecked = group.permissions.count { selectedPerms[it.name] == true }
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                                        .background(MexaWarehouseColors.inputBg, RoundedCornerShape(8.dp)),
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isReadOnly) {
                                                expandedSections[group.category] = !expanded
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                group.category,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MexaWarehouseColors.textPrimary,
                                            )
                                            if (groupChecked > 0) {
                                                Text(
                                                    "$groupChecked/${group.permissions.size}",
                                                    fontSize = 11.sp,
                                                    color = MexaWarehouseColors.indigoAccent,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            if (!isReadOnly) {
                                                val allChecked =
                                                    group.permissions.all { selectedPerms[it.name] == true }
                                                Text(
                                                    if (allChecked) "Hammasini olib tashlash" else "Hammasini tanlash",
                                                    fontSize = 10.sp,
                                                    color = MexaWarehouseColors.indigoAccent,
                                                    modifier = Modifier.clickable(enabled = !isSaving) {
                                                        group.permissions.forEach { p ->
                                                            selectedPerms[p.name] = !allChecked
                                                        }
                                                    },
                                                )
                                            }
                                            Icon(
                                                if (expanded) Icons.Default.KeyboardArrowUp
                                                else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MexaWarehouseColors.textMuted,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(),
                                    ) {
                                        Column(
                                            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            group.permissions.forEach { perm ->
                                                val checked = selectedPerms[perm.name] == true
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable(enabled = !isSaving && !isReadOnly) {
                                                            selectedPerms[perm.name] = !checked
                                                        }
                                                        .padding(vertical = 3.dp),
                                                ) {
                                                    Checkbox(
                                                        checked = checked,
                                                        onCheckedChange = {
                                                            if (!isSaving && !isReadOnly)
                                                                selectedPerms[perm.name] = it
                                                        },
                                                        enabled = !isReadOnly,
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = MexaWarehouseColors.indigoAccent,
                                                        ),
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            perm.displayName
                                                                .replace("_", " ")
                                                                .split(" ")
                                                                .joinToString(" ") {
                                                                    it.replaceFirstChar { c -> c.uppercaseChar() }
                                                                },
                                                            fontSize = 13.sp,
                                                            color = MexaWarehouseColors.textPrimary,
                                                        )
                                                        Text(
                                                            perm.name,
                                                            fontSize = 10.sp,
                                                            color = MexaWarehouseColors.textCaption,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // ── Tavsif ────────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "TAVSIF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textMuted,
                            letterSpacing = 0.5.sp,
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            enabled = !isSaving && !isReadOnly,
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            placeholder = {
                                Text(
                                    "Bu rol haqida qisqacha ma'lumot…",
                                    color = MexaWarehouseColors.textCaption,
                                )
                            },
                            colors = editFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                // ── Error ─────────────────────────────────────────────────────
                error?.let {
                    Text(
                        it,
                        color = MexaWarehouseColors.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                // ── Footer ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { if (!isSaving) onDismiss() },
                            enabled = !isSaving,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MexaWarehouseColors.textMuted,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MexaWarehouseColors.borderSubtle,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Bekor qilish")
                        }
                        Button(
                            onClick = {
                                onSave(
                                    UpdateRoleForm(
                                        name        = name.trim(),
                                        description = description.trim(),
                                        permissions = selectedPerms
                                            .filter { it.value }
                                            .keys
                                            .toSet(),
                                    ),
                                )
                            },
                            enabled = !isSaving && !isReadOnly && name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MexaWarehouseColors.indigoAccent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (isSaving) "Saqlanmoqda…" else "Saqlash",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFormField(
    label: String,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MexaWarehouseColors.textMuted,
            letterSpacing = 0.5.sp,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MexaWarehouseColors.textCaption) },
            singleLine = true,
            colors = editFieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MexaWarehouseColors.indigoAccent,
    unfocusedBorderColor    = MexaWarehouseColors.borderSubtle,
    focusedContainerColor   = MexaWarehouseColors.inputBg,
    unfocusedContainerColor = MexaWarehouseColors.inputBg,
    disabledContainerColor  = MexaWarehouseColors.inputBg,
    disabledBorderColor     = MexaWarehouseColors.borderSubtle,
    focusedTextColor        = MexaWarehouseColors.textPrimary,
    unfocusedTextColor      = MexaWarehouseColors.textPrimary,
)

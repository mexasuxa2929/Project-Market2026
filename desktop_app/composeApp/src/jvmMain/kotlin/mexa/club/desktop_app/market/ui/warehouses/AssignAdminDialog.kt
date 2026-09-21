package mexa.club.desktop_app.market.ui.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class AdminUser(
    val id: String,
    val username: String,
    val email: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignAdminDialog(
    warehouseId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val allAdminUsers = remember { mutableStateListOf<AdminUser>() }
    val assignedIds = remember { mutableStateListOf<String>() }
    val currentIds = remember { mutableStateListOf<String>() }
    val selectedIds = remember { mutableStateListOf<String>() }

    var addDropdownExpanded by remember { mutableStateOf(false) }
    var pendingRemove by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val adminsJson = withContext(Dispatchers.IO) {
                ApiClient.get("/api/admin/users/by-role?role=ROLE_ADMIN")
            }
            val parsed = ApiClient.parseJsonObject(adminsJson)
            val dataArr = parsed?.let { root ->
                (root["data"] as? JsonArray) ?: (root["items"] as? JsonArray)
            } ?: JsonArray(emptyList())
            allAdminUsers.clear()
            for (el in dataArr) {
                val o = el as? JsonObject ?: continue
                allAdminUsers.add(AdminUser(
                    id = o.stringField("id"),
                    username = o.stringField("username"),
                    email = o.stringField("email"),
                ))
            }

            val assignedJson = withContext(Dispatchers.IO) {
                ApiClient.get("/api/warehouses/admins/assigned")
            }
            val assignedParsed = ApiClient.parseJsonObject(assignedJson)
            assignedIds.clear()
            assignedIds.addAll(
                (assignedParsed?.get("data") as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    ?: emptyList()
            )

            val currentJson = withContext(Dispatchers.IO) {
                ApiClient.get("/api/warehouses/$warehouseId/admins")
            }
            val currentParsed = ApiClient.parseJsonObject(currentJson)
            val warehouseAdminIds = (currentParsed?.get("data") as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?: emptyList()

            currentIds.clear()
            currentIds.addAll(warehouseAdminIds)
            selectedIds.clear()
            selectedIds.addAll(warehouseAdminIds)
        } catch (e: Exception) {
            error = e.message ?: "Xatolik"
        }
        loading = false
    }

    val availableForAdd = allAdminUsers.filter { u ->
        !selectedIds.contains(u.id) && (!assignedIds.contains(u.id) || currentIds.contains(u.id))
    }

    fun save() {
        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{\"adminUserIds\":[")
                append(selectedIds.joinToString(",") { it.jsonString() })
                append("]}")
            }
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.put("/api/warehouses/$warehouseId/admins", json)
                }
                saving = false
                onSaved()
            } catch (e: Exception) {
                error = e.message ?: "Xatolik yuz berdi"
                saving = false
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 380.dp, max = 560.dp).fillMaxWidth(0.9f),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Admin tayinlash",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Hozirgi adminlar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)

                        if (selectedIds.isEmpty()) {
                            Text(
                                "Admin tayinlanmagan",
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textMuted,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            selectedIds.forEach { uid ->
                                val user = allAdminUsers.find { it.id == uid }
                                Row(
                                    Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Default.Person, null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(user?.username ?: "ID: ${uid.take(8)}...", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                            if (user != null) {
                                                Text(user.email, fontSize = 11.sp, color = MexaWarehouseColors.textCaption)
                                            }
                                        }
                                    }
                                    IconButton(onClick = { if (!saving) pendingRemove = uid }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        if (availableForAdd.isNotEmpty()) {
                            Text("Admin qo'shish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = addDropdownExpanded,
                                    onExpandedChange = { addDropdownExpanded = it },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    OutlinedTextField(
                                        value = "",
                                        onValueChange = {},
                                        readOnly = true,
                                        placeholder = { Text("Admin tanlang...", color = MexaWarehouseColors.textCaption) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = addDropdownExpanded,
                                        onDismissRequest = { addDropdownExpanded = false },
                                    ) {
                                        availableForAdd.forEach { user ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(user.username, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        Text(user.email, fontSize = 11.sp, color = MexaWarehouseColors.textCaption)
                                                    }
                                                },
                                                onClick = {
                                                    selectedIds.add(user.id)
                                                    addDropdownExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Barcha admin foydalanuvchilar allaqachon biriktirilgan",
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        }

                        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                    }

                    error?.let { err ->
                        Text(err, color = MexaWarehouseColors.danger, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(
                            onClick = { if (!saving) onDismiss() },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Bekor qilish")
                        }
                        Button(
                            onClick = { save() },
                            enabled = !saving && !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (saving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Saqlash", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    val pendingUser = pendingRemove?.let { uid -> allAdminUsers.find { it.id == uid } }
    if (pendingRemove != null) {
        Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            Surface(
                modifier = Modifier.widthIn(min = 300.dp, max = 440.dp).fillMaxWidth(0.85f),
                shape = RoundedCornerShape(14.dp),
                color = MexaWarehouseColors.surfaceLowest,
                tonalElevation = 8.dp,
            ) {
                Column {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(52.dp).background(MexaWarehouseColors.statusBlockedBg, RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Close, null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(28.dp))
                        }
                        Text(
                            "Adminni olib tashlash",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Haqiqatan ham \"${pendingUser?.username ?: "bu foydalanuvchi"}\"ni ushbu ombor adminlaridan olib tashlamoqchimisiz?",
                            fontSize = 14.sp,
                            color = MexaWarehouseColors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { pendingRemove = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textMuted),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Bekor qilish")
                        }
                        Button(
                            onClick = {
                                pendingRemove?.let { selectedIds.remove(it) }
                                pendingRemove = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Olib tashlash", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

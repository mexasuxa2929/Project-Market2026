package mexa.club.desktop_app.market.ui.couriers

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.warehouses.DropdownField

private data class SelectableUser(val id: String, val label: String)
private data class SelectableZone(val id: String, val name: String, val warehouseId: String, val warehouseName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierFormDialog(
    existing: CourierRow?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isEdit = existing != null

    var users by remember { mutableStateOf<List<SelectableUser>>(emptyList()) }
    var usersLoading by remember { mutableStateOf(true) }
    var selectedUser by remember { mutableStateOf<SelectableUser?>(null) }
    var userExpanded by remember { mutableStateOf(false) }

    var zones by remember { mutableStateOf<List<SelectableZone>>(emptyList()) }
    var zonesLoading by remember { mutableStateOf(true) }
    var selectedZoneId by remember { mutableStateOf("") }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var region by remember { mutableStateOf(existing?.region ?: "") }
    var active by remember { mutableStateOf(existing?.active ?: true) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun fetchUsers(): List<SelectableUser> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/users?page=0&size=200")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
        val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        val all = items.mapNotNull { el ->
            val u = el as? JsonObject ?: return@mapNotNull null
            val id = u.stringField("id").ifEmpty { return@mapNotNull null }
            val roles = (u["roles"] as? JsonArray)?.joinToString(",") { it.toString() } ?: ""
            val username = u.stringField("username")
            val email = u.stringField("email")
            Triple(id, "$username ($email)", roles.contains("COURIER", ignoreCase = true))
        }
        val courierUsers = all.filter { it.third }.map { SelectableUser(it.first, it.second) }
        if (courierUsers.isNotEmpty()) courierUsers else all.map { SelectableUser(it.first, it.second) }
    }

    suspend fun fetchZones(): List<SelectableZone> = withContext(Dispatchers.IO) {
        val whText = ApiClient.get("/api/warehouses?page=0&size=100")
        val whParsed = ApiClient.parseJsonObject(whText)
        val whItems = whParsed?.let { ApiClient.dataObject(it) }?.itemsOrContentArray() ?: JsonArray(emptyList())
        val whMap = whItems.mapNotNull { el ->
            val w = el as? JsonObject ?: return@mapNotNull null
            val id = w.stringField("id").ifEmpty { return@mapNotNull null }
            val whName = w.stringField("name").ifEmpty { return@mapNotNull null }
            id to whName
        }.toMap()

        val text = ApiClient.get("/api/admin/geo-zones")
        val parsed = ApiClient.parseJsonObject(text)
        val items = parsed?.let { it["data"] as? JsonArray } ?: JsonArray(emptyList())
        items.mapNotNull { el ->
            val z = el as? JsonObject ?: return@mapNotNull null
            val warehouseId = z.stringField("warehouseId")
            if (warehouseId.isBlank()) return@mapNotNull null
            val warehouseName = whMap[warehouseId] ?: return@mapNotNull null
            SelectableZone(id = warehouseId, name = warehouseName, warehouseId = warehouseId, warehouseName = warehouseName)
        }.distinctBy { it.warehouseId }
    }

    LaunchedEffect(Unit) {
        usersLoading = true
        zonesLoading = true
        runCatching { fetchUsers() }
            .onSuccess { list ->
                users = list
                if (isEdit) {
                    selectedUser = list.find { it.id == existing?.userId }
                        ?: existing?.userId?.let { SelectableUser(it, it.take(8)) }
                }
            }
        usersLoading = false
        runCatching { fetchZones() }
            .onSuccess { list ->
                zones = list
                if (isEdit && existing?.region != null) {
                    selectedZoneId = list.find { it.warehouseName == existing.region || it.name == existing.region }?.id ?: ""
                }
            }
        zonesLoading = false
    }

    fun save() {
        val uid = selectedUser?.id ?: existing?.userId
        if (uid.isNullOrBlank()) { error = "Kuryer uchun foydalanuvchini tanlang"; return }
        val regionValue: String
        if (zones.isNotEmpty()) {
            regionValue = zones.find { it.id == selectedZoneId }?.warehouseName ?: ""
            if (regionValue.isBlank()) { error = "Hududni tanlang"; return }
        } else {
            regionValue = region
        }
        if (name.isBlank() || phone.isBlank() || regionValue.isBlank()) { error = "Barcha maydonlarni to'ldiring"; return }
        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{")
                append("\"userId\":\"$uid\",")
                append("\"name\":\"${name.trim().replace("\"", "\\\"")}\",")
                append("\"phone\":\"${phone.trim().replace("\"", "\\\"")}\",")
                append("\"region\":\"${regionValue.trim().replace("\"", "\\\"")}\",")
                append("\"active\":$active")
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (isEdit) ApiClient.put("/api/admin/couriers/${existing!!.id}", json)
                    else ApiClient.post("/api/admin/couriers", json)
                }
            }
            result.onSuccess { onSaved() }
                .onFailure { e -> error = e.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 340.dp, max = 480.dp).fillMaxWidth(0.88f),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isEdit) "Kuryerni tahrirlash" else "Yangi kuryer qo'shish", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("Foydalanuvchi (kuryer akkaunti)")
                    if (usersLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        ExposedDropdownMenuBox(expanded = userExpanded, onExpandedChange = { if (!isEdit) userExpanded = it }) {
                            OutlinedTextField(
                                value = selectedUser?.label ?: "Tanlang",
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isEdit,
                                trailingIcon = { if (!isEdit) ExposedDropdownMenuDefaults.TrailingIcon(expanded = userExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            )
                            if (!isEdit) {
                                ExposedDropdownMenu(expanded = userExpanded, onDismissRequest = { userExpanded = false }) {
                                    users.forEach { u ->
                                        DropdownMenuItem(text = { Text(u.label) }, onClick = { selectedUser = u; userExpanded = false })
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("Ism")
                    OutlinedTextField(value = name, onValueChange = { name = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("Telefon")
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it; error = null },
                        placeholder = { Text("+998901234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("Hudud")
                    if (zonesLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else if (zones.isEmpty()) {
                        OutlinedTextField(value = region, onValueChange = { region = it; error = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp), placeholder = { Text("Hudud nomini kiriting") })
                    } else {
                        DropdownField(
                            value = selectedZoneId,
                            placeholder = "Hududni tanlang",
                            options = zones.map { it.id to it.warehouseName },
                            onSelect = { id -> selectedZoneId = id; region = zones.find { z -> z.id == id }?.warehouseName ?: "" },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FormLabel("Faol")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Bekor qilish") }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
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

@Composable
private fun FormLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
}

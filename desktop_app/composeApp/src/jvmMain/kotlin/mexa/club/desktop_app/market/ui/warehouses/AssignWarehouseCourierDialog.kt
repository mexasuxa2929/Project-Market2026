package mexa.club.desktop_app.market.ui.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class WarehouseCourier(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val active: Boolean,
    val currentDeliveries: Int,
)

private data class UnassignedCourier(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignWarehouseCourierDialog(
    warehouseId: String,
    warehouseName: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Shu omborga biriktirilganlar (warehouseId == shu ombor).
    val attached = remember { mutableStateListOf<WarehouseCourier>() }
    // Hali hech qayerda bo'lmagan courier qatorlari.
    val unassigned = remember { mutableStateListOf<UnassignedCourier>() }

    var attachExpanded by remember { mutableStateOf(false) }
    var selectedUnassigned by remember { mutableStateOf<UnassignedCourier?>(null) }
    var attachSearch by remember { mutableStateOf("") }

    var pendingRemove by remember { mutableStateOf<WarehouseCourier?>(null) }

    suspend fun loadAll() {
        // 1) Shu ombor kuryerlari
        val attachedText = withContext(Dispatchers.IO) {
            ApiClient.get("/api/admin/couriers/by-warehouse?warehouseId=$warehouseId")
        }
        val attachedArr = (ApiClient.parseJsonObject(attachedText)?.get("data") as? JsonArray)
            ?: JsonArray(emptyList())
        attached.clear()
        attached.addAll(attachedArr.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val id = c.stringField("id").ifEmpty { return@mapNotNull null }
            WarehouseCourier(
                id = id,
                userId = c.stringField("userId"),
                name = c.stringField("name"),
                phone = c.stringField("phone"),
                active = c.stringField("active").toBooleanStrictOrNull() ?: true,
                currentDeliveries = c.stringField("currentDeliveries").toIntOrNull() ?: 0,
            )
        })

        // 2) Biriktirilmaganlar
        val unassignedText = withContext(Dispatchers.IO) {
            ApiClient.get("/api/admin/couriers/unassigned")
        }
        val unassignedArr = (ApiClient.parseJsonObject(unassignedText)?.get("data") as? JsonArray)
            ?: JsonArray(emptyList())
        unassigned.clear()
        unassigned.addAll(unassignedArr.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val id = c.stringField("id").ifEmpty { return@mapNotNull null }
            UnassignedCourier(
                id = id,
                userId = c.stringField("userId"),
                name = c.stringField("name"),
                phone = c.stringField("phone"),
            )
        })
    }

    LaunchedEffect(Unit) {
        runCatching { loadAll() }.onFailure { error = it.message ?: "Xatolik" }
        loading = false
    }

    fun reload() {
        scope.launch {
            loading = true
            runCatching { loadAll() }.onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    fun attachExisting() {
        val target = selectedUnassigned ?: return
        scope.launch {
            saving = true; error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.put("/api/admin/couriers/${target.id}/warehouse", "{\"warehouseId\":\"$warehouseId\"}")
                }
            }.onSuccess {
                selectedUnassigned = null
                reload()
                onSaved()
            }.onFailure { error = it.message ?: "Xatolik" }
            saving = false
        }
    }

    fun detach(target: WarehouseCourier) {
        scope.launch {
            saving = true; error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.delete("/api/admin/couriers/${target.id}/warehouse")
                }
            }.onSuccess {
                pendingRemove = null
                reload()
                onSaved()
            }.onFailure { error = it.message ?: "Xatolik" }
            saving = false
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
            modifier = Modifier.widthIn(min = 480.dp, max = 700.dp).fillMaxWidth(0.94f).heightIn(max = 680.dp),
        ) {
            Column(
                Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Kuryer biriktirish", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                        Text(warehouseName, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }
                } else {
                    // ── Shu ombor kuryerlari (faqat shu ro'yxat scroll bo'ladi) ──
                    Text("Shu ombor kuryerlari (${attached.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    if (attached.isEmpty()) {
                        Text("Hali kuryer biriktirilmagan", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(attached, key = { it.id }) { c ->
                                Row(
                                    Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Default.DeliveryDining, null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(c.name.ifBlank { "Kuryer" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                            Text(
                                                "${c.phone} • ${c.currentDeliveries} ta faol",
                                                fontSize = 11.sp, color = MexaWarehouseColors.textCaption,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { if (!saving) pendingRemove = c }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Mavjud (biriktirilmagan) kuryerni biriktirish ──
                    Text("Mavjud kuryerni biriktirish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    if (unassigned.isEmpty()) {
                        Text("Bo'sh kuryer yo'q — barchasi omborlarga biriktirilgan", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(
                                expanded = attachExpanded,
                                onExpandedChange = {
                                    attachExpanded = it
                                    if (!it) attachSearch = ""
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                // Qidiruv dropdownning o'zida: yozgan zahoti ro'yxat filtrlanadi
                                OutlinedTextField(
                                    value = if (attachExpanded) attachSearch
                                    else selectedUnassigned?.let { "${it.name} (${it.phone})" } ?: "",
                                    onValueChange = {
                                        attachSearch = it
                                        if (!attachExpanded) attachExpanded = true
                                    },
                                    placeholder = { Text("Ism yoki telefon...", color = MexaWarehouseColors.textCaption) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = attachExpanded) },
                                    singleLine = true,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                ExposedDropdownMenu(expanded = attachExpanded, onDismissRequest = { attachExpanded = false; attachSearch = "" }) {
                                    val filteredUnassigned = unassigned.filter { u ->
                                        attachSearch.isBlank() ||
                                            u.name.contains(attachSearch, ignoreCase = true) ||
                                            u.phone.contains(attachSearch)
                                    }
                                    if (filteredUnassigned.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Topilmadi", fontSize = 12.sp, color = MexaWarehouseColors.textMuted) },
                                            onClick = {},
                                            enabled = false,
                                        )
                                    } else {
                                        filteredUnassigned.forEach { u ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(u.name.ifBlank { "Kuryer" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        Text(u.phone, fontSize = 11.sp, color = MexaWarehouseColors.textCaption)
                                                    }
                                                },
                                                onClick = { selectedUnassigned = u; attachExpanded = false; attachSearch = "" },
                                            )
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { attachExisting() },
                                enabled = !saving && selectedUnassigned != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                                shape = RoundedCornerShape(8.dp),
                            ) { Text("Biriktirish") }
                        }
                    }

                    error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) {
                            Text("Yopish")
                        }
                    }
                }
            }
        }
    }

    pendingRemove?.let { target ->
        Dialog(onDismissRequest = {}) {
            Surface(modifier = Modifier.widthIn(min = 300.dp, max = 440.dp), shape = RoundedCornerShape(14.dp), color = MexaWarehouseColors.surfaceLowest) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kuryerni bo'shatish", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text(
                        "\"${target.name}\" shu ombordan bo'shatiladi. Faol yetkazmasi bo'lsa — bo'shatib bo'lmaydi.",
                        fontSize = 13.sp, color = MexaWarehouseColors.textMuted,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { pendingRemove = null }, shape = RoundedCornerShape(8.dp)) { Text("Bekor") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { detach(target) },
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("Bo'shatish", color = Color.White) }
                    }
                }
            }
        }
    }
}

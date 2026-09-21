package mexa.club.desktop_app.market.ui.warehouses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
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
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class InventoryItemRow(
    val id: String,
    val productId: String,
    val productName: String,
    val systemQuantity: Double,
    val countedQuantity: Double?,
    val difference: Double?,
    val status: String,
)

private data class InventorySessionRow(
    val id: String,
    val status: String,
    val note: String,
    val startedAt: String,
    val completedAt: String,
    val items: List<InventoryItemRow> = emptyList(),
) {
    val isOpen: Boolean get() = status == "OPEN" || status == "IN_PROGRESS"
    val countedCount: Int get() = items.count { it.countedQuantity != null }
}

@Composable
internal fun InventoryTab(warehouseId: String, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<InventorySessionRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    var showStartDialog by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<InventorySessionRow?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/warehouses/$warehouseId/inventory?page=0&size=50")
                val root = ApiClient.parseJsonObject(text)
                val data = root?.let { ApiClient.dataObject(it) }
                (data?.itemsOrContentArray() ?: JsonArray(emptyList())).mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    val items = (o["items"] as? JsonArray)?.mapNotNull { ie ->
                        val i = ie as? JsonObject ?: return@mapNotNull null
                        InventoryItemRow(
                            id = i.stringField("id"),
                            productId = i.stringField("productId"),
                            productName = i.stringField("productName").ifEmpty { "Mahsulot #${i.stringField("productId").take(8)}" },
                            systemQuantity = (i["systemQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            countedQuantity = (i["countedQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                            difference = (i["difference"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                            status = i.stringField("status"),
                        )
                    } ?: emptyList()
                    InventorySessionRow(
                        id = o.stringField("id"),
                        status = o.stringField("status"),
                        note = o.stringField("note"),
                        startedAt = o.stringField("startedAt").take(16).replace("T", " "),
                        completedAt = o.stringField("completedAt").take(16).replace("T", " "),
                        items = items,
                    )
                }
            }
        }.onSuccess { sessions = it }
            .onFailure { error = it.message ?: "Xatolik yuz berdi" }
        loading = false
    }

    if (showStartDialog) {
        StartInventoryDialog(
            warehouseId = warehouseId,
            onDismiss = { showStartDialog = false },
            onStarted = {
                showStartDialog = false
                refreshKey++
                onChanged()
            },
        )
    }

    selectedSession?.let { session ->
        InventorySessionDialog(
            warehouseId = warehouseId,
            session = session,
            onDismiss = { selectedSession = null },
            onChanged = {
                selectedSession = null
                refreshKey++
                onChanged()
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Inventarizatsiya sessiyalari", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Fizik sanash sessiyalari va natijalari", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
            }
            Button(
                onClick = { showStartDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Yangi inventarizatsiya", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MexaWarehouseColors.surfaceLowest,
            border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("BOSHLANGAN", Modifier.weight(1.1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    Text("IZOH", Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    Text("SANALDI", Modifier.width(80.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    Text("HOLATI", Modifier.width(110.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(32.dp))
                    }

                    error != null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text(error ?: "Xatolik", color = MexaWarehouseColors.danger, fontSize = 14.sp)
                    }

                    sessions.isEmpty() -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Inventory, contentDescription = null, modifier = Modifier.size(44.dp), tint = MexaWarehouseColors.outlineVariant)
                            Text("Hozircha sessiyalar yo'q", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }

                    else -> sessions.forEachIndexed { index, session ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedSession = session }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(session.startedAt.ifEmpty { "—" }, Modifier.weight(1.1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                            Text(session.note.ifEmpty { "—" }, Modifier.weight(1.5f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${session.countedCount}/${session.items.size}",
                                Modifier.width(80.dp),
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            SessionStatusBadge(session.status, Modifier.width(110.dp))
                        }
                        if (index < sessions.lastIndex) {
                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        "COMPLETED" -> Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "YAKUNLANGAN")
        "CANCELLED" -> Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "BEKOR")
        "IN_PROGRESS" -> Triple(MexaWarehouseColors.statCyanBg, MexaWarehouseColors.statCyanFg, "JARAYONDA")
        else -> Triple(MexaWarehouseColors.infoBadgeBg, MexaWarehouseColors.indigoAccent, "OCHIQ")
    }
    Box(modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun StartInventoryDialog(warehouseId: String, onDismiss: () -> Unit, onStarted: () -> Unit) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.width(460.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Yangi inventarizatsiya", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text("Barcha stok qatorlari suratga olinadi va sanash sessiyasi ochiladi.", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IZOH (ixtiyoriy)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Masalan: Oylik sanash", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                            focusedBorderColor = MexaWarehouseColors.primary,
                            focusedContainerColor = MexaWarehouseColors.inputBg,
                            unfocusedContainerColor = MexaWarehouseColors.inputBg,
                        ),
                    )
                }
                if (error != null) {
                    Text(error ?: "", color = MexaWarehouseColors.danger, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(8.dp)) {
                        Text("Bekor qilish", color = MexaWarehouseColors.textMuted)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                error = null
                                val body = buildString { append("{\"note\":${note.trim().jsonString()}}") }
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { ApiClient.post("/api/warehouses/$warehouseId/inventory", body) }
                                }
                                result.onSuccess { saving = false; onStarted() }
                                    .onFailure { e -> error = e.message ?: "Xatolik yuz berdi"; saving = false }
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Boshlash", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySessionDialog(
    warehouseId: String,
    session: InventorySessionRow,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(session) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmingAction by remember { mutableStateOf<String?>(null) }
    val countedInputs = remember { mutableStateOf<MutableMap<String, String>>(mutableMapOf()) }

    fun refresh() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/warehouses/$warehouseId/inventory/${session.id}")
                    val o = ApiClient.parseJsonObject(text)?.get("data") as? JsonObject ?: return@withContext
                    val items = (o["items"] as? JsonArray)?.mapNotNull { ie ->
                        val i = ie as? JsonObject ?: return@mapNotNull null
                        InventoryItemRow(
                            id = i.stringField("id"),
                            productId = i.stringField("productId"),
                            productName = i.stringField("productName").ifEmpty { "Mahsulot" },
                            systemQuantity = (i["systemQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            countedQuantity = (i["countedQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                            difference = (i["difference"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                            status = i.stringField("status"),
                        )
                    } ?: emptyList()
                    current = InventorySessionRow(
                        id = o.stringField("id"),
                        status = o.stringField("status"),
                        note = o.stringField("note"),
                        startedAt = o.stringField("startedAt").take(16).replace("T", " "),
                        completedAt = o.stringField("completedAt").take(16).replace("T", " "),
                        items = items,
                    )
                }
            }.onFailure { error = it.message ?: "Xatolik yuz berdi" }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(0.92f),
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Orqaga", tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Inventarizatsiya — ${current.startedAt.ifEmpty { session.id.take(8) }}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                            if (current.note.isNotBlank()) {
                                Text(current.note, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                            }
                        }
                    }
                    SessionStatusBadge(current.status)
                }

                if (current.isOpen) {
                    Row(
                        Modifier.fillMaxWidth().background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Mahsulotlar sanalgan miqdorni kiriting. Yakunlashda farqlar stokga qo'llanadi.",
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.onSurface,
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { confirmingAction = "CANCEL" },
                                enabled = !saving,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Bekor qilish", color = MexaWarehouseColors.textMuted)
                            }
                            Button(
                                onClick = { confirmingAction = "COMPLETE" },
                                enabled = !saving,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.statusActiveFg),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Yakunlash", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(error ?: "", color = MexaWarehouseColors.danger, fontSize = 13.sp)
                }

                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MexaWarehouseColors.surfaceLowest,
                    border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("MAHSULOT", Modifier.weight(1.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                            Text("TIZIMDA", Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                            Text("SANALDI", Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                            Text("FARQ", Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                            Text("HOLATI", Modifier.width(90.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                        }
                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)

                        if (current.items.isEmpty()) {
                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                Text("Ma'lumot yo'q", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                            }
                        } else {
                            current.items.forEachIndexed { index, item ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(item.productName, Modifier.weight(1.6f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatQty(item.systemQuantity), Modifier.weight(0.7f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                                    if (current.isOpen) {
                                        OutlinedTextField(
                                            value = countedInputs.value[item.id] ?: item.countedQuantity?.let { formatQty(it) }.orEmpty(),
                                            onValueChange = { countedInputs.value[item.id] = it },
                                            placeholder = { Text("—") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(0.9f).height(40.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                                                focusedBorderColor = MexaWarehouseColors.primary,
                                                focusedContainerColor = MexaWarehouseColors.inputBg,
                                                unfocusedContainerColor = MexaWarehouseColors.inputBg,
                                            ),
                                        )
                                    } else {
                                        Text(item.countedQuantity?.let { formatQty(it) } ?: "—", Modifier.weight(0.9f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                    }
                                    val diff = item.difference
                                    Text(
                                        if (diff == null) "—" else (if (diff > 0) "+" else "") + formatQty(diff),
                                        Modifier.weight(0.7f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            diff == null -> MexaWarehouseColors.textMuted
                                            diff > 0 -> MexaWarehouseColors.statusActiveFg
                                            diff < 0 -> MexaWarehouseColors.danger
                                            else -> MexaWarehouseColors.textMuted
                                        },
                                    )
                                    Text(
                                        itemStatusLabel(item.status),
                                        Modifier.width(90.dp),
                                        fontSize = 12.sp,
                                        color = MexaWarehouseColors.textMuted,
                                    )
                                }
                                if (index < current.items.lastIndex) {
                                    HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                                }
                            }

                            if (current.isOpen) {
                                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                current.items.forEach { item ->
                                                    val raw = countedInputs.value[item.id]?.trim()
                                                    if (raw.isNullOrEmpty()) return@forEach
                                                    val qty = raw.toDoubleOrNull() ?: return@forEach
                                                    runCatching {
                                                        withContext(Dispatchers.IO) {
                                                            ApiClient.put(
                                                                "/api/warehouses/$warehouseId/inventory/${current.id}/items/${item.id}",
                                                                "{\"countedQuantity\":$qty}",
                                                            )
                                                        }
                                                    }
                                                }
                                                refresh()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("Barcha qiymatlarni saqlash", fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.indigoAccent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmingAction?.let { action ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmingAction = null },
            title = { Text(if (action == "COMPLETE") "Sessiyani yakunlash" else "Sessiyani bekor qilish") },
            text = {
                Text(
                    if (action == "COMPLETE") "Sanagan farqlar stokga qo'llanadi. Davom etasizmi?" else "Sessiya bekor qilinadi, stok o'zgarmaydi. Davom etasizmi?",
                    color = MexaWarehouseColors.textMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingAction = null
                    scope.launch {
                        saving = true
                        error = null
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                ApiClient.post("/api/warehouses/$warehouseId/inventory/${current.id}/${if (action == "COMPLETE") "complete" else "cancel"}", "{}")
                            }
                        }
                        result.onSuccess { saving = false; onChanged() }
                            .onFailure { e -> error = e.message ?: "Xatolik yuz berdi"; saving = false }
                    }
                }) {
                    Text(
                        if (action == "COMPLETE") "Yakunlash" else "Bekor qilish",
                        color = if (action == "COMPLETE") MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.danger,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingAction = null }) {
                    Text("Orqaga", color = MexaWarehouseColors.textMuted)
                }
            },
        )
    }
}

private fun itemStatusLabel(status: String): String = when (status) {
    "COUNTED" -> "SANALDI"
    "APPROVED" -> "TASDIQLANDI"
    else -> "KUTILMOQDA"
}

private fun formatQty(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

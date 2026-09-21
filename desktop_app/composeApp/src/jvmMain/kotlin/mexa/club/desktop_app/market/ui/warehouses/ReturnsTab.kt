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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
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
import java.util.UUID
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class ReturnItemRow(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val condition: String,
)

private data class StockReturnRow(
    val id: String,
    val status: String,
    val reason: String,
    val note: String,
    val orderId: String,
    val createdAt: String,
    val items: List<ReturnItemRow> = emptyList(),
) {
    val isPending: Boolean get() = status == "PENDING"
}

@Composable
internal fun ReturnsTab(warehouseId: String, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var returns by remember { mutableStateOf<List<StockReturnRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedReturn by remember { mutableStateOf<StockReturnRow?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/warehouses/$warehouseId/returns?page=0&size=50")
                val root = ApiClient.parseJsonObject(text)
                val data = root?.let { ApiClient.dataObject(it) }
                (data?.itemsOrContentArray() ?: JsonArray(emptyList())).mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    val items = (o["items"] as? JsonArray)?.mapNotNull { ie ->
                        val i = ie as? JsonObject ?: return@mapNotNull null
                        ReturnItemRow(
                            id = i.stringField("id"),
                            productId = i.stringField("productId"),
                            productName = i.stringField("productName").ifEmpty { "Mahsulot #${i.stringField("productId").take(8)}" },
                            quantity = (i["quantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            condition = i.stringField("condition"),
                        )
                    } ?: emptyList()
                    StockReturnRow(
                        id = o.stringField("id"),
                        status = o.stringField("status"),
                        reason = o.stringField("reason"),
                        note = o.stringField("note"),
                        orderId = o.stringField("orderId"),
                        createdAt = o.stringField("createdAt").take(16).replace("T", " "),
                        items = items,
                    )
                }
            }
        }.onSuccess { returns = it }
            .onFailure { error = it.message ?: "Xatolik yuz berdi" }
        loading = false
    }

    LaunchedEffect(refreshKey) { load() }

    if (showCreateDialog) {
        CreateReturnDialog(
            warehouseId = warehouseId,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
                refreshKey++
                onChanged()
            },
        )
    }

    selectedReturn?.let { returnRow ->
        ReturnDetailDialog(
            warehouseId = warehouseId,
            returnRow = returnRow,
            onDismiss = { selectedReturn = null },
            onChanged = {
                selectedReturn = null
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
                Text("Qaytarmalar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Xaridor va yetkazib beruvchi qaytarmalari", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
            }
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Qaytarma yaratish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    Text("SANASI", Modifier.weight(1.1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    Text("SABAB", Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    Text("MAHSULOTLAR", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
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

                    returns.isEmpty() -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(44.dp), tint = MexaWarehouseColors.outlineVariant)
                            Text("Qaytarmalar yo'q", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }

                    else -> returns.forEachIndexed { index, returnRow ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedReturn = returnRow }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(returnRow.createdAt.ifEmpty { "â€”" }, Modifier.weight(1.1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                            Text(returnRow.reason.ifEmpty { "â€”" }, Modifier.weight(1.5f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${returnRow.items.size} ta", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            Row(Modifier.width(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                ReturnStatusBadge(returnRow.status)
                                if (returnRow.isPending && processingId != returnRow.id) {
                                    IconButton(
                                        onClick = { selectedReturn = returnRow },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        if (index < returns.lastIndex) {
                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnStatusBadge(status: String) {
    val (bg, fg, label) = when (status) {
        "APPROVED" -> Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "TASDIQLANDI")
        "REJECTED" -> Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "RAD ETILDI")
        else -> Triple(MexaWarehouseColors.statCyanBg, MexaWarehouseColors.statCyanFg, "KUTILMOQDA")
    }
    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun CreateReturnDialog(
    warehouseId: String,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var orderId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ReturnDraftItem>>(listOf(ReturnDraftItem())) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<ProductChoice>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val loaded = mutableListOf<ProductChoice>()
                var page = 0
                while (true) {
                    val text = ApiClient.get("/api/products?page=$page&size=500&active=true")
                    val root = ApiClient.parseJsonObject(text)
                    val data = root?.let { ApiClient.dataObjectOrSelf(it) }
                    val arr = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    if (arr.isEmpty()) break
                    loaded.addAll(arr.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        val id = o.stringField("id").ifEmpty { return@mapNotNull null }
                        ProductChoice(id = id, name = o.stringField("name").ifEmpty { id.take(8) })
                    })
                    if (arr.size < 500) break
                    page++
                    if (page > 40) break
                }
                loaded
            }
        }.onSuccess { products = it }
    }

    fun save() {
        if (reason.isBlank()) {
            error = "Sabab majburiy"
            return
        }
        if (orderId.isNotBlank() && runCatching { UUID.fromString(orderId.trim()) }.isFailure) {
            error = "Buyurtma ID to'g'ri UUID formatida bo'lishi kerak"
            return
        }
        val validItems = items.filter { it.productId.isNotBlank() && it.quantity > 0 }
        if (validItems.isEmpty()) {
            error = "Kamida bitta mahsulot qo'shing"
            return
        }
        val body = buildString {
            append("{")
            if (orderId.isNotBlank()) append("\"orderId\":${orderId.trim().jsonString()},")
            append("\"reason\":${reason.trim().jsonString()},")
            if (note.isNotBlank()) append("\"note\":${note.trim().jsonString()},")
            append("\"items\":[")
            validItems.forEachIndexed { index, it ->
                if (index > 0) append(",")
                append("{\"productId\":${it.productId.jsonString()},\"quantity\":${it.quantity},\"condition\":${it.condition.jsonString()}}")
            }
            append("]}")
        }
        scope.launch {
            saving = true
            error = null
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.post("/api/warehouses/$warehouseId/returns", body) }
            }
            result.onSuccess { saving = false; onCreated() }
                .onFailure { e -> error = e.message ?: "Xatolik yuz berdi"; saving = false }
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
            modifier = Modifier.widthIn(min = 420.dp, max = 760.dp).fillMaxWidth(0.9f),
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Qaytarma yaratish", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SABAB *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it; error = null },
                        placeholder = { Text("Masalan: Mahsulot sifatsiz", fontSize = 13.sp) },
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("BUYURTMA ID (ixtiyoriy)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    OutlinedTextField(
                        value = orderId,
                        onValueChange = { orderId = it },
                        placeholder = { Text("UUID", fontSize = 13.sp) },
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IZOH (ixtiyoriy)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Qo'shimcha izoh", fontSize = 13.sp) },
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

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("MAHSULOTLAR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                    OutlinedButton(
                        onClick = { items = items + ReturnDraftItem() },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Qator qo'shish", fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Mahsulot qidirish...", fontSize = 13.sp) },
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

                items.forEachIndexed { index, item ->
                    ReturnDraftRow(
                        item = item,
                        products = products.filter { searchQuery.isBlank() || it.name.contains(searchQuery.trim(), ignoreCase = true) },
                        onUpdate = { updated -> items = items.toMutableList().apply { this[index] = updated } },
                        onRemove = { items = items.toMutableList().apply { removeAt(index) } },
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
                        onClick = { save() },
                        enabled = !saving,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Yuborish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class ReturnDraftItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val condition: String = "GOOD",
)

private data class ProductChoice(val id: String, val name: String)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReturnDraftRow(
    item: ReturnDraftItem,
    products: List<ProductChoice>,
    onUpdate: (ReturnDraftItem) -> Unit,
    onRemove: () -> Unit,
) {
    var productExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf(item.quantity.takeIf { it > 0 }?.toString().orEmpty()) }

    Row(
        Modifier.fillMaxWidth().background(MexaWarehouseColors.surfaceContainerLow, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = productExpanded,
            onExpandedChange = { productExpanded = it },
            modifier = Modifier.weight(1.6f),
        ) {
            OutlinedTextField(
                value = item.productName,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Mahsulot tanlang", fontSize = 13.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                    focusedBorderColor = MexaWarehouseColors.primary,
                    focusedContainerColor = MexaWarehouseColors.inputBg,
                    unfocusedContainerColor = MexaWarehouseColors.inputBg,
                ),
            )
            ExposedDropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                products.take(60).forEach { p ->
                    DropdownMenuItemCompat(
                        text = p.name,
                        onClick = {
                            onUpdate(item.copy(productId = p.id, productName = p.name))
                            productExpanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = { raw ->
                quantityText = raw.filter { it.isDigit() || it == '.' }
                onUpdate(item.copy(quantity = quantityText.toDoubleOrNull() ?: 0.0))
            },
            placeholder = { Text("Miqdor", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.width(110.dp).height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                focusedBorderColor = MexaWarehouseColors.primary,
                focusedContainerColor = MexaWarehouseColors.inputBg,
                unfocusedContainerColor = MexaWarehouseColors.inputBg,
            ),
        )

        ExposedDropdownMenuBox(
            expanded = conditionExpanded,
            onExpandedChange = { conditionExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = when (item.condition) {
                    "GOOD" -> "YAXSHI"
                    "DAMAGED" -> "SHIKASTLANGAN"
                    "EXPIRED" -> "MUDDATI O'TGAN"
                    else -> item.condition
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                    focusedBorderColor = MexaWarehouseColors.primary,
                    focusedContainerColor = MexaWarehouseColors.inputBg,
                    unfocusedContainerColor = MexaWarehouseColors.inputBg,
                ),
            )
            ExposedDropdownMenu(expanded = conditionExpanded, onDismissRequest = { conditionExpanded = false }) {
                listOf("GOOD" to "YAXSHI", "DAMAGED" to "SHIKASTLANGAN", "EXPIRED" to "MUDDATI O'TGAN").forEach { (code, label) ->
                    DropdownMenuItemCompat(label, onClick = {
                        onUpdate(item.copy(condition = code))
                        conditionExpanded = false
                    })
                }
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "O'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DropdownMenuItemCompat(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 13.sp) },
        onClick = onClick,
    )
}

@Composable
private fun ReturnDetailDialog(
    warehouseId: String,
    returnRow: StockReturnRow,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(0.9f),
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Orqaga", tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Qaytarma â€” ${returnRow.createdAt.ifEmpty { returnRow.id.take(8) }}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                            if (returnRow.reason.isNotBlank()) {
                                Text(returnRow.reason, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                            }
                        }
                    }
                    ReturnStatusBadge(returnRow.status)
                }

                if (returnRow.orderId.isNotBlank()) {
                    Text("Buyurtma ID: ${returnRow.orderId}", fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                }
                if (returnRow.note.isNotBlank()) {
                    Text("Izoh: ${returnRow.note}", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
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
                            Text("MIQDOR", Modifier.width(90.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                            Text("HOLAT", Modifier.width(120.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                        }
                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                        if (returnRow.items.isEmpty()) {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("Ma'lumot yo'q", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                            }
                        } else {
                            returnRow.items.forEachIndexed { index, item ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.productName, Modifier.weight(1.6f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ReturnDetailFormat.qty(item.quantity), Modifier.width(90.dp), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                    Text(
                                        when (item.condition) {
                                            "GOOD" -> "YAXSHI"
                                            "DAMAGED" -> "SHIKASTLANGAN"
                                            "EXPIRED" -> "MUDDATI O'TGAN"
                                            else -> item.condition
                                        },
                                        Modifier.width(120.dp),
                                        fontSize = 12.sp,
                                        color = if (item.condition == "GOOD") MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.textMuted,
                                    )
                                }
                                if (index < returnRow.items.lastIndex) {
                                    HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }

                if (returnRow.isPending) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    processing = true
                                    error = null
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { ApiClient.put("/api/warehouses/$warehouseId/returns/${returnRow.id}/reject", "{}") }
                                    }
                                    result.onSuccess { processing = false; onChanged() }
                                        .onFailure { e -> error = e.message ?: "Xatolik yuz berdi"; processing = false }
                                }
                            },
                            enabled = !processing,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Rad etish", color = MexaWarehouseColors.danger, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    processing = true
                                    error = null
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { ApiClient.put("/api/warehouses/$warehouseId/returns/${returnRow.id}/approve", "{}") }
                                    }
                                    result.onSuccess { processing = false; onChanged() }
                                        .onFailure { e -> error = e.message ?: "Xatolik yuz berdi"; processing = false }
                                }
                            },
                            enabled = !processing,
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.statusActiveFg),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Tasdiqlash", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private object ReturnDetailFormat {
    fun qty(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}

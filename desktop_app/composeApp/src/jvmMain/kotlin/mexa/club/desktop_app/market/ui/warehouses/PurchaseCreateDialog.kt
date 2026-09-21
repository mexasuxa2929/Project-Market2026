package mexa.club.desktop_app.market.ui.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

private data class PurchaseProduct(
    val id: String,
    val name: String,
    val barcode: String,
    val active: Boolean,
)

private class PurchaseRowState {
    var productId by mutableStateOf("")
    var nameQuery by mutableStateOf("")
    var barcodeQuery by mutableStateOf("")
    var nameExpanded by mutableStateOf(false)
    var barcodeExpanded by mutableStateOf(false)
    var quantity by mutableStateOf("")
    var unitPrice by mutableStateOf("")
    var nameError by mutableStateOf<String?>(null)
    var barcodeError by mutableStateOf<String?>(null)
    var quantityError by mutableStateOf<String?>(null)
    var unitPriceError by mutableStateOf<String?>(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseItemRow(
    products: List<PurchaseProduct>,
    row: PurchaseRowState,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mahsulot nomi bo'yicha qidiruv
        Box(Modifier.weight(2.2f)) {
            ExposedDropdownMenuBox(expanded = row.nameExpanded, onExpandedChange = { row.nameExpanded = it }) {
                OutlinedTextField(
                    value = row.nameQuery,
                    onValueChange = {
                        row.nameQuery = it
                        row.nameError = null
                        row.productId = ""
                        row.nameExpanded = true
                        val exact = products.firstOrNull { p -> p.name.equals(it.trim(), ignoreCase = true) }
                        if (exact != null) {
                            row.productId = exact.id
                            row.barcodeQuery = exact.barcode
                            row.barcodeError = null
                        } else if (it.isBlank()) {
                            row.barcodeQuery = ""
                        }
                    },
                    placeholder = { Text("Mahsulot nomi...", color = MexaWarehouseColors.textCaption) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = row.nameExpanded) },
                    singleLine = true,
                    enabled = enabled,
                    isError = row.nameError != null,
                    supportingText = row.nameError?.let { msg -> { Text(msg, fontSize = 11.sp, color = MexaWarehouseColors.danger) } },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = row.nameExpanded, onDismissRequest = { row.nameExpanded = false }) {
                    val filtered = products.filter {
                        row.nameQuery.isBlank() || it.name.contains(row.nameQuery.trim(), ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                            onClick = {},
                            enabled = false,
                        )
                    } else {
                        filtered.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    row.productId = p.id
                                    row.nameQuery = p.name
                                    row.barcodeQuery = p.barcode
                                    row.nameError = null
                                    row.barcodeError = null
                                    row.nameExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        // Barcode bo'yicha qidiruv
        Box(Modifier.weight(1.4f)) {
            ExposedDropdownMenuBox(expanded = row.barcodeExpanded, onExpandedChange = { row.barcodeExpanded = it }) {
                OutlinedTextField(
                    value = row.barcodeQuery,
                    onValueChange = {
                        row.barcodeQuery = it
                        row.barcodeError = null
                        row.productId = ""
                        row.barcodeExpanded = true
                        val exact = products.firstOrNull { p ->
                            p.barcode.isNotEmpty() && p.barcode.equals(it.trim(), ignoreCase = true)
                        }
                        if (exact != null) {
                            row.productId = exact.id
                            row.nameQuery = exact.name
                            row.nameError = null
                        } else if (it.isBlank()) {
                            row.nameQuery = ""
                        }
                    },
                    placeholder = { Text("Barcode...", color = MexaWarehouseColors.textCaption) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = row.barcodeExpanded) },
                    singleLine = true,
                    enabled = enabled,
                    isError = row.barcodeError != null,
                    supportingText = row.barcodeError?.let { msg -> { Text(msg, fontSize = 11.sp, color = MexaWarehouseColors.danger) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = row.barcodeExpanded, onDismissRequest = { row.barcodeExpanded = false }) {
                    val filtered = products.filter {
                        it.barcode.isNotEmpty() &&
                            (row.barcodeQuery.isBlank() || it.barcode.contains(row.barcodeQuery.trim(), ignoreCase = true))
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                            onClick = {},
                            enabled = false,
                        )
                    } else {
                        filtered.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.barcode, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    row.productId = p.id
                                    row.barcodeQuery = p.barcode
                                    row.nameQuery = p.name
                                    row.nameError = null
                                    row.barcodeError = null
                                    row.barcodeExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        // Soni
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = row.quantity,
                onValueChange = {
                    if (it.all { c -> c.isDigit() || c == '.' }) row.quantity = it
                    row.quantityError = null
                },
                placeholder = { Text("Soni", color = MexaWarehouseColors.textCaption) },
                singleLine = true,
                enabled = enabled,
                isError = row.quantityError != null,
                supportingText = row.quantityError?.let { msg -> { Text(msg, fontSize = 11.sp, color = MexaWarehouseColors.danger) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
            )
        }
        // Tannarx
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = row.unitPrice,
                onValueChange = {
                    if (it.all { c -> c.isDigit() || c == '.' }) row.unitPrice = it
                    row.unitPriceError = null
                },
                placeholder = { Text("Tannarx", color = MexaWarehouseColors.textCaption) },
                singleLine = true,
                enabled = enabled,
                isError = row.unitPriceError != null,
                supportingText = row.unitPriceError?.let { msg -> { Text(msg, fontSize = 11.sp, color = MexaWarehouseColors.danger) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
            )
        }
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Default.Close, null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun PurchaseCreateDialog(
    warehouseId: String,
    pendingTransfers: List<TransferRow> = emptyList(),
    onTransfersChanged: () -> Unit = {},
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var products by remember { mutableStateOf<List<PurchaseProduct>>(emptyList()) }
    val rows = remember { mutableStateListOf(PurchaseRowState()) }

    LaunchedEffect(Unit) {
        runCatching {
            val prodText = withContext(Dispatchers.IO) { ApiClient.get("/api/products?page=0&size=200&active=true") }
            val prodObj = ApiClient.parseJsonObject(prodText)?.let { ApiClient.dataObjectOrSelf(it) }
            products = (prodObj?.itemsOrContentArray() ?: JsonArray(emptyList())).mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val id = o.stringField("id").ifEmpty { return@mapNotNull null }
                PurchaseProduct(
                    id = id,
                    name = o.stringField("name").ifEmpty { "Mahsulot" },
                    barcode = o.stringField("barcode"),
                    active = (o["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                )
            }.filter { it.active }
        }.onFailure { error = it.message ?: "Xatolik" }
        loading = false
    }

    fun save() {
        var hasError = false
        val resolvedRows = rows.map { r ->
            r.nameError = null
            r.barcodeError = null
            r.quantityError = null
            r.unitPriceError = null

            val nameQ = r.nameQuery.trim()
            val barcodeQ = r.barcodeQuery.trim()
            val nameMatch = products.firstOrNull { it.name.equals(nameQ, ignoreCase = true) }
            val barcodeMatch = if (barcodeQ.isNotEmpty())
                products.firstOrNull { it.barcode.isNotEmpty() && it.barcode.equals(barcodeQ, ignoreCase = true) }
            else null

            val pid = if (r.productId.isNotEmpty()) r.productId
            else when {
                nameMatch != null && barcodeMatch == null -> nameMatch.id
                barcodeMatch != null && nameMatch == null -> barcodeMatch.id
                nameMatch != null && barcodeMatch != null && nameMatch.id == barcodeMatch.id -> nameMatch.id
                else -> ""
            }

            if (pid.isEmpty()) {
                hasError = true
                if (nameMatch != null && barcodeMatch != null && nameMatch.id != barcodeMatch.id) {
                    r.nameError = "Nom va barcode bir mahsulotga mos kelmaydi"
                    r.barcodeError = "Nom va barcode bir mahsulotga mos kelmaydi"
                } else if (nameQ.isNotEmpty() && barcodeQ.isNotEmpty()) {
                    r.nameError = "Bazada topilmadi"
                    r.barcodeError = "Bazada topilmadi"
                } else if (nameQ.isNotEmpty()) {
                    r.nameError = "Bazada topilmadi"
                } else if (barcodeQ.isNotEmpty()) {
                    r.barcodeError = "Bazada topilmadi"
                } else {
                    r.nameError = "Mahsulot tanlang"
                }
            }
            if (r.quantity.toDoubleOrNull()?.let { q -> q > 0 } != true) {
                hasError = true
                r.quantityError = "To'g'ri kiriting"
            }
            if (r.unitPrice.toDoubleOrNull()?.let { p -> p > 0 } != true) {
                hasError = true
                r.unitPriceError = "To'g'ri kiriting"
            }
            r to pid
        }
        if (hasError) {
            error = "Xatolik yuz berdi — qizil bilan belgilangan maydonlarni tuzating"
            return
        }
        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{\"items\":[")
                resolvedRows.forEachIndexed { i, (r, pid) ->
                    if (i > 0) append(",")
                    append("{\"productId\":${pid.jsonString()},\"quantity\":${r.quantity},\"unitPrice\":${r.unitPrice}}")
                }
                append("]}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.post("/api/warehouses/$warehouseId/purchases", json) }
            }
            result.onSuccess {
                saving = false
                onSaved()
            }.onFailure { e ->
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
            modifier = Modifier.widthIn(min = 720.dp, max = 1320.dp).fillMaxWidth(0.96f),
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
                        "Omborga kirim",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                PendingTransfersSection(
                    warehouseId = warehouseId,
                    transfers = pendingTransfers,
                    onTransfersChanged = onTransfersChanged,
                )

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Kelgan mahsulotlar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            OutlinedButton(
                                onClick = { rows.add(PurchaseRowState()) },
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Qator qo'shish")
                            }
                        }

                        rows.forEachIndexed { index, row ->
                            PurchaseItemRow(
                                products = products,
                                row = row,
                                enabled = !saving,
                                onRemove = { if (rows.size > 1) rows.removeAt(index) },
                            )
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
    }
}

// ─── Pending transfers (Kirim tasdiqlash) ──────────────────────────────────────

@Composable
private fun PendingTransfersSection(
    warehouseId: String,
    transfers: List<TransferRow>,
    onTransfersChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var localTransfers by remember { mutableStateOf(transfers) }
    var decision by remember { mutableStateOf<Pair<TransferRow, Boolean>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(transfers) { localTransfers = transfers }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kutilayotgan kirimlar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
            if (localTransfers.isNotEmpty()) {
                Box(
                    Modifier.background(Color(0xFFEF4444), RoundedCornerShape(999.dp)).padding(horizontal = 7.dp, vertical = 1.dp),
                ) {
                    Text(localTransfers.size.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Surface(
            Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MexaWarehouseColors.surfaceLowest,
        ) {
            Column {
                Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    TableHead("YUBORUVCHI OMBOR", Modifier.weight(1.5f))
                    TableHead("MAHSULOT", Modifier.weight(2f))
                    TableHead("MIQDOR", Modifier.weight(1f))
                    TableHead("SANA", Modifier.weight(1.2f))
                    TableHead("AMALLAR", Modifier.weight(1.3f))
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                if (localTransfers.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        Text("Kutilayotgan kirimlar yo'q", color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
                    }
                } else {
                    Column {
                        localTransfers.forEachIndexed { idx, tr ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(tr.fromWarehouseName, Modifier.weight(1.5f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tr.productName, Modifier.weight(2f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatNum(tr.quantity.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                Text(tr.createdAt.ifEmpty { "—" }, Modifier.weight(1.2f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                Row(Modifier.weight(1.3f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { decision = tr to true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = ButtonDefaults.ContentPadding,
                                    ) {
                                        Text("Tasdiqlash", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { decision = tr to false },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = ButtonDefaults.ContentPadding,
                                    ) {
                                        Text("Rad etish", fontSize = 12.sp, color = MexaWarehouseColors.danger)
                                    }
                                }
                            }
                            if (idx < localTransfers.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                            }
                        }
                    }
                }
            }
        }
    }

    decision?.let { (tr, approve) ->
        TransferDecisionDialog(
            transfer = tr,
            approve = approve,
            busy = busy,
            error = error,
            onDismiss = { decision = null; error = null },
            onConfirm = { note ->
                scope.launch {
                    busy = true
                    error = null
                    val path = if (approve)
                        "/api/warehouses/$warehouseId/stock/transfers/${tr.id}/confirm"
                    else
                        "/api/warehouses/$warehouseId/stock/transfers/${tr.id}/reject"
                    val body = """{"note":${note.jsonString()}}"""
                    val result = withContext(Dispatchers.IO) {
                        runCatching { ApiClient.post(path, body) }
                    }
                    result.onSuccess {
                        busy = false
                        decision = null
                        localTransfers = localTransfers.filterNot { it.id == tr.id }
                        onTransfersChanged()
                    }.onFailure { e ->
                        busy = false
                        error = e.message ?: "Xatolik yuz berdi"
                    }
                }
            },
        )
    }
}

@Composable
private fun TransferDecisionDialog(
    transfer: TransferRow,
    approve: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 360.dp, max = 480.dp).fillMaxWidth(0.9f),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (approve) "Kirimni tasdiqlash" else "Kirimni rad etish",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                )
                Text(
                    "${transfer.fromWarehouseName} → joriy ombor",
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                Text(
                    "${transfer.productName} — ${formatNum(transfer.quantity.toInt())} dona",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Izoh (ixtiyoriy)", color = MexaWarehouseColors.textCaption) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp),
                )
                error?.let { err ->
                    Text(err, color = MexaWarehouseColors.danger, fontSize = 13.sp)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = { if (!busy) onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Bekor qilish")
                    }
                    Button(
                        onClick = { onConfirm(note.trim()) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (approve) Color(0xFF16A34A) else MexaWarehouseColors.danger,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (approve) "Tasdiqlash" else "Rad etish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

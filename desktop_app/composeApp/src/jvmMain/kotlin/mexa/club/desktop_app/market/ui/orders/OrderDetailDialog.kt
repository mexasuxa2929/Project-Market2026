package mexa.club.desktop_app.market.ui.orders

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class OrderItemRow(
    val productName: String,
    val quantity: Int,
    val unitPrice: String,
    val subtotal: String,
)

private data class OrderDetail(
    val orderNumber: String,
    val userId: String,
    val status: String,
    val paymentStatus: String,
    val currency: String,
    val subtotal: String,
    val deliveryFee: String,
    val totalAmount: String,
    val discountAmount: String,
    val deliveryAddress: String,
    val note: String,
    val cancelReason: String,
    val adminNote: String,
    val createdAt: String,
    val items: List<OrderItemRow>,
)

private data class StatusHistoryRow(
    val fromStatus: String,
    val toStatus: String,
    val changedBy: String,
    val reason: String,
    val createdAt: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailDialog(
    orderId: String,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<OrderDetail?>(null) }
    var statusExpanded by remember { mutableStateOf(false) }
    var newStatus by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var adminNoteText by remember { mutableStateOf("") }
    var savingNote by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<StatusHistoryRow>>(emptyList()) }
    var loadingHistory by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    suspend fun fetchDetail(): OrderDetail = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/orders/$orderId")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
            ?: throw IllegalStateException("Buyurtma topilmadi")
        val itemsArr = data["items"] as? JsonArray ?: JsonArray(emptyList())
        OrderDetail(
            orderNumber = data.stringField("orderNumber"),
            userId = data.stringField("userId"),
            status = data.stringField("status").ifEmpty { "PENDING" },
            paymentStatus = data.stringField("paymentStatus").ifEmpty { "UNPAID" },
            currency = data.stringField("currency").ifEmpty { "UZS" },
            subtotal = data.stringField("subtotal").ifEmpty { "0" },
            deliveryFee = data.stringField("deliveryFee").ifEmpty { "0" },
            totalAmount = data.stringField("totalAmount").ifEmpty { "0" },
            discountAmount = data.stringField("discountAmount").ifEmpty { "0" },
            deliveryAddress = data.stringField("deliveryAddress"),
            note = data.stringField("note"),
            cancelReason = data.stringField("cancelReason"),
            adminNote = data.stringField("adminNote"),
            createdAt = data.stringField("createdAt"),
            items = itemsArr.mapNotNull { el ->
                val it = el as? JsonObject ?: return@mapNotNull null
                OrderItemRow(
                    productName = it.stringField("productName").ifEmpty { "Mahsulot" },
                    quantity = it.stringField("quantity").toIntOrNull() ?: 1,
                    unitPrice = it.stringField("unitPrice").ifEmpty { "0" },
                    subtotal = it.stringField("subtotal").ifEmpty { "0" },
                )
            },
        )
    }

    suspend fun fetchHistory(): List<StatusHistoryRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/orders/$orderId/history")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) } ?: return@withContext emptyList()
        val arr = data as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            StatusHistoryRow(
                fromStatus = o.stringField("fromStatus"),
                toStatus = o.stringField("toStatus"),
                changedBy = o.stringField("changedBy"),
                reason = o.stringField("reason"),
                createdAt = o.stringField("createdAt"),
            )
        }
    }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchDetail() }
                .onSuccess { detail = it; newStatus = it.status; adminNoteText = it.adminNote }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    LaunchedEffect(orderId) { load() }

    fun saveStatus() {
        scope.launch {
            saving = true
            saveError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = buildString {
                        append("{\"status\":\"").append(newStatus).append("\"")
                        if (reason.isNotBlank()) {
                            append(",\"reason\":\"").append(reason.replace("\"", "\\\"")).append("\"")
                        }
                        append("}")
                    }
                    ApiClient.put("/api/admin/orders/$orderId/status", body)
                }
            }.onSuccess { onUpdated() }
                .onFailure { saveError = it.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    fun saveAdminNote() {
        scope.launch {
            savingNote = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = "{\"note\":\"${adminNoteText.replace("\"", "\\\"")}\"}"
                    ApiClient.put("/api/admin/orders/$orderId/admin-note", body)
                }
            }.onFailure { /* ignore */ }
            savingNote = false
        }
    }

    fun refundOrder() {
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = if (reason.isNotBlank()) "{\"reason\":\"${reason.replace("\"", "\\\"")}\"}" else "{}"
                    ApiClient.post("/api/admin/orders/$orderId/refund", body)
                }
            }.onSuccess { onUpdated() }
                .onFailure { saveError = it.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    fun deleteOrder() {
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.delete("/api/admin/orders/$orderId")
                }
            }.onSuccess { onUpdated() }
                .onFailure { saveError = it.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    fun toggleHistory() {
        if (showHistory) {
            showHistory = false
        } else {
            showHistory = true
            loadingHistory = true
            scope.launch {
                runCatching { fetchHistory() }
                    .onSuccess { history = it }
                loadingHistory = false
            }
        }
    }

    fun formatHistoryDate(iso: String): String {
        if (iso.isBlank()) return "—"
        return runCatching {
            val parsed = if (iso.contains("T") && !iso.contains("+") && !iso.endsWith("Z")) {
                java.time.LocalDateTime.parse(iso).atZone(ZoneId.systemDefault())
            } else {
                OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
            }
            parsed.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH))
        }.getOrElse { iso }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 500.dp, max = 600.dp).heightIn(max = 700.dp),
        ) {
            when {
                loading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                }
                error != null || detail == null -> Box(Modifier.fillMaxWidth().height(200.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Xatolik", color = MexaWarehouseColors.danger)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onDismiss) { Text("Yopish") }
                    }
                }
                else -> {
                    val d = detail!!
                    Column(
                        Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("#${d.orderNumber}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                                Text(formatOrderDate(d.createdAt), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OrderStatusBadge(d.status)
                            }
                        }

                        DetailRow("To'lov holati", when (d.paymentStatus.uppercase(Locale.getDefault())) {
                            "PAID" -> "To'langan"
                            "REFUNDED" -> "Qaytarilgan"
                            else -> "To'lanmagan"
                        })
                        if (d.deliveryAddress.isNotBlank()) {
                            DetailRow("Yetkazib berish manzili", d.deliveryAddress)
                        }
                        if (d.note.isNotBlank()) {
                            DetailRow("Izoh", d.note)
                        }
                        if (d.cancelReason.isNotBlank()) {
                            DetailRow("Bekor qilish sababi", d.cancelReason)
                        }

                        // ── Items ────────────────────────────────────────────────
                        Text("Mahsulotlar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (d.items.isEmpty()) {
                                Text("Mahsulotlar topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            }
                            d.items.forEach { item ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                        Text("${item.quantity} x ${formatMoney(item.unitPrice, d.currency)}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                    }
                                    Text(formatMoney(item.subtotal, d.currency), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                                }
                            }
                        }

                        // ── Totals ───────────────────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TotalRow("Mahsulotlar summasi", formatMoney(d.subtotal, d.currency))
                            TotalRow("Yetkazib berish", formatMoney(d.deliveryFee, d.currency))
                            if (d.discountAmount.toDoubleOrNull()?.let { it > 0 } == true) {
                                TotalRow("Chegirma", "-${formatMoney(d.discountAmount, d.currency)}")
                            }
                            TotalRow("Jami", formatMoney(d.totalAmount, d.currency), bold = true)
                        }

                        // ── Admin Note ───────────────────────────────────────────
                        Text("Admin izohi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                        OutlinedTextField(
                            value = adminNoteText,
                            onValueChange = { adminNoteText = it },
                            placeholder = { Text("Admin izohi...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                            maxLines = 4,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { saveAdminNote() },
                                enabled = !savingNote && adminNoteText != d.adminNote,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            ) {
                                Text(if (savingNote) "Saqlanmoqda..." else "Izohni saqlash")
                            }
                        }

                        // ── Status update ────────────────────────────────────────
                        Text("Holatni o'zgartirish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                            OutlinedTextField(
                                value = orderStatusLabel(newStatus),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            )
                            ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                ORDER_STATUS_OPTIONS.forEach { opt ->
                                    DropdownMenuItem(text = { Text(orderStatusLabel(opt)) }, onClick = { newStatus = opt; statusExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            placeholder = { Text("Izoh (ixtiyoriy)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        )
                        if (saveError != null) {
                            Text(saveError ?: "", color = MexaWarehouseColors.danger, fontSize = 12.sp)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = onDismiss) { Text("Yopish") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { saveStatus() },
                                enabled = !saving && newStatus != d.status,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            ) {
                                if (saving) {
                                    CircularProgressIndicator(modifier = Modifier.height(16.dp), color = Color.White)
                                } else {
                                    Text("Saqlash")
                                }
                            }
                        }

                        // ── Action buttons ─────────────────────────────────────────
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (d.status == "DELIVERED") {
                                Button(
                                    onClick = { refundOrder() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                ) {
                                    Text("Qaytarish (Refund)")
                                }
                            }
                            if (d.status == "CANCELLED" || d.status == "REFUNDED") {
                                Button(
                                    onClick = { deleteOrder() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                                ) {
                                    Text("O'chirish")
                                }
                            }
                            OutlinedButton(onClick = { toggleHistory() }) {
                                Text(if (showHistory) "Tarixni yashirish" else "Tarixni ko'rish")
                            }
                        }

                        // ── Status History ─────────────────────────────────────────
                        if (showHistory) {
                            Text("Holatlar tarixi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            if (loadingHistory) {
                                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent, modifier = Modifier.height(20.dp))
                            } else if (history.isEmpty()) {
                                Text("Tarix topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            } else {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    history.forEach { h ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                val from = if (h.fromStatus.isNullOrBlank()) "—" else orderStatusLabel(h.fromStatus)
                                                val to = orderStatusLabel(h.toStatus)
                                                Text("$from → $to", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                                if (h.reason.isNotBlank()) {
                                                    Text(h.reason, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                                }
                                            }
                                            Text(formatHistoryDate(h.createdAt), fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        Text(value, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (bold) 14.sp else 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (bold) MexaWarehouseColors.textPrimary else MexaWarehouseColors.textMuted)
        Text(value, fontSize = if (bold) 14.sp else 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
    }
}

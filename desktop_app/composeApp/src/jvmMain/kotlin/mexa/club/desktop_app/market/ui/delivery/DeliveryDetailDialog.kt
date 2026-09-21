package mexa.club.desktop_app.market.ui.delivery

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import mexa.club.desktop_app.market.ui.orders.formatMoney
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TrackingEvent(
    val status: String,
    val location: String,
    val description: String,
    val createdAt: String,
)

private data class DeliveryDetail(
    val id: String,
    val orderId: String,
    val courierId: String,
    val status: String,
    val deliveryAddress: String,
    val recipientName: String,
    val recipientPhone: String,
    val region: String,
    val district: String,
    val trackingCode: String,
    val deliveryFee: String,
    val estimatedDelivery: String,
    val actualDelivery: String,
    val note: String,
    val failReason: String,
    val createdAt: String,
    val events: List<TrackingEvent>,
)

@Composable
fun DeliveryDetailDialog(
    deliveryId: String,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
    onAssign: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<DeliveryDetail?>(null) }
    var editNote by remember { mutableStateOf("") }
    var savingNote by remember { mutableStateOf(false) }
    var savingAction by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    suspend fun fetchDetail(): DeliveryDetail? = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/deliveries/$deliveryId")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) } ?: return@withContext null
        val eventsArr = data["events"] as? JsonArray ?: JsonArray(emptyList())
        DeliveryDetail(
            id = data.stringField("id"),
            orderId = data.stringField("orderId"),
            courierId = data.stringField("courierId"),
            status = data.stringField("status").ifEmpty { "PENDING" },
            deliveryAddress = data.stringField("deliveryAddress"),
            recipientName = data.stringField("recipientName"),
            recipientPhone = data.stringField("recipientPhone"),
            region = data.stringField("region"),
            district = data.stringField("district"),
            trackingCode = data.stringField("trackingCode"),
            deliveryFee = data.stringField("deliveryFee").ifEmpty { "0" },
            estimatedDelivery = data.stringField("estimatedDelivery"),
            actualDelivery = data.stringField("actualDelivery"),
            note = data.stringField("note"),
            failReason = data.stringField("failReason"),
            createdAt = data.stringField("createdAt"),
            events = eventsArr.mapNotNull { el ->
                val e = el as? JsonObject ?: return@mapNotNull null
                TrackingEvent(
                    status = e.stringField("status"),
                    location = e.stringField("location"),
                    description = e.stringField("description"),
                    createdAt = e.stringField("createdAt"),
                )
            },
        )
    }

    LaunchedEffect(deliveryId) {
        loading = true
        runCatching { fetchDetail() }
            .onSuccess { detail = it; editNote = it?.note ?: "" }
            .onFailure { error = it.message ?: "Xatolik" }
        loading = false
    }

    fun saveNote() {
        scope.launch {
            savingNote = true
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.put("/api/admin/deliveries/$deliveryId/note", "{\"note\":\"${editNote.replace("\"", "\\\"")}\"}")
                }
            }
            savingNote = false
        }
    }

    fun returnDelivery() {
        scope.launch {
            savingAction = true; actionError = null
            val body = if (editNote.isNotBlank()) "{\"note\":\"${editNote.replace("\"", "\\\"")}\"}" else "{}"
            runCatching {
                withContext(Dispatchers.IO) { ApiClient.post("/api/admin/deliveries/$deliveryId/return", body) }
            }.onSuccess { onUpdated() }
                .onFailure { actionError = it.message ?: "Xatolik" }
            savingAction = false
        }
    }

    fun formatDate(iso: String): String {
        if (iso.isBlank()) return "—"
        return runCatching {
            val parsed = if (iso.contains("T") && !iso.contains("+") && !iso.endsWith("Z"))
                java.time.LocalDateTime.parse(iso).atZone(ZoneId.systemDefault())
            else OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
            parsed.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH))
        }.getOrElse { iso }
    }

    fun formatDateShort(iso: String): String {
        if (iso.isBlank()) return "—"
        return runCatching {
            java.time.LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
        }.getOrElse { iso.take(10) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 520.dp, max = 620.dp).heightIn(max = 720.dp),
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
                        Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Header
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(d.trackingCode.ifBlank { "Yetkazma" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                                Text("${d.recipientName} • ${d.recipientPhone}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val (bg, fg) = deliveryStatusColors(d.status)
                                Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(deliveryStatusLabel(d.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                                }
                                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                                }
                            }
                        }

                        // Info
                        Column(Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoRow("Buyurtma ID", d.orderId.take(8).ifBlank { "—" })
                            InfoRow("Manzil", d.deliveryAddress.ifBlank { "—" })
                            InfoRow("Hudud", "${d.region}, ${d.district}".trimEnd(',', ' ').ifBlank { "—" })
                            InfoRow("Yetkazib berish", formatDateShort(d.estimatedDelivery))
                            if (d.actualDelivery.isNotBlank()) InfoRow("Yetkazilgan", formatDate(d.actualDelivery))
                            if (d.failReason.isNotBlank()) InfoRow("Sabab", d.failReason)
                            InfoRow("Summa", formatMoney(d.deliveryFee))
                        }

                        // Note edit
                        Text("Admin izohi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            placeholder = { Text("Izoh...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                            maxLines = 4,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { saveNote() },
                                enabled = !savingNote && editNote != d.note,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            ) { Text(if (savingNote) "Saqlanmoqda..." else "Izohni saqlash") }
                        }

                        // Actions
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onAssign) {
                                Text("Kuryer tayinlash")
                            }
                            if (d.status == "FAILED") {
                                Button(
                                    onClick = { returnDelivery() },
                                    enabled = !savingAction,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                ) { Text("Qaytarish (Return)") }
                            }
                        }

                        actionError?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 12.sp) }

                        // Tracking timeline
                        if (d.events.isNotEmpty()) {
                            Text("Kuzatuv tarixi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                d.events.forEachIndexed { idx, ev ->
                                    val isLast = idx == d.events.lastIndex
                                    Row(Modifier.fillMaxWidth()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                            Box(
                                                Modifier.size(10.dp).background(
                                                    if (isLast) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.outline,
                                                    RoundedCornerShape(5.dp),
                                                )
                                            )
                                            if (!isLast) Box(Modifier.width(2.dp).height(30.dp).background(MexaWarehouseColors.outline.copy(alpha = 0.3f)))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 16.dp)) {
                                            Text(deliveryStatusLabel(ev.status), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                            if (ev.description.isNotBlank()) {
                                                Text(ev.description, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                            }
                                            Text(formatDate(ev.createdAt), fontSize = 11.sp, color = MexaWarehouseColors.outline)
                                        }
                                    }
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = onDismiss) { Text("Yopish") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onUpdated,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            ) { Text("Yangilash") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        Text(value, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
    }
}

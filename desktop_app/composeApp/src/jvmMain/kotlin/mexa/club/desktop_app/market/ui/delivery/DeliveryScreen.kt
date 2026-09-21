package mexa.club.desktop_app.market.ui.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

data class DeliveryRow(
    val id: String,
    val orderId: String,
    val courierId: String,
    val status: String,
    val deliveryAddress: String,
    val recipientName: String,
    val recipientPhone: String,
    val region: String,
    val trackingCode: String,
    val deliveryFee: String,
    val createdAt: String,
)

val DELIVERY_STATUS_OPTIONS = listOf("PENDING", "ASSIGNED", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED")
private val ALL_FILTER = "ALL"

fun deliveryStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Kutilmoqda"
    "ASSIGNED" -> "Kuryer tayinlandi"
    "PICKED_UP" -> "Olib ketildi"
    "IN_TRANSIT" -> "Yo'lda"
    "DELIVERED" -> "Yetkazildi"
    "FAILED" -> "Muvaffaqiyatsiz"
    "RETURNED" -> "Qaytarildi"
    else -> status
}

fun deliveryStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "DELIVERED" -> MexaWarehouseColors.statusActiveBg to MexaWarehouseColors.statusActiveFg
    "ASSIGNED", "PICKED_UP", "IN_TRANSIT" -> MexaWarehouseColors.infoBadgeBg to MexaWarehouseColors.indigoAccent
    "PENDING" -> MexaWarehouseColors.amberBadgeBg to Color(0xFFB45309)
    "FAILED", "RETURNED" -> MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
    else -> MexaWarehouseColors.borderSubtle to MexaWarehouseColors.textMuted
}

fun formatDeliveryAmount(raw: String): String {
    val dec = raw.toBigDecimalOrNull() ?: return "$raw UZS"
    val plain = dec.stripTrailingZeros().toPlainString()
    val parts = plain.split('.')
    val whole = parts[0].let { w ->
        val sign = if (w.startsWith("-")) "-" else ""
        val d = w.removePrefix("-")
        sign + d.reversed().chunked(3).joinToString(" ").reversed()
    }
    return "$whole UZS"
}

fun formatDeliveryDate(iso: String): String {
    val s = iso.trim()
    if (s.isEmpty()) return "—"
    return runCatching {
        val parsed = if (s.contains("T") && !s.contains("+") && !s.endsWith("Z"))
            java.time.LocalDateTime.parse(s)
        else
            java.time.OffsetDateTime.parse(s).toLocalDateTime()
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", java.util.Locale.ENGLISH))
    }.getOrElse { s.take(10) }
}

private fun String.toBigDecimalOrNull(): java.math.BigDecimal? = runCatching { java.math.BigDecimal(this) }.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deliveries by remember { mutableStateOf<List<DeliveryRow>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(ALL_FILTER) }
    var statusExpanded by remember { mutableStateOf(false) }
    var courierFilter by remember { mutableStateOf(ALL_FILTER) }
    var courierExpanded by remember { mutableStateOf(false) }
    var courierList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var detailTarget by remember { mutableStateOf<DeliveryRow?>(null) }
    var assignTarget by remember { mutableStateOf<DeliveryRow?>(null) }
    var showZones by remember { mutableStateOf(false) }

    suspend fun fetchDeliveries(): List<DeliveryRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/deliveries?page=0&size=200")
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val d = el as? JsonObject ?: return@mapNotNull null
            val id = d.stringField("id").ifEmpty { return@mapNotNull null }
            DeliveryRow(
                id = id,
                orderId = d.stringField("orderId"),
                courierId = d.stringField("courierId"),
                status = d.stringField("status").ifEmpty { "PENDING" },
                deliveryAddress = d.stringField("deliveryAddress"),
                recipientName = d.stringField("recipientName"),
                recipientPhone = d.stringField("recipientPhone"),
                region = d.stringField("region"),
                trackingCode = d.stringField("trackingCode"),
                deliveryFee = d.stringField("deliveryFee").ifEmpty { "0" },
                createdAt = d.stringField("createdAt"),
            )
        }.sortedByDescending { it.createdAt }
    }

    suspend fun fetchCouriers(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/couriers")
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val id = c.stringField("id").ifEmpty { return@mapNotNull null }
            val name = c.stringField("name").ifBlank { id.take(8) }
            id to name
        }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchDeliveries() }
                .onSuccess { deliveries = it }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(Unit) {
        runCatching { fetchCouriers() }
            .onSuccess { courierList = it }
    }

    detailTarget?.let { target ->
        DeliveryDetailDialog(
            deliveryId = target.id,
            onDismiss = { detailTarget = null },
            onUpdated = { detailTarget = null; reload() },
            onAssign = { detailTarget = null; assignTarget = target },
        )
    }

    assignTarget?.let { target ->
        AssignCourierDialog(
            delivery = target,
            onDismiss = { assignTarget = null },
            onAssigned = { assignTarget = null; reload() },
        )
    }

    if (showZones) {
        DeliveryZonesDialog(onDismiss = { showZones = false })
    }

    val courierIdMap = courierList.toMap()
    val filtered = deliveries.filter { d ->
        val matchStatus = statusFilter == ALL_FILTER || d.status.equals(statusFilter, ignoreCase = true)
        val matchCourier = courierFilter == ALL_FILTER || d.courierId.equals(courierFilter, ignoreCase = true)
        val matchSearch = searchQuery.isBlank() ||
            d.recipientName.contains(searchQuery, ignoreCase = true) ||
            d.recipientPhone.contains(searchQuery, ignoreCase = true) ||
            d.trackingCode.contains(searchQuery, ignoreCase = true) ||
            d.deliveryAddress.contains(searchQuery, ignoreCase = true)
        matchStatus && matchCourier && matchSearch
    }

    val totalCount = deliveries.size
    val pendingCount = deliveries.count { it.status.equals("PENDING", ignoreCase = true) }
    val inTransitCount = deliveries.count { it.status in setOf("ASSIGNED", "PICKED_UP", "IN_TRANSIT") }
    val deliveredCount = deliveries.count { it.status.equals("DELIVERED", ignoreCase = true) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Yetkazib berish", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Buyurtmalarni yetkazib berish holati", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
            OutlinedButton(
                onClick = { showZones = true },
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Zonalar", fontSize = 13.sp)
            }
        }

        if (!loading) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth < 700.dp
                val gap = 16.dp
                val cards = listOf<@Composable (Modifier) -> Unit>(
                    { m -> DeliveryStatCard("Jami yetkazmalar", totalCount.toString(), Icons.Default.LocalShipping, MexaWarehouseColors.indigoAccent, MexaWarehouseColors.statIndigoBg, m) },
                    { m -> DeliveryStatCard("Kutilmoqda", pendingCount.toString(), Icons.Default.PendingActions, Color(0xFFB45309), MexaWarehouseColors.amberBadgeBg, m) },
                    { m -> DeliveryStatCard("Yo'lda", inTransitCount.toString(), Icons.Default.LocalShipping, Color(0xFF0369A1), Color(0xFFE0F2FE), m) },
                    { m -> DeliveryStatCard("Yetkazildi", deliveredCount.toString(), Icons.Default.CheckCircle, MexaWarehouseColors.statusActiveFg, MexaWarehouseColors.statusActiveBg, m) },
                )
                if (twoColumns) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        cards.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                row.forEach { it(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        cards.forEach { it(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Qabul qiluvchi, telefon, manzil yoki kod...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }, modifier = Modifier.widthIn(max = 180.dp)) {
                OutlinedTextField(
                    value = if (statusFilter == ALL_FILTER) "Barcha holatlar" else deliveryStatusLabel(statusFilter),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    DropdownMenuItem(text = { Text("Barcha holatlar") }, onClick = { statusFilter = ALL_FILTER; statusExpanded = false })
                    DELIVERY_STATUS_OPTIONS.forEach { opt ->
                        DropdownMenuItem(text = { Text(deliveryStatusLabel(opt)) }, onClick = { statusFilter = opt; statusExpanded = false })
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = courierExpanded, onExpandedChange = { courierExpanded = it }, modifier = Modifier.widthIn(max = 180.dp)) {
                OutlinedTextField(
                    value = if (courierFilter == ALL_FILTER) "Barcha kuryerlar"
                            else courierIdMap[courierFilter] ?: "Kuryer",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courierExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = courierExpanded, onDismissRequest = { courierExpanded = false }) {
                    DropdownMenuItem(text = { Text("Barcha kuryerlar") }, onClick = { courierFilter = ALL_FILTER; courierExpanded = false })
                    courierList.forEach { (id, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { courierFilter = id; courierExpanded = false })
                    }
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val bytes = ApiClient.getBytes("/api/admin/deliveries/export")
                                val file = java.io.File(System.getProperty("user.home") + "/Downloads/deliveries_${java.time.LocalDate.now()}.csv")
                                file.writeBytes(bytes)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .size(40.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MexaWarehouseColors.indigoAccent)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
            error != null -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "Xatolik", color = MexaWarehouseColors.danger)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload() }) { Text("Qayta urinish") }
                }
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Yetkazmalar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> DeliveriesTable(filtered, onRowClick = { detailTarget = it })
        }
    }
}

@Composable
private fun DeliveriesTable(deliveries: List<DeliveryRow>, onRowClick: (DeliveryRow) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Kod" to 0.9f, "Qabul qiluvchi" to 1.3f, "Manzil" to 1.5f, "Hudud" to 0.8f, "Summa" to 0.8f, "Holat" to 1f, "Sana" to 0.9f).forEach { (h, w) ->
                Text(h.uppercase(), modifier = Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        deliveries.forEachIndexed { idx, d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .clickable { onRowClick(d) }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(d.trackingCode.ifBlank { "—" }, modifier = Modifier.weight(0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Column(Modifier.weight(1.3f)) {
                    Text(d.recipientName.ifBlank { "—" }, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                    Text(d.recipientPhone, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                }
                Text(d.deliveryAddress.ifBlank { "—" }, modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(d.region.ifBlank { "—" }, modifier = Modifier.weight(0.8f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(formatDeliveryAmount(d.deliveryFee), modifier = Modifier.weight(0.8f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                Box(Modifier.weight(1f)) {
                    val (bg, fg) = deliveryStatusColors(d.status)
                    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(deliveryStatusLabel(d.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
                Text(formatDeliveryDate(d.createdAt), modifier = Modifier.weight(0.9f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun DeliveryStatCard(label: String, value: String, icon: ImageVector, iconTint: Color, iconBg: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textMuted)
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, color = MexaWarehouseColors.textPrimary)
        }
        Box(Modifier.size(44.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
    }
}

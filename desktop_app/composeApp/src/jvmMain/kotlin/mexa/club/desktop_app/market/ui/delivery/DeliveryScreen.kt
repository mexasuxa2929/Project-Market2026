package mexa.club.desktop_app.market.ui.delivery

import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
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
import androidx.compose.material3.HorizontalDivider
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.components.PaginationBar
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

val DELIVERY_STATUS_OPTIONS = listOf("PENDING", "ASSIGNED", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "FAILED", "RETURNED", "CANCELLED")
private val ALL_FILTER = "ALL"

fun deliveryStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Kutilmoqda"
    "ASSIGNED" -> "Kuryer tayinlandi"
    "PICKED_UP" -> "Olib ketildi"
    "IN_TRANSIT" -> "Yo'lda"
    "DELIVERED" -> "Yetkazildi"
    "FAILED" -> "Muvaffaqiyatsiz"
    "RETURNED" -> "Qaytarildi"
    "CANCELLED" -> "Bekor qilingan"
    else -> status
}

fun deliveryStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "DELIVERED" -> MexaWarehouseColors.statusActiveBg to MexaWarehouseColors.statusActiveFg
    "ASSIGNED", "PICKED_UP", "IN_TRANSIT" -> MexaWarehouseColors.infoBadgeBg to MexaWarehouseColors.indigoAccent
    "PENDING" -> MexaWarehouseColors.amberBadgeBg to Color(0xFFB45309)
    "FAILED", "RETURNED", "CANCELLED" -> MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
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
    // O-46: server paginatsiya (200 dan keyin jim kesilmasligi uchun)
    var page by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(20) }
    var totalElements by remember { mutableStateOf(0L) }
    var assignTarget by remember { mutableStateOf<DeliveryRow?>(null) }
    var showZones by remember { mutableStateOf(false) }

    suspend fun fetchDeliveries(): Pair<List<DeliveryRow>, Long> = withContext(Dispatchers.IO) {
        // O-46: serverga filter+pagination uzatiladi (client-side 200-cut emas)
        val params = buildList {
            add("page=$page")
            add("size=$pageSize")
            if (statusFilter != ALL_FILTER) add("status=$statusFilter")
            if (courierFilter != ALL_FILTER) add("courierId=$courierFilter")
            if (searchQuery.isNotBlank()) {
                add("search=${java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8")}")
            }
        }
        val text = ApiClient.get("/api/admin/deliveries?${params.joinToString("&")}")
        // O-46: boshqa ekranlardagi naqsh — dataObjectOrSelf + itemsOrContentArray
        // (server Page payload qaytarsa ham, eski array qaytarsa ham ishlaydi)
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObjectOrSelf(it) }
        val arr = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        val rows = arr.mapNotNull { el ->
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
        val total = (data?.get("totalElements") as? JsonPrimitive)?.content?.toLongOrNull()
            ?: rows.size.toLong()
        rows to total
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
                .onSuccess { (rows, total) ->
                    totalElements = total
                    val totalPages = if (total == 0L) 1 else ((total + pageSize - 1) / pageSize).toInt()
                    if (page > totalPages - 1) {
                        page = totalPages - 1 // effekt qayta ishga tushadi
                    } else {
                        deliveries = rows
                    }
                }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    // O-46: filtr/qidiruv/sahifa o'zgarishi → 300ms debounce bilan server so'rovi
    LaunchedEffect(searchQuery, statusFilter, courierFilter, page, pageSize) {
        delay(300)
        reload()
    }

    LaunchedEffect(Unit) {
        runCatching { fetchCouriers() }
            .onSuccess { courierList = it }
    }

    var exportMsg by remember { mutableStateOf<String?>(null) }

    // M-40: delivery realtime hodisalari → qayta yuklash
    LaunchedEffect(Unit) {
        mexa.club.desktop_app.market.api.GatewayRealtimeHub.events
            .filter { it.contains("/deliveries") }
            .collect { reload() }
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
    // O-46: filtr/qidiruv serverda (yuqoridagi params) — client filter yo'q

    val totalCount = totalElements.toInt()
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
                    DropdownMenuItem(text = { Text(AppStrings.t(appLanguage(), "Barcha holatlar")) }, onClick = { statusFilter = ALL_FILTER; statusExpanded = false })
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
                        }.onSuccess { exportMsg = "Saqlandi" }
                            .onFailure { exportMsg = "Export xatosi: " + (it.message ?: it.toString()) }
                    }
                },
                modifier = Modifier
                    .background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .size(40.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = MexaWarehouseColors.indigoAccent)
            }
            exportMsg?.let { msg ->
                Text(msg, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
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
                    Button(onClick = { reload() }) { Text(AppStrings.t(appLanguage(), "Qayta urinish")) }
                }
            }
            deliveries.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Yetkazmalar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> DeliveriesTable(deliveries, onRowClick = { detailTarget = it })
        }

        // O-46: server pagination (OrdersScreen andozasi)
        if (!loading && error == null && deliveries.isNotEmpty()) {
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            PaginationBar(
                page = page,
                pageSize = pageSize,
                totalElements = totalElements.toInt(),
                onPageChange = { page = it },
                onPageSizeChange = { pageSize = it; page = 0 },
            )
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

package mexa.club.desktop_app.market.ui.orders

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Models ────────────────────────────────────────────────────────────────────

data class OrderRow(
    val id: String,
    val orderNumber: String,
    val userId: String,
    val status: String,
    val paymentStatus: String,
    val totalAmount: String,
    val currency: String,
    val createdAt: String,
    val deliveryAddress: String,
    val itemsCount: Int,
)

data class OrderStatsData(
    val todayTotal: Long,
    val todayRevenue: String,
    val monthTotal: Long,
    val pendingCount: Long,
)

val ORDER_STATUS_OPTIONS = listOf(
    "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED",
)

fun orderStatusLabel(status: String): String = when (status.uppercase(Locale.getDefault())) {
    "PENDING" -> "Kutilmoqda"
    "CONFIRMED" -> "Tasdiqlandi"
    "PROCESSING" -> "Jarayonda"
    "SHIPPED" -> "Yo'lda"
    "DELIVERED" -> "Yetkazildi"
    "CANCELLED" -> "Bekor qilindi"
    "REFUNDED" -> "Qaytarildi"
    else -> status
}

fun orderStatusColors(status: String): Pair<Color, Color> = when (status.uppercase(Locale.getDefault())) {
    "DELIVERED" -> MexaWarehouseColors.statusActiveBg to MexaWarehouseColors.statusActiveFg
    "CONFIRMED", "PROCESSING", "SHIPPED" -> MexaWarehouseColors.infoBadgeBg to MexaWarehouseColors.indigoAccent
    "PENDING" -> MexaWarehouseColors.amberBadgeBg to Color(0xFFB45309)
    "CANCELLED", "REFUNDED" -> MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
    else -> MexaWarehouseColors.borderSubtle to MexaWarehouseColors.textMuted
}

fun formatMoney(raw: String, currency: String = "UZS"): String {
    val dec = raw.toBigDecimalOrNull() ?: return "$raw $currency"
    val plain = dec.stripTrailingZeros().toPlainString()
    val parts = plain.split('.')
    val whole = parts[0].let { w ->
        val sign = if (w.startsWith("-")) "-" else ""
        val d = w.removePrefix("-")
        sign + d.reversed().chunked(3).joinToString(" ").reversed()
    }
    val frac = parts.getOrNull(1)
    val number = if (frac != null && frac.isNotEmpty()) "$whole.$frac" else whole
    return "$number $currency"
}

fun formatOrderDate(iso: String): String {
    val s = iso.trim()
    if (s.isEmpty()) return "—"
    return runCatching {
        val parsed = if (s.contains("T") && !s.contains("+") && !s.endsWith("Z")) {
            java.time.LocalDateTime.parse(s).atZone(ZoneId.systemDefault())
        } else {
            OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault())
        }
        parsed.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH))
    }.getOrElse { s }
}

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var orders by remember { mutableStateOf<List<OrderRow>>(emptyList()) }
    var stats by remember { mutableStateOf<OrderStatsData?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Barcha holatlar") }
    var statusExpanded by remember { mutableStateOf(false) }
    var paymentFilter by remember { mutableStateOf("Barcha to'lovlar") }
    var paymentExpanded by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<OrderRow?>(null) }

    suspend fun fetchOrders(): List<OrderRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/orders?page=0&size=100")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
        val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        items.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val id = o.stringField("id").ifEmpty { return@mapNotNull null }
            val itemsArr = o["items"] as? JsonArray
            OrderRow(
                id = id,
                orderNumber = o.stringField("orderNumber").ifEmpty { id.take(8) },
                userId = o.stringField("userId"),
                status = o.stringField("status").ifEmpty { "PENDING" },
                paymentStatus = o.stringField("paymentStatus").ifEmpty { "UNPAID" },
                totalAmount = o.stringField("totalAmount").ifEmpty { "0" },
                currency = o.stringField("currency").ifEmpty { "UZS" },
                createdAt = o.stringField("createdAt"),
                deliveryAddress = o.stringField("deliveryAddress"),
                itemsCount = itemsArr?.size ?: 0,
            )
        }.sortedByDescending { it.createdAt }
    }

    suspend fun fetchStats(): OrderStatsData? = withContext(Dispatchers.IO) {
        runCatching {
            val text = ApiClient.get("/api/admin/orders/stats")
            val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) } ?: return@runCatching null
            val today = data["today"] as? JsonObject
            val month = data["thisMonth"] as? JsonObject
            OrderStatsData(
                todayTotal = today?.stringField("total")?.toLongOrNull() ?: 0L,
                todayRevenue = today?.stringField("revenue")?.ifEmpty { "0" } ?: "0",
                monthTotal = month?.stringField("total")?.toLongOrNull() ?: 0L,
                pendingCount = (data["pendingCount"] as? JsonPrimitive)?.content?.toLongOrNull()
                    ?: (data["byStatus"] as? JsonObject)?.get("PENDING")?.let { (it as? JsonPrimitive)?.content?.toLongOrNull() }
                    ?: 0L,
            )
        }.getOrNull()
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchOrders() }
                .onSuccess { orders = it }
                .onFailure { error = it.message ?: "Xatolik" }
            stats = fetchStats()
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    selectedOrder?.let { sel ->
        OrderDetailDialog(
            orderId = sel.id,
            onDismiss = { selectedOrder = null },
            onUpdated = {
                selectedOrder = null
                reload()
            },
        )
    }

    val statusOptions = listOf("Barcha holatlar") + ORDER_STATUS_OPTIONS
    val paymentOptions = listOf("Barcha to'lovlar", "PAID", "UNPAID", "REFUNDED")
    val filtered = orders.filter { o ->
        val matchSearch = searchQuery.isBlank() ||
            o.orderNumber.contains(searchQuery, ignoreCase = true) ||
            o.userId.contains(searchQuery, ignoreCase = true)
        val matchStatus = statusFilter == "Barcha holatlar" || o.status.equals(statusFilter, ignoreCase = true)
        val matchPayment = paymentFilter == "Barcha to'lovlar" || o.paymentStatus.equals(paymentFilter, ignoreCase = true)
        matchSearch && matchStatus && matchPayment
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Buyurtmalar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Barcha buyurtmalarni ko'rish va boshqarish", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        // ── Stat cards ────────────────────────────────────────────────────────
        if (!loading) {
            val s = stats
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth < 700.dp
                val gap = 16.dp
                val cards = listOf<@Composable (Modifier) -> Unit>(
                    { m -> OrderStatCard("Bugungi buyurtmalar", (s?.todayTotal ?: 0L).toString(), Icons.Default.ShoppingCart, MexaWarehouseColors.indigoAccent, MexaWarehouseColors.statIndigoBg, m) },
                    { m -> OrderStatCard("Bugungi daromad", formatMoney(s?.todayRevenue ?: "0"), Icons.Default.AttachMoney, MexaWarehouseColors.statusActiveFg, MexaWarehouseColors.statusActiveBg, m) },
                    { m -> OrderStatCard("Shu oy buyurtmalar", (s?.monthTotal ?: 0L).toString(), Icons.Default.CalendarMonth, Color(0xFF0369A1), Color(0xFFE0F2FE), m) },
                    { m -> OrderStatCard("Kutilayotgan", (s?.pendingCount ?: 0L).toString(), Icons.Default.HourglassEmpty, Color(0xFFD97706), MexaWarehouseColors.amberBadgeBg, m) },
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

        // ── Filter bar ────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buyurtma raqami yoki foydalanuvchi ID...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(
                    value = if (statusFilter == "Barcha holatlar") statusFilter else orderStatusLabel(statusFilter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Holat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().widthIn(min = 140.dp, max = 180.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(if (opt == "Barcha holatlar") opt else orderStatusLabel(opt)) },
                            onClick = { statusFilter = opt; statusExpanded = false },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = paymentExpanded, onExpandedChange = { paymentExpanded = it }) {
                OutlinedTextField(
                    value = when (paymentFilter) {
                        "Barcha to'lovlar" -> "Barcha to'lovlar"
                        "PAID" -> "To'langan"
                        "UNPAID" -> "To'lanmagan"
                        "REFUNDED" -> "Qaytarilgan"
                        else -> paymentFilter
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To'lov") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                    modifier = Modifier.menuAnchor().widthIn(min = 140.dp, max = 180.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                    paymentOptions.forEach { opt ->
                        val label = when (opt) {
                            "Barcha to'lovlar" -> opt
                            "PAID" -> "To'langan"
                            "UNPAID" -> "To'lanmagan"
                            "REFUNDED" -> "Qaytarilgan"
                            else -> opt
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { paymentFilter = opt; paymentExpanded = false },
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val bytes = ApiClient.getBytes("/api/admin/orders/export")
                                val file = java.io.File(System.getProperty("user.home") + "/Downloads/orders_${java.time.LocalDate.now()}.csv")
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

        // ── Content ───────────────────────────────────────────────────────────
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
                Text("Buyurtmalar topilmadi", color = MexaWarehouseColors.textMuted)
            }

            else -> OrdersTable(orders = filtered, onRowClick = { selectedOrder = it })
        }
    }
}

// ─── Table ────────────────────────────────────────────────────────────────────

@Composable
private fun OrdersTable(orders: List<OrderRow>, onRowClick: (OrderRow) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MexaWarehouseColors.tableHeaderBg)
                .padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Buyurtma" to 1.6f, "Mijoz" to 1.6f, "Mahsulot" to 0.8f, "Summa" to 1.1f, "To'lov" to 1f, "Holat" to 1.1f, "Sana" to 1.2f).forEach { (h, w) ->
                Text(
                    h.uppercase(Locale.getDefault()),
                    modifier = Modifier.weight(w),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = MexaWarehouseColors.outline,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        orders.forEachIndexed { idx, o ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .clickable { onRowClick(o) }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#${o.orderNumber}", modifier = Modifier.weight(1.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Text(
                    o.userId.take(8).ifBlank { "—" },
                    modifier = Modifier.weight(1.6f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                Text(o.itemsCount.toString(), modifier = Modifier.weight(0.8f), fontSize = 14.sp, color = MexaWarehouseColors.textPrimary)
                Text(formatMoney(o.totalAmount, o.currency), modifier = Modifier.weight(1.1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Box(Modifier.weight(1f)) { PaymentStatusPill(o.paymentStatus) }
                Box(Modifier.weight(1.1f)) { OrderStatusBadge(o.status) }
                Text(formatOrderDate(o.createdAt), modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (bg, fg) = orderStatusColors(status)
    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(orderStatusLabel(status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun PaymentStatusPill(status: String) {
    val (bg, fg, label) = when (status.uppercase(Locale.getDefault())) {
        "PAID" -> Triple(MexaWarehouseColors.greenBadgeBg, MexaWarehouseColors.greenBadgeFg, "To'langan")
        "REFUNDED" -> Triple(MexaWarehouseColors.blueBadgeBg, MexaWarehouseColors.blueBadgeFg, "Qaytarilgan")
        else -> Triple(MexaWarehouseColors.amberBadgeBg, MexaWarehouseColors.amberBadgeFg, "To'lanmagan")
    }
    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ─── Stat card ────────────────────────────────────────────────────────────────

@Composable
private fun OrderStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textMuted)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, color = MexaWarehouseColors.textPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(44.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

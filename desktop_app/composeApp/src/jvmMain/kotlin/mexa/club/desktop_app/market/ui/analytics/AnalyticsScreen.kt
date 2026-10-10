package mexa.club.desktop_app.market.ui.analytics

import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.util.Locale
import kotlinx.coroutines.flow.collect

private const val K_SUMMARY = "summary"
private const val K_REVENUE = "revenue"
private const val K_ORDERS = "orders"
private const val K_PRODUCTS = "products"
private const val K_STOCK = "stock"
private const val K_USERS = "users"
private const val K_DISCOUNTS = "discounts"
private const val SECTION_COUNT = 7

// ─── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val lang = appLanguage()

    var loading by remember { mutableStateOf(true) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var summary by remember { mutableStateOf<JsonObject?>(null) }
    var revenue by remember { mutableStateOf<JsonObject?>(null) }
    var orders by remember { mutableStateOf<JsonObject?>(null) }
    var products by remember { mutableStateOf<JsonObject?>(null) }
    var stock by remember { mutableStateOf<JsonObject?>(null) }
    var users by remember { mutableStateOf<JsonObject?>(null) }
    var discounts by remember { mutableStateOf<JsonObject?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            errors = emptyMap()
            val errs = linkedMapOf<String, String>()

            suspend fun load(key: String, path: String, sink: (JsonObject) -> Unit) {
                runCatching {
                    val text = withContext(Dispatchers.IO) { ApiClient.get(path) }
                    sink(requireData(text))
                }.onFailure { e ->
                    errs[key] = e.message?.trim().orEmpty()
                }
            }

            load(K_SUMMARY, "/api/admin/analytics/summary?refresh=true") { summary = it }
            load(K_REVENUE, "/api/admin/analytics/revenue/chart?period=week") { revenue = it }
            load(K_ORDERS, "/api/admin/analytics/orders/breakdown?period=week") { orders = it }
            load(K_PRODUCTS, "/api/admin/analytics/products/top?period=month&limit=10") { products = it }
            load(K_STOCK, "/api/admin/analytics/stock/alerts?threshold=10") { stock = it }
            load(K_USERS, "/api/admin/analytics/users/growth?period=week") { users = it }
            load(K_DISCOUNTS, "/api/admin/analytics/discounts/summary") { discounts = it }

            errors = errs
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(Unit) {
        GatewayRealtimeHub.events.filter { it.contains("/analytics") }.collect { reload() }
    }

    val hasData = summary != null || revenue != null || orders != null ||
        products != null || stock != null || users != null || discounts != null
    val allFailed = !loading && errors.size >= SECTION_COUNT

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(AppStrings.t(lang, "Analitika"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text(
                    AppStrings.t(lang, "Savdo, buyurtma va foydalanuvchi ko'rsatkichlari"),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
            }
            IconButton(
                onClick = { reload() },
                enabled = !loading,
                modifier = Modifier
                    .background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .size(40.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = MexaWarehouseColors.indigoAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = AppStrings.t(lang, "Yangilash"),
                        tint = MexaWarehouseColors.indigoAccent,
                    )
                }
            }
        }

        when {
            loading && !hasData -> Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }

            allFailed -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(32.dp))
                    Text(AppStrings.t(lang, "Ma'lumot yuklanmadi"), color = MexaWarehouseColors.danger, fontSize = 14.sp)
                    Button(
                        onClick = { reload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(AppStrings.t(lang, "Qayta urinish")) }
                }
            }

            else -> {
                if (errors.isNotEmpty() && hasData) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MexaWarehouseColors.amberBadgeBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MexaWarehouseColors.amberBadgeFg, modifier = Modifier.size(18.dp))
                        Text(
                            "${AppStrings.t(lang, "Ba'zi bo'limlar yuklanmadi")}: ${errors.size}",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.amberBadgeFg,
                        )
                        TextButton(onClick = { reload() }) {
                            Text(AppStrings.t(lang, "Qayta urinish"), color = MexaWarehouseColors.indigoAccent, fontSize = 13.sp)
                        }
                    }
                }

                // ── 1. Summary ────────────────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Umumiy ko'rsatkichlar"),
                    subtitle = summary?.let {
                        "${AppStrings.t(lang, "Hisoblangan vaqt")}: ${it.stringField("computedAt").ifBlank { "—" }}"
                    },
                    error = errors[K_SUMMARY],
                    onRetry = { reload() },
                ) { SummaryContent(summary) }

                // ── 2. Revenue chart ──────────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Daromad grafigi"),
                    subtitle = revenue?.let { RevenueSubtitle(it) },
                    error = errors[K_REVENUE],
                    onRetry = { reload() },
                ) { PointBarList(revenue, isRevenue = true) }

                // ── 3. Orders breakdown ───────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Buyurtmalar taqsimoti"),
                    subtitle = orders?.let {
                        "${AppStrings.t(lang, "Davr")}: ${it.stringField("period").ifBlank { "—" }}" +
                            " · ${AppStrings.t(lang, "Konversiya")}: ${formatPercent(it.stringField("conversionRate"))}"
                    },
                    error = errors[K_ORDERS],
                    onRetry = { reload() },
                ) { OrdersBreakdownContent(orders) }

                // ── 4. Top products ───────────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Top mahsulotlar"),
                    subtitle = products?.let {
                        "${AppStrings.t(lang, "Davr")}: ${it.stringField("period").ifBlank { "—" }}"
                    },
                    error = errors[K_PRODUCTS],
                    onRetry = { reload() },
                ) { TopProductsContent(products) }

                // ── 5. Stock alerts ───────────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Zaxira ogohlantirishlari"),
                    subtitle = stock?.let {
                        "${AppStrings.t(lang, "Chegira")}: ${it.stringField("lowStockThreshold").ifBlank { "—" }}" +
                            " · ${AppStrings.t(lang, "Jami")}: ${fmtAmount(it.stringField("totalLowStock"))}"
                    },
                    error = errors[K_STOCK],
                    onRetry = { reload() },
                ) { StockAlertsContent(stock) }

                // ── 6. Users growth ───────────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Foydalanuvchilar o'sishi"),
                    subtitle = users?.let { UsersSubtitle(it) },
                    error = errors[K_USERS],
                    onRetry = { reload() },
                ) { PointBarList(users, isRevenue = false) }

                // ── 7. Discounts ──────────────────────────────────────────────
                SectionCard(
                    title = AppStrings.t(lang, "Chegirmalar"),
                    subtitle = null,
                    error = errors[K_DISCOUNTS],
                    onRetry = { reload() },
                ) { DiscountsContent(discounts) }
            }
        }
    }
}

// ─── Summary ───────────────────────────────────────────────────────────────────

@Composable
private fun SummaryContent(data: JsonObject?) {
    val lang = appLanguage()
    if (data == null) {
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    StatGrid(
        listOf<@Composable (Modifier) -> Unit>(
            { m ->
                StatCard(
                    AppStrings.t(lang, "Bugungi buyurtmalar"),
                    fmtAmount(data.stringField("ordersToday")),
                    Icons.Default.ShoppingCart,
                    MexaWarehouseColors.indigoAccent,
                    MexaWarehouseColors.statIndigoBg,
                    m,
                )
            },
            { m ->
                StatCard(
                    AppStrings.t(lang, "Bugungi daromad (so'm)"),
                    fmtAmount(data.stringField("revenueToday")),
                    Icons.Default.AttachMoney,
                    MexaWarehouseColors.statusActiveFg,
                    MexaWarehouseColors.statusActiveBg,
                    m,
                )
            },
            { m ->
                StatCard(
                    AppStrings.t(lang, "Jami foydalanuvchilar"),
                    fmtAmount(data.stringField("totalUsers")),
                    Icons.Default.People,
                    Color(0xFF1D4ED8),
                    MexaWarehouseColors.blueBadgeBg,
                    m,
                )
            },
            { m ->
                StatCard(
                    AppStrings.t(lang, "Kam qolgan mahsulotlar"),
                    fmtAmount(data.stringField("lowStockTotal")),
                    Icons.Default.Warning,
                    MexaWarehouseColors.amberBadgeFg,
                    MexaWarehouseColors.amberBadgeBg,
                    m,
                )
            },
        ),
    )

    Spacer(Modifier.height(12.dp))
    StatGrid(
        listOf<@Composable (Modifier) -> Unit>(
            { m -> MiniStat(AppStrings.t(lang, "Bugungi yangi foydalanuvchilar"), fmtAmount(data.stringField("newUsersToday")), m) },
            { m -> MiniStat(AppStrings.t(lang, "Tasdiqlangan foydalanuvchilar"), fmtAmount(data.stringField("verifiedUsers")), m) },
            { m -> MiniStat(AppStrings.t(lang, "Faol aksiyalar"), fmtAmount(data.stringField("activePromotions")), m) },
            { m -> MiniStat(AppStrings.t(lang, "Bugungi chegirma (so'm)"), fmtAmount(data.stringField("discountGivenToday")), m) },
        ),
    )
}

// ─── Subtitles (computed from response) ────────────────────────────────────────

@Composable
private fun RevenueSubtitle(data: JsonObject): String {
    val lang = appLanguage()
    val points = parsePoints(data, revenueField = true)
    val total = points.sumOf { it.num }
    val max = points.maxOfOrNull { it.num } ?: 0.0
    return "${AppStrings.t(lang, "Davr")}: ${data.stringField("period").ifBlank { "—" }}" +
        " · ${AppStrings.t(lang, "Jami")}: ${fmtAmount(total.toLong().toString())}" +
        " · ${AppStrings.t(lang, "Eng yuqori")}: ${fmtAmount(max.toLong().toString())}"
}

@Composable
private fun UsersSubtitle(data: JsonObject): String {
    val lang = appLanguage()
    val points = parsePoints(data, revenueField = false)
    val newTotal = points.sumOf { it.num }
    val arr = data["points"] as? JsonArray
    val lastTotal = (arr?.lastOrNull() as? JsonObject)?.stringField("totalUsers").orEmpty().ifBlank { "—" }
    return "${AppStrings.t(lang, "Davr")}: ${data.stringField("period").ifBlank { "—" }}" +
        " · ${AppStrings.t(lang, "Yangi foydalanuvchilar")}: ${fmtAmount(newTotal.toLong().toString())}" +
        " · ${AppStrings.t(lang, "Jami foydalanuvchilar")}: ${fmtAmount(lastTotal)}"
}

private data class ChartPoint(val label: String, val value: String, val num: Double)

private fun parsePoints(data: JsonObject, revenueField: Boolean): List<ChartPoint> {
    val arr = data["points"] as? JsonArray ?: return emptyList()
    return arr.mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        val raw = if (revenueField) o.stringField("revenue") else o.stringField("newUsers")
        if (raw.isBlank()) return@mapNotNull null
        ChartPoint(
            label = o.stringField("date").ifBlank { "—" },
            value = raw,
            num = raw.toDoubleOrNull() ?: 0.0,
        )
    }
}

// ─── Bar lists (revenue + user growth) ─────────────────────────────────────────

@Composable
private fun PointBarList(data: JsonObject?, isRevenue: Boolean) {
    val lang = appLanguage()
    val points = if (data == null) emptyList() else parsePoints(data, revenueField = isRevenue)
    if (points.isEmpty()) {
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    val max = (points.maxOfOrNull { it.num } ?: 0.0).takeIf { it > 0.0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        points.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    p.label,
                    modifier = Modifier.width(96.dp),
                    fontSize = 11.sp,
                    color = MexaWarehouseColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(MexaWarehouseColors.surfaceContainerLow, RoundedCornerShape(4.dp)),
                ) {
                    val fraction = (p.num / max).coerceIn(0.0, 1.0).toFloat()
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(14.dp)
                            .background(
                                if (isRevenue) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.statusActiveFg,
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
                Text(
                    fmtAmount(p.value),
                    modifier = Modifier.width(112.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Orders breakdown ──────────────────────────────────────────────────────────

@Composable
private fun OrdersBreakdownContent(data: JsonObject?) {
    val lang = appLanguage()
    val breakdown = data?.get("breakdown") as? JsonObject
    val entries = breakdown?.entries?.mapNotNull { (k, v) ->
        val n = (v as? JsonPrimitive)?.content?.toLongOrNull() ?: return@mapNotNull null
        k to n
    }?.sortedByDescending { it.second } ?: emptyList()

    if (entries.isEmpty()) {
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    val max = (entries.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEach { (label, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    label,
                    modifier = Modifier.width(140.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(MexaWarehouseColors.surfaceContainerLow, RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(count.toFloat() / max.toFloat())
                            .height(12.dp)
                            .background(MexaWarehouseColors.indigoAccent, RoundedCornerShape(4.dp)),
                    )
                }
                Text(
                    fmtAmount(count.toString()),
                    modifier = Modifier.width(64.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// ─── Top products table ────────────────────────────────────────────────────────

@Composable
private fun TopProductsContent(data: JsonObject?) {
    val lang = appLanguage()
    val items = data?.itemsOrContentArray()?.filterIsInstance<JsonObject>() ?: emptyList()
    if (items.isEmpty()) {
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    TableFrame {
        TableRow {
            Text(AppStrings.t(lang, "#"), modifier = Modifier.weight(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Mahsulot"), modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Sotilgan"), modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(
                AppStrings.t(lang, "Daromad (so'm)"),
                modifier = Modifier.weight(1.2f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = MexaWarehouseColors.outline,
                textAlign = TextAlign.End,
            )
        }
        items.forEachIndexed { idx, o ->
            TableRow(alt = idx % 2 == 1) {
                Text(
                    o.stringField("rank").ifBlank { (idx + 1).toString() },
                    modifier = Modifier.weight(0.4f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textMuted,
                )
                Text(
                    o.stringField("productName").ifBlank { "—" },
                    modifier = Modifier.weight(2f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    fmtAmount(o.stringField("soldCount")),
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                )
                Text(
                    fmtAmount(o.stringField("revenue")),
                    modifier = Modifier.weight(1.2f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Stock alerts table ────────────────────────────────────────────────────────

@Composable
private fun StockAlertsContent(data: JsonObject?) {
    val lang = appLanguage()
    val items = data?.itemsOrContentArray()?.filterIsInstance<JsonObject>() ?: emptyList()
    if (items.isEmpty()) {
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    val threshold = data?.stringField("lowStockThreshold").orEmpty().ifBlank { "—" }
    TableFrame {
        TableRow {
            Text(AppStrings.t(lang, "Mahsulot"), modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Ombor"), modifier = Modifier.weight(1.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Miqdor"), modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(
                AppStrings.t(lang, "Chegira"),
                modifier = Modifier.weight(0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = MexaWarehouseColors.outline,
                textAlign = TextAlign.End,
            )
        }
        items.forEachIndexed { idx, o ->
            TableRow(alt = idx % 2 == 1) {
                Text(
                    o.stringField("productName").ifBlank { "—" },
                    modifier = Modifier.weight(2f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    o.stringField("warehouseId").take(8).ifBlank { "—" },
                    modifier = Modifier.weight(1.4f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                Text(
                    fmtAmount(o.stringField("availableQuantity")),
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.statusBlockedFg,
                )
                Text(
                    threshold,
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// ─── Discounts ─────────────────────────────────────────────────────────────────

@Composable
private fun DiscountsContent(data: JsonObject?) {
    val lang = appLanguage()
    if (data == null) {
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    StatGrid(
        listOf<@Composable (Modifier) -> Unit>(
            { m ->
                StatCard(
                    AppStrings.t(lang, "Faol aksiyalar"),
                    fmtAmount(data.stringField("activePromotions")),
                    Icons.Default.BarChart,
                    MexaWarehouseColors.indigoAccent,
                    MexaWarehouseColors.statIndigoBg,
                    m,
                )
            },
            { m ->
                StatCard(
                    AppStrings.t(lang, "Bugungi chegirma (so'm)"),
                    fmtAmount(data.stringField("discountGivenToday")),
                    Icons.Default.AttachMoney,
                    MexaWarehouseColors.statusActiveFg,
                    MexaWarehouseColors.statusActiveBg,
                    m,
                )
            },
        ),
    )

    val coupons = (data["topCouponsLast30Days"] as? JsonArray)?.filterIsInstance<JsonObject>() ?: emptyList()
    if (coupons.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        EmptyHint(AppStrings.t(lang, "Ma'lumot topilmadi"))
        return
    }
    Spacer(Modifier.height(12.dp))
    TableFrame {
        TableRow {
            Text(AppStrings.t(lang, "Kod"), modifier = Modifier.weight(1.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Ishlatilgan"), modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(
                AppStrings.t(lang, "Summa (so'm)"),
                modifier = Modifier.weight(1.2f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = MexaWarehouseColors.outline,
                textAlign = TextAlign.End,
            )
        }
        coupons.forEachIndexed { idx, o ->
            TableRow(alt = idx % 2 == 1) {
                Text(
                    o.stringField("code").ifBlank { "—" },
                    modifier = Modifier.weight(1.4f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.indigoAccent,
                )
                Text(
                    fmtAmount(o.stringField("usageCount")),
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                )
                Text(
                    fmtAmount(o.stringField("totalDiscount")),
                    modifier = Modifier.weight(1.2f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Shared building blocks ────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    subtitle: String?,
    error: String?,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    val errText = when {
        error == null -> null
        error.isBlank() -> AppStrings.t(appLanguage(), "Ma'lumot yuklanmadi")
        else -> error
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MexaWarehouseColors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (errText != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MexaWarehouseColors.statusBlockedBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(errText, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MexaWarehouseColors.statusBlockedFg, maxLines = 3)
                TextButton(onClick = onRetry) {
                    Text(AppStrings.t(appLanguage(), "Qayta urinish"), color = MexaWarehouseColors.indigoAccent, fontSize = 13.sp)
                }
            }
        } else {
            content()
        }
    }
}

@Composable
private fun StatGrid(cards: List<@Composable (Modifier) -> Unit>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth < 700.dp
        val gap = 16.dp
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

@Composable
private fun StatCard(
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(40.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp))
            .background(MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TableFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
        content = content,
    )
}

@Composable
private fun TableRow(alt: Boolean = false, content: @Composable RowScope.() -> Unit) {
    if (alt) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (alt) MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f)
                else MexaWarehouseColors.surfaceLowest,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
    }
}

// ─── Parsing / formatting helpers ──────────────────────────────────────────────

/**
 * Analytics xizmati to'g'ridan-to'g'ri DTO qaytaradi (`{period, points: [...]}`),
 * ba'zi proxylar esa `{success, data: {...}}` konvertsiyasini qo'llaydi — ikkala
 * shakl ham qo'llab-quvvatlanadi.
 */
private fun requireData(text: String): JsonObject {
    val root = ApiClient.parseJsonObject(text) ?: throw IllegalStateException("")
    val success = (root["success"] as? JsonPrimitive)?.content
    if (success == "false") throw IllegalStateException(root.stringField("message"))
    val data = root["data"]
    if (data is JsonObject) return data
    return root
}

private fun fmtAmount(raw: String): String {
    if (raw.isBlank()) return "—"
    val dec = runCatching { java.math.BigDecimal(raw) }.getOrNull() ?: return raw
    val plain = dec.toPlainString()
    val whole = plain.substringBefore('.')
    val negative = whole.startsWith("-")
    val digits = whole.removePrefix("-")
    if (digits.isEmpty() || digits.any { !it.isDigit() }) return plain
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    return (if (negative) "-" else "") + grouped
}

private fun formatPercent(raw: String): String {
    val d = raw.toDoubleOrNull() ?: return "—"
    return String.format(Locale.US, "%.1f%%", d * 100.0)
}

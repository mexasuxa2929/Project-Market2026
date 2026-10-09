package mexa.club.desktop_app.market.ui.promotions

import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.components.PaginationBar
import mexa.club.desktop_app.market.ui.orders.formatMoney
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Models (PromotionResponse / PromotionStatsResponse / PromotionUsage) ─────

data class PromotionRow(
    val id: String,
    val code: String,
    val name: String,
    val description: String,
    val type: String,
    val value: String,
    val usageCount: Int,
    val usageLimit: Int?,
    val startsAt: String,
    val endsAt: String,
    val active: Boolean,
)

data class PromotionStatsData(
    val totalPromotions: String,
    val totalDiscountAmountGiven: String,
    val topPromotionCode: String,
    val topPromotionUsageCount: String,
)

data class PromotionUsageRow(
    val id: String,
    val userId: String,
    val orderId: String,
    val discountAmount: String,
    val usedAt: String,
)

/** PromotionType enum qiymatlari (discount-service). */
val PROMOTION_TYPE_OPTIONS = listOf("PERCENTAGE", "FIXED_AMOUNT", "FREE_SHIPPING")

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** "2026-12-31T23:59:00" → "2026-12-31 23:59" (kiritish maydoni uchun). */
private fun toDateTimeInput(iso: String): String {
    val s = iso.trim().replace('T', ' ')
    if (s.isEmpty()) return ""
    return s.take(16)
}

/** Foydalanuvchi kiritgan sani ISO LocalDateTime'ga aylantiradi; noto'g'ri bo'lsa null. */
private fun normalizeDateTime(raw: String): String? {
    var s = raw.trim().replace(' ', 'T')
    if (s.isEmpty()) return null
    if (s.length == 10) s = "${s}T00:00:00"
    else if (s.length == 16) s = "${s}:00"
    return if (runCatching { LocalDateTime.parse(s) }.isSuccess) s else null
}

private fun formatDateTime(iso: String): String {
    val s = iso.trim()
    if (s.isEmpty()) return "—"
    return runCatching {
        val parsed = if (s.contains("T") && !s.contains("+") && !s.endsWith("Z")) {
            LocalDateTime.parse(s).atZone(ZoneId.systemDefault())
        } else {
            OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault())
        }
        parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH))
    }.getOrElse { s }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(modifier: Modifier = Modifier) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var promotions by remember { mutableStateOf<List<PromotionRow>>(emptyList()) }
    var stats by remember { mutableStateOf<PromotionStatsData?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") }
    var activeExpanded by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf("ALL") }
    var typeExpanded by remember { mutableStateOf(false) }

    var page by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(20) }
    var totalElements by remember { mutableStateOf(0L) }

    var createOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<PromotionRow?>(null) }
    var deleteTarget by remember { mutableStateOf<PromotionRow?>(null) }
    var deactivateTarget by remember { mutableStateOf<PromotionRow?>(null) }
    var usagesTarget by remember { mutableStateOf<PromotionRow?>(null) }

    suspend fun fetchPromotions(): Pair<List<PromotionRow>, Long> = withContext(Dispatchers.IO) {
        val params = buildList {
            add("page=$page")
            add("size=$pageSize")
            if (searchQuery.isNotBlank()) {
                add("search=${URLEncoder.encode(searchQuery.trim(), "UTF-8")}")
            }
            if (activeFilter != "ALL") add("active=$activeFilter")
            if (typeFilter != "ALL") add("type=$typeFilter")
        }
        val text = ApiClient.get("/api/admin/promotions?${params.joinToString("&")}")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
        val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        val rows = items.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val id = o.stringField("id").ifEmpty { return@mapNotNull null }
            PromotionRow(
                id = id,
                code = o.stringField("code"),
                name = o.stringField("name"),
                description = o.stringField("description"),
                type = o.stringField("type").ifEmpty { "PERCENTAGE" },
                value = o.stringField("value").ifEmpty { "0" },
                usageCount = o.stringField("usageCount").toIntOrNull() ?: 0,
                usageLimit = o.stringField("usageLimit").toIntOrNull(),
                startsAt = o.stringField("startsAt"),
                endsAt = o.stringField("endsAt"),
                active = o.stringField("active").toBooleanStrictOrNull() ?: false,
            )
        }
        val total = (data?.get("totalElements") as? JsonPrimitive)?.content?.toLongOrNull()
            ?: rows.size.toLong()
        rows to total
    }

    suspend fun fetchStats(): PromotionStatsData? = withContext(Dispatchers.IO) {
        runCatching {
            val text = ApiClient.get("/api/admin/promotions/stats")
            val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
                ?: return@runCatching null
            PromotionStatsData(
                totalPromotions = data.stringField("totalPromotions").ifEmpty { "0" },
                totalDiscountAmountGiven = data.stringField("totalDiscountAmountGiven").ifEmpty { "0" },
                topPromotionCode = data.stringField("topPromotionCode"),
                topPromotionUsageCount = data.stringField("topPromotionUsageCount").ifEmpty { "0" },
            )
        }.getOrNull()
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchPromotions() }
                .onSuccess { (rows, total) ->
                    totalElements = total
                    val totalPages = if (total == 0L) 1 else ((total + pageSize - 1) / pageSize).toInt()
                    if (page > totalPages - 1) {
                        page = totalPages - 1 // effekt qayta ishga tushadi
                    } else {
                        promotions = rows
                    }
                }
                .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
            stats = fetchStats()
            loading = false
        }
    }

    // Debounce: filtr/qidiruv o'zgarishi → 300ms so'ng server so'rovi
    LaunchedEffect(searchQuery, activeFilter, typeFilter, page, pageSize) {
        delay(300)
        reload()
    }

    LaunchedEffect(Unit) {
        // Realtime mutatsiyalar → avtomatik yangilash
        GatewayRealtimeHub.dashboardRefresh.drop(1).collect { reload() }
    }

    if (createOpen) {
        PromotionFormDialog(
            initial = null,
            onDismiss = { createOpen = false },
            onSaved = { createOpen = false; reload() },
        )
    }

    editTarget?.let { target ->
        PromotionFormDialog(
            initial = target,
            onDismiss = { editTarget = null },
            onSaved = { editTarget = null; reload() },
        )
    }

    deleteTarget?.let { target ->
        PromotionDeleteDialog(
            promotion = target,
            onDismiss = { deleteTarget = null },
            onDeleted = { deleteTarget = null; reload() },
        )
    }

    deactivateTarget?.let { target ->
        PromotionDeactivateDialog(
            promotion = target,
            onDismiss = { deactivateTarget = null },
            onDeactivated = { deactivateTarget = null; reload() },
        )
    }

    usagesTarget?.let { target ->
        PromotionUsagesDialog(
            promotionId = target.id,
            promotionCode = target.code,
            onDismiss = { usagesTarget = null },
        )
    }

    val activeOptions = listOf("ALL", "true", "false")
    val totalPages = if (totalElements == 0L) 1 else ((totalElements + pageSize - 1) / pageSize).toInt()
    val safePage = page.coerceIn(0, totalPages - 1)

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
                Icon(Icons.Default.Percent, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(AppStrings.t(appLanguage(), "Aksiyalar"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text(AppStrings.t(appLanguage(), "Chegirma va promo-kodlarni boshqarish"), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
            Button(
                onClick = { createOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.t(appLanguage(), "Yangi aksiya"), fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Stat cards (GET /api/admin/promotions/stats) ──────────────────────
        if (!loading) {
            val s = stats
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth < 700.dp
                val gap = 16.dp
                val cards = listOf<@Composable (Modifier) -> Unit>(
                    { m ->
                        PromotionStatCard(
                            AppStrings.t(appLanguage(), "Jami aksiyalar"),
                            s?.totalPromotions ?: "0",
                            Icons.Default.Percent,
                            MexaWarehouseColors.indigoAccent,
                            MexaWarehouseColors.statIndigoBg,
                            m,
                        )
                    },
                    { m ->
                        PromotionStatCard(
                            AppStrings.t(appLanguage(), "Berilgan chegirma"),
                            formatMoney(s?.totalDiscountAmountGiven ?: "0"),
                            Icons.Default.Savings,
                            MexaWarehouseColors.statusActiveFg,
                            MexaWarehouseColors.statusActiveBg,
                            m,
                        )
                    },
                    { m ->
                        PromotionStatCard(
                            AppStrings.t(appLanguage(), "Top aksiya kodi"),
                            s?.topPromotionCode?.ifBlank { "—" } ?: "—",
                            Icons.Default.Star,
                            Color(0xFFB45309),
                            MexaWarehouseColors.amberBadgeBg,
                            m,
                        )
                    },
                    { m ->
                        PromotionStatCard(
                            AppStrings.t(appLanguage(), "Top aksiya foydalanishlari"),
                            s?.topPromotionUsageCount ?: "0",
                            Icons.Default.BarChart,
                            Color(0xFF0369A1),
                            Color(0xFFE0F2FE),
                            m,
                        )
                    },
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
                onValueChange = { searchQuery = it; page = 0 },
                placeholder = { Text(AppStrings.t(appLanguage(), "Aksiya nomi yoki kod bo'yicha..."), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenuBox(expanded = activeExpanded, onExpandedChange = { activeExpanded = it }) {
                OutlinedTextField(
                    value = when (activeFilter) {
                        "true" -> AppStrings.t(appLanguage(), "Faol")
                        "false" -> AppStrings.t(appLanguage(), "Nofaol")
                        else -> AppStrings.t(appLanguage(), "Barcha holatlar")
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(AppStrings.tableStatus(appLanguage())) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activeExpanded) },
                    modifier = Modifier.menuAnchor().widthIn(min = 140.dp, max = 180.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = activeExpanded, onDismissRequest = { activeExpanded = false }) {
                    activeOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (opt) {
                                        "true" -> AppStrings.t(appLanguage(), "Faol")
                                        "false" -> AppStrings.t(appLanguage(), "Nofaol")
                                        else -> AppStrings.t(appLanguage(), "Barcha holatlar")
                                    },
                                )
                            },
                            onClick = { activeFilter = opt; activeExpanded = false; page = 0 },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = if (typeFilter == "ALL") AppStrings.t(appLanguage(), "Barcha turlar") else promotionTypeLabel(typeFilter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(AppStrings.t(appLanguage(), "Turi")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().widthIn(min = 150.dp, max = 200.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.t(appLanguage(), "Barcha turlar")) },
                        onClick = { typeFilter = "ALL"; typeExpanded = false; page = 0 },
                    )
                    PROMOTION_TYPE_OPTIONS.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(promotionTypeLabel(opt)) },
                            onClick = { typeFilter = opt; typeExpanded = false; page = 0 },
                        )
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when {
            loading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }

            error != null -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: AppStrings.t(appLanguage(), "Xatolik"), color = MexaWarehouseColors.danger)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload() }) { Text(AppStrings.t(appLanguage(), "Qayta urinish")) }
                }
            }

            promotions.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(AppStrings.t(appLanguage(), "Aksiyalar topilmadi"), color = MexaWarehouseColors.textMuted)
            }

            else -> PromotionsTable(
                promotions = promotions,
                onEdit = { editTarget = it },
                onDelete = { deleteTarget = it },
                onDeactivate = { deactivateTarget = it },
                onUsages = { usagesTarget = it },
            )
        }

        // ── Pagination (umumiy andoza: PaginationBar) — server total ──────────
        if (!loading && error == null && promotions.isNotEmpty()) {
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            PaginationBar(
                page = safePage,
                pageSize = pageSize,
                totalElements = totalElements.toInt(),
                infoText = run {
                    val from = safePage * pageSize + 1
                    val to = minOf((safePage + 1) * pageSize, totalElements.toInt())
                    AppStrings.t(appLanguage(), "$from - $to / $totalElements ta aksiya")
                },
                onPageChange = { page = it },
                onPageSizeChange = { pageSize = it; page = 0 },
            )
        }
    }
}

// ─── Table ────────────────────────────────────────────────────────────────────

@Composable
private fun PromotionsTable(
    promotions: List<PromotionRow>,
    onEdit: (PromotionRow) -> Unit,
    onDelete: (PromotionRow) -> Unit,
    onDeactivate: (PromotionRow) -> Unit,
    onUsages: (PromotionRow) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf(
                AppStrings.t(appLanguage(), "NOM / KOD") to 2.2f,
                AppStrings.t(appLanguage(), "TURI") to 1.2f,
                AppStrings.t(appLanguage(), "CHEGIRMA") to 1.1f,
                AppStrings.t(appLanguage(), "MOHLAT") to 1.7f,
                AppStrings.t(appLanguage(), "FOYDALANISH") to 1.1f,
                AppStrings.t(appLanguage(), "HOLAT") to 0.8f,
                AppStrings.t(appLanguage(), "AMALLAR") to 1.6f,
            ).forEach { (h, w) ->
                Text(
                    h,
                    modifier = Modifier.weight(w),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = MexaWarehouseColors.outline,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        promotions.forEachIndexed { idx, p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(2.2f)) {
                    Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    if (p.code.isNotBlank()) {
                        Text(p.code, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                }
                Text(promotionTypeLabel(p.type), modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(
                    when (p.type) {
                        "PERCENTAGE" -> "${p.value}%"
                        "FIXED_AMOUNT" -> formatMoney(p.value)
                        else -> "—"
                    },
                    modifier = Modifier.weight(1.1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                )
                Column(Modifier.weight(1.7f)) {
                    Text(
                        "${formatDateTime(p.startsAt)} → ${formatDateTime(p.endsAt)}",
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textMuted,
                    )
                }
                Text(
                    if (p.usageLimit != null) "${p.usageCount} / ${p.usageLimit}" else p.usageCount.toString(),
                    modifier = Modifier.weight(1.1f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                )
                Box(Modifier.weight(0.8f)) {
                    val (bg, fg, label) = if (p.active)
                        Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, AppStrings.t(appLanguage(), "FAOL"))
                    else
                        Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, AppStrings.t(appLanguage(), "NOFAOL"))
                    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
                Row(modifier = Modifier.weight(1.6f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TableAction(icon = Icons.Default.Visibility, tint = MexaWarehouseColors.indigoAccent, description = AppStrings.t(appLanguage(), "Foydalanishlar")) { onUsages(p) }
                    TableAction(icon = Icons.Default.Edit, tint = MexaWarehouseColors.outline, description = AppStrings.t(appLanguage(), "Tahrirlash")) { onEdit(p) }
                    if (p.active) {
                        TableAction(icon = Icons.Default.Block, tint = Color(0xFFB45309), description = AppStrings.t(appLanguage(), "Nofaollashtirish")) { onDeactivate(p) }
                    }
                    TableAction(icon = Icons.Default.Delete, tint = MexaWarehouseColors.danger, description = AppStrings.t(appLanguage(), "O'chirish")) { onDelete(p) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun TableAction(
    icon: ImageVector,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ─── Labels ───────────────────────────────────────────────────────────────────

@Composable
private fun promotionTypeLabel(type: String): String = when (type) {
    "FIXED_AMOUNT" -> AppStrings.t(appLanguage(), "Qat'iy summa")
    "FREE_SHIPPING" -> AppStrings.t(appLanguage(), "Bepul yetkazish")
    else -> AppStrings.t(appLanguage(), "Foiz")
}

// ─── Stat card ────────────────────────────────────────────────────────────────

@Composable
private fun PromotionStatCard(
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
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, color = MexaWarehouseColors.textPrimary)
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

// ─── Create / Edit dialog (CreatePromotionRequest / UpdatePromotionRequest) ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromotionFormDialog(
    initial: PromotionRow?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "PERCENTAGE") }
    var value by remember { mutableStateOf(initial?.value ?: "") }
    var startsAt by remember { mutableStateOf(toDateTimeInput(initial?.startsAt ?: "")) }
    var endsAt by remember { mutableStateOf(toDateTimeInput(initial?.endsAt ?: "")) }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }

    fun save() {
        if (name.trim().isEmpty()) {
            error = AppStrings.t(lang, "Nomi majburiy")
            return
        }
        if (initial == null && code.trim().isEmpty()) {
            error = AppStrings.t(lang, "Kod majburiy")
            return
        }
        val valueDec = runCatching { java.math.BigDecimal(value.trim()) }.getOrNull()
        if (valueDec == null || valueDec.signum() < 0) {
            error = AppStrings.t(lang, "Chegirma qiymati noto'g'ri")
            return
        }
        val startsIso = normalizeDateTime(startsAt)
        if (startsIso == null) {
            error = AppStrings.t(lang, "Boshlanish vaqti noto'g'ri formatda")
            return
        }
        val endsIso = if (endsAt.isBlank()) null else normalizeDateTime(endsAt)
        if (endsAt.isNotBlank() && endsIso == null) {
            error = AppStrings.t(lang, "Tugash vaqti noto'g'ri formatda")
            return
        }

        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{")
                if (initial == null) append("\"code\":${code.trim().jsonString()},")
                append("\"name\":${name.trim().jsonString()}")
                append(",\"type\":${type.jsonString()}")
                append(",\"value\":${valueDec.toPlainString()}")
                append(",\"startsAt\":${startsIso.jsonString()}")
                if (endsIso != null) append(",\"endsAt\":${endsIso.jsonString()}")
                append(",\"description\":${description.trim().jsonString()}")
                append(",\"active\":$active")
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (initial == null) ApiClient.post("/api/admin/promotions", json)
                    else ApiClient.put("/api/admin/promotions/${initial.id}", json)
                }
            }
            saving = false
            result.onSuccess { onSaved() }
                .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
        }
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 340.dp, max = 560.dp).fillMaxWidth(0.88f),
        ) {
            Column(
                Modifier
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (initial == null) AppStrings.t(appLanguage(), "Yangi aksiya qo'shish")
                        else AppStrings.t(appLanguage(), "Aksiyani tahrirlash"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.t(appLanguage(), "Yopish"), tint = MexaWarehouseColors.textMuted)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(AppStrings.t(appLanguage(), "Nomi")) },
                    placeholder = { Text(AppStrings.t(appLanguage(), "Yoz aksiyasi"), color = MexaWarehouseColors.textCaption) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = name.isBlank() && error != null,
                    shape = RoundedCornerShape(8.dp),
                )

                if (initial == null) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; error = null },
                        label = { Text(AppStrings.t(appLanguage(), "Kod")) },
                        placeholder = { Text(AppStrings.t(appLanguage(), "YAY10"), color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = code.isBlank() && error != null,
                        shape = RoundedCornerShape(8.dp),
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(AppStrings.t(appLanguage(), "Turi"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(
                                value = promotionTypeLabel(type),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            )
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                PROMOTION_TYPE_OPTIONS.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(promotionTypeLabel(opt)) },
                                        onClick = { type = opt; typeExpanded = false; error = null },
                                    )
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            AppStrings.t(appLanguage(), "Chegirma qiymati"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it; error = null },
                            placeholder = {
                                Text(
                                    if (type == "PERCENTAGE") AppStrings.t(appLanguage(), "10")
                                    else AppStrings.t(appLanguage(), "50000"),
                                    color = MexaWarehouseColors.textCaption,
                                )
                            },
                            trailingIcon = {
                                if (type == "PERCENTAGE") Text("%", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = startsAt,
                        onValueChange = { startsAt = it; error = null },
                        label = { Text(AppStrings.t(appLanguage(), "Boshlanish vaqti")) },
                        placeholder = { Text(AppStrings.t(appLanguage(), "2026-12-01 00:00"), color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                    OutlinedTextField(
                        value = endsAt,
                        onValueChange = { endsAt = it; error = null },
                        label = { Text(AppStrings.t(appLanguage(), "Tugash vaqti")) },
                        placeholder = { Text(AppStrings.t(appLanguage(), "2026-12-31 23:59"), color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; error = null },
                    label = { Text(AppStrings.t(appLanguage(), "Tavsif")) },
                    placeholder = { Text(AppStrings.t(appLanguage(), "Ixtiyoriy matn"), color = MexaWarehouseColors.textCaption) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp),
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text(AppStrings.t(appLanguage(), "Faol"), fontSize = 14.sp, color = MexaWarehouseColors.textPrimary)
                }

                error?.let { err ->
                    Text(err, color = MexaWarehouseColors.danger, fontSize = 13.sp)
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) {
                        Text(AppStrings.cancel(appLanguage()))
                    }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(AppStrings.settingsSave(appLanguage()), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Delete confirmation ──────────────────────────────────────────────────────

@Composable
private fun PromotionDeleteDialog(
    promotion: PromotionRow,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = {
            Text(
                AppStrings.t(appLanguage(), "Aksiyani o'chirish"),
                fontWeight = FontWeight.Bold,
                color = MexaWarehouseColors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    AppStrings.t(appLanguage(), "\"${promotion.name}\" aksiyasini o'chirishni tasdiqlaysizmi?"),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                error?.let { Text(it, fontSize = 12.sp, color = MexaWarehouseColors.danger) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    deleting = true
                    error = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { ApiClient.delete("/api/admin/promotions/${promotion.id}") }
                        }
                        deleting = false
                        result.onSuccess { onDeleted() }
                            .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
                    }
                },
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
            ) {
                if (deleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(AppStrings.t(appLanguage(), "O'chirish"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { if (!deleting) onDismiss() }) {
                Text(AppStrings.cancel(appLanguage()))
            }
        },
    )
}

// ─── Deactivate confirmation (POST /{id}/deactivate) ──────────────────────────

@Composable
private fun PromotionDeactivateDialog(
    promotion: PromotionRow,
    onDismiss: () -> Unit,
    onDeactivated: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = {
            Text(
                AppStrings.t(appLanguage(), "Aksiyani nofaollashtirish"),
                fontWeight = FontWeight.Bold,
                color = MexaWarehouseColors.textPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    AppStrings.t(appLanguage(), "\"${promotion.name}\" aksiyasi darhol o'chiriladi va mijozlar uchun amal qilmaydi."),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                error?.let { Text(it, fontSize = 12.sp, color = MexaWarehouseColors.danger) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    working = true
                    error = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { ApiClient.post("/api/admin/promotions/${promotion.id}/deactivate", "{}") }
                        }
                        working = false
                        result.onSuccess { onDeactivated() }
                            .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
                    }
                },
                enabled = !working,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
            ) {
                if (working) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(AppStrings.t(appLanguage(), "Nofaollashtirish"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { if (!working) onDismiss() }) {
                Text(AppStrings.cancel(appLanguage()))
            }
        },
    )
}

// ─── Usages dialog (GET /{id}/usages) ─────────────────────────────────────────

@Composable
private fun PromotionUsagesDialog(
    promotionId: String,
    promotionCode: String,
    onDismiss: () -> Unit,
) {
    val lang = appLanguage()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var usages by remember { mutableStateOf<List<PromotionUsageRow>>(emptyList()) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/admin/promotions/$promotionId/usages?page=0&size=50")
                    val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
                    val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    items.mapNotNull { el ->
                        val u = el as? JsonObject ?: return@mapNotNull null
                        PromotionUsageRow(
                            id = u.stringField("id").ifEmpty { return@mapNotNull null },
                            userId = u.stringField("userId"),
                            orderId = u.stringField("orderId"),
                            discountAmount = u.stringField("discountAmount").ifEmpty { "0" },
                            usedAt = u.stringField("usedAt"),
                        )
                    }
                }
            }.onSuccess { usages = it }
                .onFailure { e -> error = e.message ?: AppStrings.t(lang, "Xatolik") }
            loading = false
        }
    }

    LaunchedEffect(promotionId) { load() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 340.dp, max = 620.dp).fillMaxWidth(0.9f),
        ) {
            Column(
                Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            AppStrings.t(appLanguage(), "Foydalanish tarixi"),
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                        Text(
                            promotionCode.ifBlank { promotionId },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.t(appLanguage(), "Yopish"), tint = MexaWarehouseColors.textMuted)
                    }
                }

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }

                    error != null -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error ?: AppStrings.t(appLanguage(), "Xatolik"), color = MexaWarehouseColors.danger, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { load() }, shape = RoundedCornerShape(8.dp)) {
                                Text(AppStrings.t(appLanguage(), "Qayta urinish"))
                            }
                        }
                    }

                    usages.isEmpty() -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(AppStrings.t(appLanguage(), "Foydalanishlar topilmadi"), color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
                    }

                    else -> Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            listOf(
                                AppStrings.t(appLanguage(), "FOYDALANUVCHI") to 1.4f,
                                AppStrings.t(appLanguage(), "BUYURTMA") to 1.4f,
                                AppStrings.t(appLanguage(), "CHEGIRMA") to 1f,
                                AppStrings.t(appLanguage(), "SANA") to 1.2f,
                            ).forEach { (h, w) ->
                                Text(
                                    h,
                                    modifier = Modifier.weight(w),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    color = MexaWarehouseColors.outline,
                                )
                            }
                        }
                        usages.forEachIndexed { idx, u ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    u.userId.take(8).ifBlank { "—" },
                                    modifier = Modifier.weight(1.4f),
                                    fontSize = 13.sp,
                                    color = MexaWarehouseColors.textPrimary,
                                )
                                Text(
                                    u.orderId.take(8).ifBlank { "—" },
                                    modifier = Modifier.weight(1.4f),
                                    fontSize = 13.sp,
                                    color = MexaWarehouseColors.textMuted,
                                )
                                Text(
                                    formatMoney(u.discountAmount),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MexaWarehouseColors.textPrimary,
                                )
                                Text(
                                    formatDateTime(u.usedAt),
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 12.sp,
                                    color = MexaWarehouseColors.textMuted,
                                )
                            }
                            if (idx < usages.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text(AppStrings.cancel(appLanguage()))
                    }
                }
            }
        }
    }
}

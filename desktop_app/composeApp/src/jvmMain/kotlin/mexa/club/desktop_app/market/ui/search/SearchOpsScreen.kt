package mexa.club.desktop_app.market.ui.search

import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SearchOpsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val lang = appLanguage()

    var statusLoading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf<JsonObject?>(null) }
    var statusError by remember { mutableStateOf<String?>(null) }

    var reindexRunning by remember { mutableStateOf(false) }
    var reindexResult by remember { mutableStateOf<JsonObject?>(null) }
    var reindexError by remember { mutableStateOf<String?>(null) }

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var resultsTotal by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun loadStatus() {
        scope.launch {
            statusLoading = true
            runCatching {
                val text = withContext(Dispatchers.IO) { ApiClient.get("/api/search/admin/status") }
                checkedData(text)
            }.onSuccess {
                status = it
                statusError = null
            }.onFailure { e ->
                statusError = e.message?.trim().orEmpty()
            }
            statusLoading = false
        }
    }

    fun runReindex() {
        scope.launch {
            reindexRunning = true
            reindexError = null
            reindexResult = null
            runCatching {
                val text = withContext(Dispatchers.IO) { ApiClient.post("/api/search/admin/reindex", "{}") }
                checkedData(text)
            }.onSuccess {
                reindexResult = it
                loadStatus()
            }.onFailure { e ->
                reindexError = e.message?.trim().orEmpty()
            }
            reindexRunning = false
        }
    }

    fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) {
            searchError = AppStrings.t(lang, "Qidiruv so'rovi bo'sh")
            return
        }
        scope.launch {
            searching = true
            searchError = null
            suggestions = emptyList()
            results = emptyList()
            resultsTotal = null
            hasSearched = false

            val enc = java.net.URLEncoder.encode(q, "UTF-8")
            var failure: String? = null

            runCatching {
                val text = withContext(Dispatchers.IO) {
                    ApiClient.get("/api/search/autocomplete?q=$enc&limit=8")
                }
                checkedArray(text)
            }.onSuccess { arr ->
                suggestions = arr.mapNotNull { (it as? JsonPrimitive)?.content }
            }.onFailure { e ->
                failure = e.message?.trim().orEmpty()
            }

            runCatching {
                val text = withContext(Dispatchers.IO) {
                    ApiClient.get("/api/search/products?q=$enc&page=0&size=10")
                }
                checkedData(text)
            }.onSuccess { data ->
                results = (data["products"] as? JsonArray)?.filterIsInstance<JsonObject>() ?: emptyList()
                resultsTotal = (data["total"] as? JsonPrimitive)?.content
            }.onFailure { e ->
                failure = failure ?: e.message?.trim().orEmpty()
            }

            searchError = failure
            hasSearched = true
            searching = false
        }
    }

    LaunchedEffect(Unit) { loadStatus() }

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
                Icon(Icons.Default.Search, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(AppStrings.t(lang, "Qidiruv boshqaruvi"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text(
                    AppStrings.t(lang, "Elasticsearch indeksi holati, qayta indekslash va qidiruvni sinash"),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
            }
            IconButton(
                onClick = { loadStatus() },
                enabled = !statusLoading,
                modifier = Modifier
                    .background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .size(40.dp),
            ) {
                if (statusLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MexaWarehouseColors.indigoAccent, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = AppStrings.t(lang, "Yangilash"), tint = MexaWarehouseColors.indigoAccent)
                }
            }
        }

        // ── Status section ────────────────────────────────────────────────────
        SectionCard(
            title = AppStrings.t(lang, "Indeks holati"),
            subtitle = status?.let { "${AppStrings.t(lang, "Server vaqti")}: ${it.stringField("serverTime").ifBlank { "—" }}" },
            error = if (status == null) statusError else null,
            onRetry = { loadStatus() },
        ) {
            when {
                statusLoading && status == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                }

                status == null -> Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    Text(AppStrings.t(lang, "Ma'lumot topilmadi"), color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
                }

                else -> {
                    val s = status
                    StatGrid(
                        listOf<@Composable (Modifier) -> Unit>(
                            { m ->
                                StatCard(
                                    AppStrings.t(lang, "Indeksdagi hujjatlar"),
                                    displayOrDash(s?.stringField("indexedCount")),
                                    Icons.Default.Storage,
                                    MexaWarehouseColors.indigoAccent,
                                    MexaWarehouseColors.statIndigoBg,
                                    m,
                                )
                            },
                            { m ->
                                StatCard(
                                    AppStrings.t(lang, "Jami mahsulotlar"),
                                    displayOrDash(s?.stringField("productTotal")),
                                    Icons.Default.Inventory,
                                    Color(0xFF1D4ED8),
                                    MexaWarehouseColors.blueBadgeBg,
                                    m,
                                )
                            },
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    // Oxirgi reindex kartasi
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp))
                            .background(MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(AppStrings.t(lang, "Oxirgi reindex"), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                displayOrDash(s?.stringField("lastReindexCount")),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            LastReindexBadge(s?.stringField("lastReindexOk"))
                        }
                        Text(
                            "${AppStrings.t(lang, "Vaqt")}: ${displayOrDash(s?.stringField("lastReindexAt"))}",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { runReindex() },
                        enabled = !reindexRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (reindexRunning) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(AppStrings.t(lang, "Reindex bajarilmoqda"), fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(AppStrings.t(lang, "Qayta indekslash"), fontSize = 14.sp)
                        }
                    }

                    reindexResult?.let { r ->
                        val ok = r.stringField("ok") == "true"
                        val label = if (ok) AppStrings.t(lang, "Reindex muvaffaqiyatli yakunlandi")
                        else AppStrings.t(lang, "Reindex xatosi")
                        Text(
                            "$label · ${AppStrings.t(lang, "Hujjatlar")}: ${displayOrDash(r.stringField("reindexedCount"))}" +
                                " · ${displayOrDash(r.stringField("finishedAt"))}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (ok) MexaWarehouseColors.greenBadgeFg else MexaWarehouseColors.statusBlockedFg,
                        )
                    }

                    reindexError?.let { raw ->
                        Text(
                            errorText(raw, lang),
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.danger,
                        )
                    }
                }
            }
        }

        // ── Index test section ────────────────────────────────────────────────
        SectionCard(
            title = AppStrings.t(lang, "Indeksni sinash"),
            subtitle = null,
            error = null,
            onRetry = {},
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(AppStrings.t(lang, "Qidiruv so'rovi"), fontSize = 13.sp) },
                    placeholder = { Text(AppStrings.t(lang, "Mahsulot nomini kiriting..."), fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                Button(
                    onClick = { runSearch() },
                    enabled = !searching,
                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (searching) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.t(lang, "Qidirish"), fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.t(lang, "Qidirish"), fontSize = 14.sp)
                    }
                }
            }

            if (searching) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                }
            }

            searchError?.let { raw ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.statusBlockedBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MexaWarehouseColors.statusBlockedFg, modifier = Modifier.size(16.dp))
                    Text(errorText(raw, lang), modifier = Modifier.weight(1f), fontSize = 12.sp, color = MexaWarehouseColors.statusBlockedFg, maxLines = 3)
                }
            }

            if (!searching && suggestions.isNotEmpty()) {
                Text(AppStrings.t(lang, "Takliflar"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                SuggestionChips(suggestions) { picked ->
                    query = picked
                    runSearch()
                }
            }

            if (!searching && hasSearched && searchError == null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(AppStrings.t(lang, "Natijalar"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    if (resultsTotal != null) {
                        Text(
                            "${AppStrings.t(lang, "Jami")}: ${resultsTotal ?: "—"}",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }
                }
                if (results.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                        Text(AppStrings.t(lang, "Ma'lumot topilmadi"), color = MexaWarehouseColors.textMuted, fontSize = 13.sp)
                    }
                } else {
                    ResultsTable(results)
                }
            }
        }
    }
}

// ─── Results table ─────────────────────────────────────────────────────────────

@Composable
private fun ResultsTable(rows: List<JsonObject>) {
    val lang = appLanguage()
    TableFrame {
        TableRow {
            Text(AppStrings.t(lang, "Mahsulot"), modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Narx"), modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline, textAlign = TextAlign.End)
            Text(AppStrings.t(lang, "Kategoriya"), modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Brend"), modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            Text(AppStrings.t(lang, "Mavjud"), modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline, textAlign = TextAlign.End)
        }
        rows.forEachIndexed { idx, o ->
            TableRow(alt = idx % 2 == 1) {
                Text(
                    o.stringField("name").ifBlank { "—" },
                    modifier = Modifier.weight(2f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    fmtAmount(o.stringField("salePrice")),
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    o.stringField("categoryName").ifBlank { "—" },
                    modifier = Modifier.weight(1.2f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    o.stringField("brandName").ifBlank { "—" },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
                    InStockBadge(o.stringField("inStock") == "true")
                }
            }
        }
    }
}

@Composable
private fun InStockBadge(inStock: Boolean) {
    val lang = appLanguage()
    val bg = if (inStock) MexaWarehouseColors.greenBadgeBg else MexaWarehouseColors.statusBlockedBg
    val fg = if (inStock) MexaWarehouseColors.greenBadgeFg else MexaWarehouseColors.statusBlockedFg
    val label = if (inStock) AppStrings.t(lang, "Ha") else AppStrings.t(lang, "Yo'q")
    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun LastReindexBadge(okRaw: String?) {
    val lang = appLanguage()
    val (bg, fg, label) = when (okRaw) {
        "true" -> Triple(MexaWarehouseColors.greenBadgeBg, MexaWarehouseColors.greenBadgeFg, AppStrings.t(lang, "Muvaffaqiyatli"))
        "false" -> Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, AppStrings.t(lang, "Muvaffaqiyatsiz"))
        else -> Triple(MexaWarehouseColors.surfaceContainerLow, MexaWarehouseColors.textMuted, "—")
    }
    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ─── Suggestion chips ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChips(items: List<String>, onClick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { s ->
            Box(
                Modifier
                    .background(MexaWarehouseColors.infoBadgeBg, RoundedCornerShape(16.dp))
                    .clickable { onClick(s) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(s, fontSize = 12.sp, color = MexaWarehouseColors.indigoAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        error.isBlank() -> AppStrings.t(appLanguage(), "Xizmatga ulanib bo'lmadi")
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

// ─── Parsing / formatting helpers ──────────────────────────────────────────────

/** `{success, data:{...}}` konvertsiyasidan data obyektini oladi. */
private fun checkedData(text: String): JsonObject {
    val root = ApiClient.parseJsonObject(text) ?: throw IllegalStateException("")
    val success = (root["success"] as? JsonPrimitive)?.content
    if (success == "false") throw IllegalStateException(root.stringField("message"))
    return root["data"] as? JsonObject ?: throw IllegalStateException("")
}

/** `{success, data:[...]}` konvertsiyasidan massivni oladi (autocomplete). */
private fun checkedArray(text: String): JsonArray {
    val root = ApiClient.parseJsonObject(text) ?: throw IllegalStateException("")
    val success = (root["success"] as? JsonPrimitive)?.content
    if (success == "false") throw IllegalStateException(root.stringField("message"))
    return root["data"] as? JsonArray ?: JsonArray(emptyList())
}

private fun displayOrDash(raw: String?): String = raw?.ifBlank { null } ?: "—"

private fun errorText(raw: String, lang: mexa.club.desktop_app.localization.AppLanguage): String =
    if (raw.isBlank()) AppStrings.t(lang, "Xizmatga ulanib bo'lmadi") else raw

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

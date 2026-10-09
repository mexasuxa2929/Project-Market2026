package mexa.club.desktop_app.market.ui.auditlog

import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.components.PaginationBar
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

data class AuditLogRow(
    val id: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val actorUsername: String,
    val details: String,
    val createdAt: String,
)

@Composable
fun AuditLogScreen(modifier: Modifier = Modifier, initialQuery: String = "") {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var logs by remember { mutableStateOf<List<AuditLogRow>>(emptyList()) }
    var searchQuery by remember { mutableStateOf(initialQuery) }

    // T-H1: global qidiruvdan kelgan so'rovni qo'llash (ekran allaqachon ochiq bo'lsa ham)
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) searchQuery = initialQuery
    }
    var page by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(20) }
    // Server pagination (T-H4): jami soni serverdan keladi
    var totalElements by remember { mutableStateOf(0L) }

    suspend fun fetchLogs(): Pair<List<AuditLogRow>, Long> = withContext(Dispatchers.IO) {
        val params = buildList {
            add("page=$page")
            add("size=$pageSize")
            if (searchQuery.isNotBlank()) {
                add("q=${java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8")}")
            }
        }
        val text = ApiClient.get("/api/audit-logs?${params.joinToString("&")}")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
        val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        val rows = items.mapNotNull { el ->
            val l = el as? JsonObject ?: return@mapNotNull null
            AuditLogRow(
                id = l.stringField("id").ifEmpty { return@mapNotNull null },
                action = l.stringField("action"),
                targetType = l.stringField("targetType"),
                targetId = l.stringField("targetId"),
                actorUsername = l.stringField("actorUsername"),
                details = l.stringField("details"),
                createdAt = l.stringField("createdAt"),
            )
        }
        val total = (data?.get("totalElements") as? JsonPrimitive)?.content?.toLongOrNull()
            ?: rows.size.toLong()
        rows to total
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchLogs() }
                .onSuccess { (rows, total) ->
                    totalElements = total
                    val totalPages = if (total == 0L) 1 else ((total + pageSize - 1) / pageSize).toInt()
                    if (page > totalPages - 1) {
                        page = totalPages - 1 // effekt qayta ishga tushadi (to'g'ri sahifa)
                    } else {
                        logs = rows
                    }
                }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    // Debounce: har bosishda emas, yozish to'xtagandan keyin serverga so'rov (T-H4)
    LaunchedEffect(searchQuery, page, pageSize) {
        delay(300)
        reload()
    }

    val totalPages = if (totalElements == 0L) 1 else ((totalElements + pageSize - 1) / pageSize).toInt()
    val safePage = page.coerceIn(0, totalPages - 1)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Audit jurnali", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Tizimda amalga oshirilgan o'zgarishlar tarixi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; page = 0 },
            placeholder = { Text("Amal, obyekt turi yoki foydalanuvchi bo'yicha...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )

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
            logs.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Yozuvlar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> AuditLogTable(logs)
        }

        // ── Pagination (umumiy andoza: PaginationBar) — server total (T-H4) ──
        if (!loading && error == null && logs.isNotEmpty()) {
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            PaginationBar(
                page = safePage,
                pageSize = pageSize,
                totalElements = totalElements.toInt(),
                infoText = run {
                    val from = safePage * pageSize + 1
                    val to = minOf((safePage + 1) * pageSize, totalElements.toInt())
                    "$from - $to / $totalElements ta yozuv"
                },
                onPageChange = { page = it },
                onPageSizeChange = { pageSize = it; page = 0 },
            )
        }
    }
}

@Composable
private fun AuditLogTable(logs: List<AuditLogRow>) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Amal" to 1.2f, "Obyekt" to 1.4f, "Foydalanuvchi" to 1.2f, "Tafsilot" to 2f, "Sana" to 1f).forEach { (h, w) ->
                Text(h.uppercase(), modifier = Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        logs.forEachIndexed { idx, l ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(l.action.ifBlank { "—" }, modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Column(Modifier.weight(1.4f)) {
                    Text(l.targetType.ifBlank { "—" }, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                    if (l.targetId.isNotBlank()) Text(l.targetId.take(8), fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                }
                Text(l.actorUsername.ifBlank { "—" }, modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(l.details.ifBlank { "—" }, modifier = Modifier.weight(2f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                Text(l.createdAt.take(19).replace("T", " "), modifier = Modifier.weight(1f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

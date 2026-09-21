package mexa.club.desktop_app.market.ui.couriers

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private const val ALL_FILTER = "__all__"

data class CourierRow(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val region: String,
    val active: Boolean,
    val currentDeliveries: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouriersScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var couriers by remember { mutableStateOf<List<CourierRow>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(ALL_FILTER) }
    var statusExpanded by remember { mutableStateOf(false) }
    var regionFilter by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CourierRow?>(null) }
    var detailTarget by remember { mutableStateOf<CourierRow?>(null) }

    var stats by remember { mutableStateOf<CourierStatsModel?>(null) }

    suspend fun fetchCouriers(): List<CourierRow> = withContext(Dispatchers.IO) {
        val params = mutableListOf<String>()
        if (searchQuery.isNotBlank()) params.add("search=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}")
        if (statusFilter != ALL_FILTER) params.add("active=$statusFilter")
        if (regionFilter.isNotBlank()) params.add("region=${java.net.URLEncoder.encode(regionFilter, "UTF-8")}")
        val queryStr = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        val text = ApiClient.get("/api/admin/couriers$queryStr")
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val id = c.stringField("id").ifEmpty { return@mapNotNull null }
            CourierRow(
                id = id,
                userId = c.stringField("userId"),
                name = c.stringField("name"),
                phone = c.stringField("phone"),
                region = c.stringField("region"),
                active = c.stringField("active").toBooleanStrictOrNull() ?: true,
                currentDeliveries = c.stringField("currentDeliveries").toIntOrNull() ?: 0,
            )
        }
    }

    suspend fun fetchStats(): CourierStatsModel? = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/couriers/stats")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) } ?: return@withContext null
        CourierStatsModel(
            totalCouriers = data.stringField("totalCouriers").toLongOrNull() ?: 0,
            activeCouriers = data.stringField("activeCouriers").toLongOrNull() ?: 0,
            totalCurrentDeliveries = data.stringField("totalCurrentDeliveries").toLongOrNull() ?: 0,
            todayDeliveries = data.stringField("todayDeliveries").toLongOrNull() ?: 0,
            weeklyDeliveries = data.stringField("weeklyDeliveries").toLongOrNull() ?: 0,
            monthlyDeliveries = data.stringField("monthlyDeliveries").toLongOrNull() ?: 0,
            successRate = data.stringField("successRate").toDoubleOrNull() ?: 0.0,
        )
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchCouriers() }
                .onSuccess { couriers = it }
                .onFailure { error = it.message ?: "Xatolik" }
            runCatching { fetchStats() }
                .onSuccess { stats = it }
            loading = false
        }
    }

    fun exportCsv() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = ApiClient.getBytes("/api/admin/couriers/export")
                    val file = java.io.File(System.getProperty("user.home") + "/Downloads/couriers_${java.time.LocalDate.now()}.csv")
                    file.writeBytes(bytes)
                }
            }
        }
    }

    if (showAddDialog) {
        CourierFormDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSaved = { showAddDialog = false; reload() },
        )
    }
    editTarget?.let { target ->
        CourierFormDialog(
            existing = target,
            onDismiss = { editTarget = null },
            onSaved = { editTarget = null; reload() },
        )
    }
    detailTarget?.let { target ->
        CourierDetailDialog(
            courierId = target.id,
            onDismiss = { detailTarget = null },
            onEdit = { detailTarget = null; editTarget = target },
            onDeleted = { detailTarget = null; reload() },
            onUpdated = { detailTarget = null; reload() },
        )
    }

    // Boshlang'ich yuklash
    LaunchedEffect(Unit) { reload() }

    // Matn filtrlari — 400ms debounce (har harfda so'rov ketmasligi uchun)
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery to regionFilter }
            .drop(1)
            .debounce(400)
            .collect { reload() }
    }

    // Dropdown filtri — darhol
    LaunchedEffect(Unit) {
        snapshotFlow { statusFilter }
            .drop(1)
            .collect { reload() }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Kuryerlar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text("Kuryerlar ro'yxati va holati", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportCsv() }, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export CSV")
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yangi kuryer", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Stats ──
        if (!loading && stats != null) {
            val s = stats!!
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth < 700.dp
                val gap = 12.dp
                val cards = listOf<@Composable (Modifier) -> Unit>(
                    { m -> CourierStatCard("Jami kuryerlar", s.totalCouriers.toString(), Icons.Default.People, MexaWarehouseColors.indigoAccent, MexaWarehouseColors.statIndigoBg, m) },
                    { m -> CourierStatCard("Faol", s.activeCouriers.toString(), Icons.Default.CheckCircle, MexaWarehouseColors.statusActiveFg, MexaWarehouseColors.statusActiveBg, m) },
                    { m -> CourierStatCard("Joriy", s.totalCurrentDeliveries.toString(), Icons.Default.LocalShipping, Color(0xFF0369A1), Color(0xFFE0F2FE), m) },
                    { m -> CourierStatCard("Bugun", s.todayDeliveries.toString(), Icons.Default.DeliveryDining, MexaWarehouseColors.roleCourierFg, MexaWarehouseColors.roleCourierBg, m) },
                )
                val rows = cards.chunked(if (twoColumns) 2 else 4)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    rows.forEach { rowCards ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            rowCards.forEach { it(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // ── Filter row ──
        Row(
            Modifier.fillMaxWidth()
                .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
                .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ism, telefon yoki hudud bo'yicha...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(
                    value = when (statusFilter) {
                        ALL_FILTER -> "Barcha"
                        "true" -> "Faol"
                        else -> "Nofaol"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Holat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().widthIn(min = 130.dp, max = 160.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    DropdownMenuItem(text = { Text("Barcha") }, onClick = { statusFilter = ALL_FILTER; statusExpanded = false })
                    DropdownMenuItem(text = { Text("Faol") }, onClick = { statusFilter = "true"; statusExpanded = false })
                    DropdownMenuItem(text = { Text("Nofaol") }, onClick = { statusFilter = "false"; statusExpanded = false })
                }
            }
            OutlinedTextField(
                value = regionFilter,
                onValueChange = { regionFilter = it },
                label = { Text("Hudud") },
                placeholder = { Text("Filter...") },
                modifier = Modifier.widthIn(min = 130.dp, max = 160.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
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
            couriers.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Kuryerlar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> CouriersTable(couriers, onRowClick = { detailTarget = it })
        }
    }
}

private data class CourierStatsModel(
    val totalCouriers: Long,
    val activeCouriers: Long,
    val totalCurrentDeliveries: Long,
    val todayDeliveries: Long,
    val weeklyDeliveries: Long,
    val monthlyDeliveries: Long,
    val successRate: Double,
)

@Composable
private fun CouriersTable(couriers: List<CourierRow>, onRowClick: (CourierRow) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Ism" to 1.6f, "Telefon" to 1.2f, "Hudud" to 1.2f, "Joriy yetkazmalar" to 1f, "Holat" to 0.8f).forEach { (h, w) ->
                Text(h.uppercase(), modifier = Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        couriers.forEachIndexed { idx, c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .clickable { onRowClick(c) }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.name, modifier = Modifier.weight(1.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Text(c.phone, modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(c.region, modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(c.currentDeliveries.toString(), modifier = Modifier.weight(1f), fontSize = 14.sp, color = MexaWarehouseColors.textPrimary)
                Box(Modifier.weight(0.8f)) {
                    val (bg, fg, label) = if (c.active)
                        Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "FAOL")
                    else
                        Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
                    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun CourierStatCard(label: String, value: String, icon: ImageVector, iconTint: Color, iconBg: Color, modifier: Modifier = Modifier) {
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

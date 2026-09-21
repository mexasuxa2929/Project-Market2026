package mexa.club.desktop_app.market.ui.warehouses

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.api.totalElementsString
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Model ─────────────────────────────────────────────────────────────────────

private data class WarehouseCardData(
    val id: String,
    val name: String,
    val location: String,
    val isActive: Boolean,
    val capacity: Int,
    val adminCount: Int,
    val productCount: Int = 0,
    val lowStockCount: Int = 0,
    val todayIncoming: Int = 0,
    val usedVolumeM3: Double = 0.0,
    val capacityPct: Int = 0,
) {
    val usagePercent: Int
        get() = capacityPct
}

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehousesScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var warehouses by remember { mutableStateOf<List<WarehouseCardData>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Barcha holatlar") }
    var gridView by remember { mutableStateOf(true) }
    var statusExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWarehouse by remember { mutableStateOf<WarehouseCardData?>(null) }
    val isSuperAdmin = AuthSession.isSuperAdmin

    suspend fun fetchWarehouses(): List<WarehouseCardData> = withContext(Dispatchers.IO) {
        val whText = ApiClient.get("/api/warehouses?page=0&size=100")
        val whData = ApiClient.parseJsonObject(whText)?.let { ApiClient.dataObject(it) }
        val items = whData?.itemsOrContentArray() ?: JsonArray(emptyList())

        items.mapNotNull { el ->
            val w = el as? JsonObject ?: return@mapNotNull null
            val id = w.stringField("id").ifEmpty { return@mapNotNull null }

            WarehouseCardData(
                id = id,
                name = w.stringField("name"),
                location = w.stringField("location"),
                isActive = (w["active"] as? JsonPrimitive)?.content?.toBoolean() ?: false,
                capacity = (w["capacity"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                adminCount = (w["adminUserIds"] as? JsonArray)?.size ?: 0,
                productCount = (w["productCount"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                lowStockCount = (w["lowStockCount"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                todayIncoming = (w["todayIncoming"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                usedVolumeM3 = (w["usedVolumeM3"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                capacityPct = (w["capacityPct"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
            )
        }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchWarehouses() }
                .onSuccess { warehouses = it }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    fun refreshAfterEdit() {
        scope.launch {
            runCatching { fetchWarehouses() }
                .onSuccess { list ->
                    warehouses = list
                    selectedWarehouse?.let { sel ->
                        selectedWarehouse = list.find { it.id == sel.id } ?: sel
                    }
                }
        }
    }

    selectedWarehouse?.let { sel ->
        WarehouseDetailScreen(
            warehouseId = sel.id,
            warehouseName = sel.name,
            warehouseLocation = sel.location,
            warehouseCapacity = sel.capacity,
            warehouseActive = sel.isActive,
            onBack = { selectedWarehouse = null; reload() },
            onWarehouseUpdated = { refreshAfterEdit() },
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    LaunchedEffect(Unit) {
        reload()
        GatewayRealtimeHub.dashboardRefresh.drop(1).collect {
            reload()
        }
    }

    if (showAddDialog) {
        AddWarehouseDialog(
            onDismiss = { showAddDialog = false },
            onSaved = {
                showAddDialog = false
                reload()
            },
        )
    }

    // Filtering
    val statusOptions = listOf("Barcha holatlar", "Faol", "Nofaol")
    val filtered = warehouses.filter { w ->
        val matchSearch = searchQuery.isBlank() || w.name.contains(searchQuery, ignoreCase = true)
            || w.location.contains(searchQuery, ignoreCase = true)
        val matchStatus = when (statusFilter) {
            "Faol" -> w.isActive
            "Nofaol" -> !w.isActive
            else -> true
        }
        matchSearch && matchStatus
    }

    // Stats
    val totalCount = warehouses.size
    val activeCount = warehouses.count { it.isActive }
    val totalCapacity = warehouses.sumOf { it.capacity }
    val totalLowStock = warehouses.sumOf { it.lowStockCount }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Warehouse, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Omborlar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text("Ombor boshqaruv tizimi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                }
            }
            if (isSuperAdmin) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yangi ombor", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Stat cards ────────────────────────────────────────────────────────
        if (!loading) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth < 700.dp
                val gap = 16.dp
                if (twoColumns) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            WarehouseStatCard(
                                label = "Jami omborlar",
                                value = totalCount.toString(),
                                icon = Icons.Default.Warehouse,
                                iconTint = MexaWarehouseColors.indigoAccent,
                                iconBg = MexaWarehouseColors.statIndigoBg,
                                modifier = Modifier.weight(1f),
                            )
                            WarehouseStatCard(
                                label = "Faol omborlar",
                                value = activeCount.toString(),
                                icon = Icons.Default.CheckCircle,
                                iconTint = MexaWarehouseColors.statusActiveFg,
                                iconBg = MexaWarehouseColors.statusActiveBg,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            WarehouseStatCard(
                                label = "Jami sig'im",
                                value = formatCapacity(totalCapacity),
                                valueSuffix = "m³",
                                icon = Icons.Default.Inventory2,
                                iconTint = Color(0xFF0369A1),
                                iconBg = Color(0xFFE0F2FE),
                                modifier = Modifier.weight(1f),
                            )
                            WarehouseStatCard(
                                label = "Kam qolgan mahsulotlar",
                                value = totalLowStock.toString(),
                                icon = Icons.Default.Warning,
                                iconTint = Color(0xFFD97706),
                                iconBg = MexaWarehouseColors.amberBadgeBg,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        WarehouseStatCard(
                            label = "Jami omborlar",
                            value = totalCount.toString(),
                            icon = Icons.Default.Warehouse,
                            iconTint = MexaWarehouseColors.indigoAccent,
                            iconBg = MexaWarehouseColors.statIndigoBg,
                            modifier = Modifier.weight(1f),
                        )
                        WarehouseStatCard(
                            label = "Faol omborlar",
                            value = activeCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconTint = MexaWarehouseColors.statusActiveFg,
                            iconBg = MexaWarehouseColors.statusActiveBg,
                            modifier = Modifier.weight(1f),
                        )
                        WarehouseStatCard(
                            label = "Jami sig'im",
                            value = formatCapacity(totalCapacity),
                            valueSuffix = "m³",
                            icon = Icons.Default.Inventory2,
                            iconTint = Color(0xFF0369A1),
                            iconBg = Color(0xFFE0F2FE),
                            modifier = Modifier.weight(1f),
                        )
                        WarehouseStatCard(
                            label = "Kam qolgan mahsulotlar",
                            value = totalLowStock.toString(),
                            icon = Icons.Default.Warning,
                            iconTint = Color(0xFFD97706),
                            iconBg = MexaWarehouseColors.amberBadgeBg,
                            modifier = Modifier.weight(1f),
                        )
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
                placeholder = { Text("Ombor nomi bo'yicha...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(
                    value = statusFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Holat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().widthIn(min = 140.dp, max = 200.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = { statusFilter = opt; statusExpanded = false })
                    }
                }
            }
            // Grid / List toggle
            Row(
                Modifier
                    .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                    .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp)),
            ) {
                IconButton(
                    onClick = { gridView = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (gridView) MexaWarehouseColors.indigoAccent else Color.Transparent,
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Warehouse,
                        contentDescription = "Grid",
                        tint = if (gridView) Color.White else MexaWarehouseColors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = { gridView = false },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (!gridView) MexaWarehouseColors.indigoAccent else Color.Transparent,
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = "List",
                        tint = if (!gridView) Color.White else MexaWarehouseColors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
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
                    Text(error ?: "Xatolik", color = MexaWarehouseColors.danger)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload() }) { Text("Qayta urinish") }
                }
            }

            else -> if (gridView) {
                WarehousesGrid(
                    warehouses = filtered,
                    onCardClick = { selectedWarehouse = it },
                    onAddClick = { showAddDialog = true },
                    showAddCard = isSuperAdmin,
                )
            } else {
                WarehousesList(
                    warehouses = filtered,
                    onRowClick = { selectedWarehouse = it },
                )
            }
        }
    }
}

// ─── Grid ─────────────────────────────────────────────────────────────────────

@Composable
private fun WarehousesGrid(
    warehouses: List<WarehouseCardData>,
    onCardClick: (WarehouseCardData) -> Unit,
    onAddClick: () -> Unit,
    showAddCard: Boolean = true,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // 1 column < 600dp, 2 columns < 1000dp, 3 columns otherwise
        val columns = when {
            maxWidth < 600.dp  -> 1
            maxWidth < 1000.dp -> 2
            else               -> 3
        }
        val allItems: List<WarehouseCardData?> = warehouses + if (showAddCard) listOf(null) else emptyList()
        val rows = allItems.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { w ->
                        if (w != null) {
                            WarehouseCard(w, onClick = { onCardClick(w) }, modifier = Modifier.weight(1f))
                        } else {
                            AddWarehouseCard(onClick = onAddClick, modifier = Modifier.weight(1f))
                        }
                    }
                    // Fill remaining empty slots so cards keep equal widths
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─── List (table) ─────────────────────────────────────────────────────────────

@Composable
private fun WarehousesList(
    warehouses: List<WarehouseCardData>,
    onRowClick: (WarehouseCardData) -> Unit,
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text("Ombor", Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
                Text("Holat", Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
                Text("Sig'im", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
                Text("Mahsulotlar", Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
                Text("Bugungi kirim", Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
                Text("Kam qolgan", Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted)
            }
            warehouses.forEachIndexed { index, w ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(w) }
                        .background(
                            if (index % 2 == 1) MexaWarehouseColors.borderSubtle.copy(alpha = 0.15f) else Color.Transparent,
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(2f)) {
                        Text(w.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                        if (w.location.isNotBlank()) {
                            Text(w.location, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }
                    val (badgeBg, badgeFg, badgeText) = if (w.isActive)
                        Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "FAOL")
                    else
                        Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
                    Box(
                        Modifier
                            .weight(0.8f)
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeFg)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                            Text("${w.usagePercent}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            if (w.usedVolumeM3 > 0) {
                                Text("· ${formatVolume(w.usedVolumeM3)} m³", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                            }
                        }
                        Box(Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MexaWarehouseColors.borderSubtle)) {
                            val barColor = when {
                                w.usagePercent >= 90 -> Color(0xFFDC2626)
                                w.usagePercent >= 70 -> Color(0xFFD97706)
                                else -> MexaWarehouseColors.indigoAccent
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth(w.usagePercent / 100f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor),
                            )
                        }
                    }
                    Text(w.productCount.toString(), Modifier.weight(0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    Text(w.todayIncoming.toString(), Modifier.weight(0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.indigoAccent)
                    Text(
                        w.lowStockCount.toString(),
                        Modifier.weight(0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (w.lowStockCount > 0) Color(0xFFDC2626) else MexaWarehouseColors.textMuted,
                    )
                }
            }
        }
    }
}

// ─── Warehouse card ───────────────────────────────────────────────────────────

@Composable
private fun WarehouseCard(w: WarehouseCardData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Name + status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(w.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Status badge
                        val (badgeBg, badgeFg, badgeText) = if (w.isActive)
                            Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "FAOL")
                        else
                            Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
                        Box(
                            Modifier
                                .background(badgeBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeFg)
                        }
                        if (w.adminCount > 0) {
                            Text("• ${w.adminCount} admin", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }
                }
            }

            // Capacity bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sig'im bandligi", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    Text("${w.usagePercent}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MexaWarehouseColors.borderSubtle),
                ) {
                    val barColor = when {
                        w.usagePercent >= 90 -> Color(0xFFDC2626)
                        w.usagePercent >= 70 -> Color(0xFFD97706)
                        else -> MexaWarehouseColors.indigoAccent
                    }
                    Box(
                        Modifier
                            .fillMaxWidth(w.usagePercent / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor),
                    )
                }
            }

            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WarehouseStatCell("MAHSULOTLAR", w.productCount.toString(), MexaWarehouseColors.textPrimary)
                WarehouseStatCell("BUGUNGI KIRIM", w.todayIncoming.toString(), MexaWarehouseColors.indigoAccent)
                WarehouseStatCell("KAM QOLGAN", w.lowStockCount.toString(),
                    if (w.lowStockCount > 0) Color(0xFFDC2626) else MexaWarehouseColors.textMuted)
            }
        }
    }
}

@Composable
private fun WarehouseStatCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.3.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ─── Add card ─────────────────────────────────────────────────────────────────

@Composable
private fun AddWarehouseCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(180.dp)
            .border(2.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(48.dp).background(MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Warehouse, contentDescription = null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(26.dp))
            }
            Text("+ Yangi ombor qo'shish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
            Text("Yangi logistika nozbasini oslash", fontSize = 12.sp, color = MexaWarehouseColors.textCaption, textAlign = TextAlign.Center)
        }
    }
}

// ─── Stat card ────────────────────────────────────────────────────────────────

@Composable
private fun WarehouseStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    valueSuffix: String? = null,
) {
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
            if (valueSuffix != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, color = MexaWarehouseColors.textPrimary)
                    Text(valueSuffix, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                }
            } else {
                Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, color = MexaWarehouseColors.textPrimary)
            }
        }
        Box(
            Modifier.size(44.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatCapacity(value: Int): String {
    if (value == 0) return "0"
    return value.toString().reversed().chunked(3).joinToString(" ").reversed()
}

private fun formatVolume(value: Double): String {
    if (value <= 0) return "0"
    return if (value >= 1000) {
        val v = (value / 1000.0).let { Math.round(it * 10) / 10.0 }
        "$v k"
    } else {
        val v = Math.round(value * 10) / 10.0
        "$v"
    }
}

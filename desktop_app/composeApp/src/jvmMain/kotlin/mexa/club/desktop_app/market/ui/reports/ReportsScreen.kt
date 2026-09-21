package mexa.club.desktop_app.market.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

private data class DashboardStats(
    val todayOrders: Long,
    val todayRevenue: String,
    val todayNewUsers: Long,
    val monthOrders: Long,
    val monthRevenue: String,
    val lowStockAlerts: Long,
    val pendingOrders: Long,
)

private val REPORT_TABS = listOf("Sotuvlar", "Mahsulotlar", "Mijozlar")
private val REPORT_ENDPOINTS = listOf("sales", "products", "customers")
private val REPORT_COLUMN_SETS = listOf(
    listOf("orderNumber" to "Buyurtma №", "userId" to "Mijoz", "totalAmount" to "Summa", "status" to "Holat", "createdAt" to "Sana"),
    listOf("orderNumber" to "Buyurtma №", "userId" to "Mijoz", "totalAmount" to "Summa", "status" to "Holat", "createdAt" to "Sana"),
    listOf("username" to "Foydalanuvchi", "email" to "Email", "enabled" to "Faol", "createdAt" to "Ro'yxatdan o'tgan sana"),
)

@Composable
fun ReportsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var dashboardLoading by remember { mutableStateOf(true) }
    var dashboard by remember { mutableStateOf<DashboardStats?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var reportLoading by remember { mutableStateOf(true) }
    var reportError by remember { mutableStateOf<String?>(null) }

    suspend fun fetchDashboard(): DashboardStats = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/reports/dashboard")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
            ?: throw IllegalStateException("Ma'lumot topilmadi")
        val today = data["today"] as? JsonObject
        val month = data["thisMonth"] as? JsonObject
        DashboardStats(
            todayOrders = today?.stringField("orders")?.toLongOrNull() ?: 0L,
            todayRevenue = today?.stringField("revenue")?.ifEmpty { "0" } ?: "0",
            todayNewUsers = today?.stringField("newUsers")?.toLongOrNull() ?: 0L,
            monthOrders = month?.stringField("orders")?.toLongOrNull() ?: 0L,
            monthRevenue = month?.stringField("revenue")?.ifEmpty { "0" } ?: "0",
            lowStockAlerts = data.stringField("lowStockAlerts").toLongOrNull() ?: 0L,
            pendingOrders = data.stringField("pendingOrders").toLongOrNull() ?: 0L,
        )
    }

    suspend fun fetchReport(endpoint: String): List<JsonObject> = withContext(Dispatchers.IO) {
        val query = buildString {
            append("/api/reports/$endpoint?format=json")
            if (dateFrom.isNotBlank()) append("&dateFrom=$dateFrom")
            if (dateTo.isNotBlank()) append("&dateTo=$dateTo")
        }
        val text = ApiClient.get(query)
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.filterIsInstance<JsonObject>()
    }

    fun loadReport() {
        scope.launch {
            reportLoading = true
            reportError = null
            runCatching { fetchReport(REPORT_ENDPOINTS[selectedTab]) }
                .onSuccess { rows = it }
                .onFailure { reportError = it.message ?: "Xatolik" }
            reportLoading = false
        }
    }

    LaunchedEffect(Unit) {
        dashboardLoading = true
        runCatching { fetchDashboard() }.onSuccess { dashboard = it }
        dashboardLoading = false
    }
    LaunchedEffect(selectedTab) { loadReport() }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Hisobotlar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Savdo, mahsulot va mijozlar bo'yicha statistika", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        if (!dashboardLoading && dashboard != null) {
            val d = dashboard!!
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val twoColumns = maxWidth < 700.dp
                val gap = 16.dp
                val cards = listOf<@Composable (Modifier) -> Unit>(
                    { m -> ReportStatCard("Bugungi buyurtmalar", d.todayOrders.toString(), Icons.Default.ShoppingCart, MexaWarehouseColors.indigoAccent, MexaWarehouseColors.statIndigoBg, m) },
                    { m -> ReportStatCard("Bugungi daromad", formatMoney(d.todayRevenue), Icons.Default.AttachMoney, MexaWarehouseColors.statusActiveFg, MexaWarehouseColors.statusActiveBg, m) },
                    { m -> ReportStatCard("Oylik buyurtmalar", d.monthOrders.toString(), Icons.Default.ShoppingCart, Color(0xFF0369A1), Color(0xFFE0F2FE), m) },
                    { m -> ReportStatCard("Oylik daromad", formatMoney(d.monthRevenue), Icons.Default.AttachMoney, Color(0xFF0369A1), Color(0xFFE0F2FE), m) },
                    { m -> ReportStatCard("Kam qolgan stock", d.lowStockAlerts.toString(), Icons.Default.Warning, Color(0xFFB45309), MexaWarehouseColors.amberBadgeBg, m) },
                    { m -> ReportStatCard("Kutilayotgan buyurtmalar", d.pendingOrders.toString(), Icons.Default.People, MexaWarehouseColors.statusBlockedFg, MexaWarehouseColors.statusBlockedBg, m) },
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
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        cards.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                row.forEach { it(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        } else if (dashboardLoading) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = MexaWarehouseColors.surfaceLowest, contentColor = MexaWarehouseColors.indigoAccent) {
            REPORT_TABS.forEachIndexed { idx, label ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = dateFrom, onValueChange = { dateFrom = it },
                placeholder = { Text("Boshlanish (YYYY-MM-DD)", fontSize = 13.sp) },
                modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp),
            )
            OutlinedTextField(
                value = dateTo, onValueChange = { dateTo = it },
                placeholder = { Text("Tugash (YYYY-MM-DD)", fontSize = 13.sp) },
                modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp),
            )
            Button(
                onClick = { loadReport() },
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Qidirish") }
        }

        when {
            reportLoading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
            reportError != null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text(reportError ?: "Xatolik", color = MexaWarehouseColors.danger)
            }
            rows.isEmpty() -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text("Ma'lumot topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> GenericReportTable(rows, REPORT_COLUMN_SETS[selectedTab])
        }
    }
}

@Composable
private fun GenericReportTable(rows: List<JsonObject>, columns: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(Modifier.background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp)) {
            columns.forEach { (_, label) ->
                Text(label.uppercase(), modifier = Modifier.width(180.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        rows.forEachIndexed { idx, r ->
            Row(
                Modifier
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .padding(vertical = 12.dp, horizontal = 20.dp),
            ) {
                columns.forEach { (key, _) ->
                    Text(r.stringField(key).ifBlank { "—" }, modifier = Modifier.width(180.dp), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun ReportStatCard(label: String, value: String, icon: ImageVector, iconTint: Color, iconBg: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textMuted)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, color = MexaWarehouseColors.textPrimary)
        }
        Box(Modifier.size(40.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatMoney(raw: String): String {
    val clean = raw.substringBefore(".")
    val negative = clean.startsWith("-")
    val digits = clean.removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    return (if (negative) "-" else "") + grouped + " so'm"
}

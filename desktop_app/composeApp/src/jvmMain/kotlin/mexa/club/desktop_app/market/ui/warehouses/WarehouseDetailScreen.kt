package mexa.club.desktop_app.market.ui.warehouses

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

// ─── Models ────────────────────────────────────────────────────────────────────

private data class StockLine(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val reserved: Double,
    val available: Double,
    val minStock: Double = 0.0,
    val minStockOverride: Double? = null,
    val volumeM3: Double = 0.0,
) {
    val isLow: Boolean get() = available <= minStock
    val statusOk: Boolean get() = available > minStock
}

private data class MovementRow(
    val id: String,
    val movementType: String,
    val status: String = "CONFIRMED",
    val productName: String,
    val quantity: Double,
    val actorUsername: String,
    val createdAt: String,
)

internal data class TransferRow(
    val id: String,
    val fromWarehouseName: String,
    val productName: String,
    val quantity: Double,
    val createdAt: String,
)

private data class StockLotRow(
    val id: String,
    val purchaseId: String,
    val productName: String,
    val quantity: Double,
    val unitCost: Double,
    val totalCost: Double,
    val receivedDate: String,
)

private data class ProfitLine(
    val productName: String,
    val soldQuantity: Double,
    val salePrice: Double,
    val revenue: Double,
    val cost: Double,
    val profit: Double,
)

private data class AdminInfo(
    val id: String,
    val username: String,
    val email: String,
)

private data class WarehouseOrder(
    val id: String,
    val orderNumber: String,
    val status: String,
    val totalAmount: Double,
    val currency: String,
    val createdAt: String,
    val itemCount: Int,
)

private data class DetailData(
    val stockLines: List<StockLine> = emptyList(),
    val stockTotal: Int = 0,
    val lowAlerts: List<StockLine> = emptyList(),
    val movements: List<MovementRow> = emptyList(),
    val movementsTotal: Int = 0,
    val pendingTransfers: List<TransferRow> = emptyList(),
    val admins: List<AdminInfo> = emptyList(),
    val orders: List<WarehouseOrder> = emptyList(),
    val ordersTotal: Int = 0,
    val lots: List<StockLotRow> = emptyList(),
    val profitLines: List<ProfitLine> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val capacityM3: Double = 0.0,
    val usedVolumeM3: Double = 0.0,
    val capacityPct: Int = 0,
)

private enum class DetailTab { STOCK, LOW_STOCK, HISTORY, LOTS, PROFIT, ADMINS, INVENTORY, RETURNS, ORDERS }

// ─── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WarehouseDetailScreen(
    warehouseId: String,
    warehouseName: String,
    warehouseLocation: String,
    warehouseCapacity: Int,
    warehouseActive: Boolean,
    onBack: () -> Unit,
    onWarehouseUpdated: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(DetailTab.STOCK) }
    var loading by remember { mutableStateOf(true) }
    var data by remember { mutableStateOf(DetailData()) }
    var stockPage by remember { mutableStateOf(1) }
    var histPage by remember { mutableStateOf(1) }
    var stockSearch by remember { mutableStateOf("") }
    var showAssignAdmin by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showCreatePurchase by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var adjustTarget by remember { mutableStateOf<StockLine?>(null) }
    var showEditMinStock by remember { mutableStateOf(false) }
    var editMinTarget by remember { mutableStateOf<StockLine?>(null) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isForceDeleting by remember { mutableStateOf(false) }
    var pageSize by remember { mutableStateOf(10) }
    var ordersPage by remember { mutableStateOf(1) }
    var loadedTabs by remember { mutableStateOf(setOf(DetailTab.STOCK)) }

    suspend fun fetchShared(search: String = stockSearch, sPage: Int = 1): DetailData = withContext(Dispatchers.IO) {
        val searchQ = search.trim()

        val stockText = runCatching {
            val url = buildString {
                append("/api/warehouses/$warehouseId/stock?page=${sPage - 1}&size=$pageSize")
                if (searchQ.isNotEmpty()) append("&search=${java.net.URLEncoder.encode(searchQ, "UTF-8")}")
            }
            ApiClient.get(url)
        }.getOrElse { return@withContext DetailData() }

        val stockObj = ApiClient.parseJsonObject(stockText)?.let { ApiClient.dataObject(it) }
        val stockTotal = stockObj?.totalElementsString()?.toIntOrNull() ?: 0
        val stockLines = (stockObj?.itemsOrContentArray() ?: JsonArray(emptyList()))
            .mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                StockLine(
                    id = o.stringField("id"),
                    productId = o.stringField("productId"),
                    productName = o.stringField("productName").ifEmpty { "Mahsulot #${o.stringField("productId").take(8)}" },
                    quantity = (o["quantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    reserved = (o["reservedQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    available = (o["availableQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    minStock = (o["minStock"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    minStockOverride = (o["minStockOverride"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                    volumeM3 = (o["volumeM3"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                )
            }

        val lowAlerts = runCatching {
            val ls = ApiClient.get("/api/warehouses/$warehouseId/stock/alerts/low")
            val root = ApiClient.parseJsonObject(ls)
            (root?.get("data") as? JsonArray)?.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                StockLine(
                    id = o.stringField("productId"),
                    productId = o.stringField("productId"),
                    productName = o.stringField("productName").ifEmpty { "Mahsulot" },
                    quantity = (o["quantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    reserved = (o["reservedQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    available = (o["availableQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    minStock = (o["minStock"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                )
            } ?: emptyList()
        }.getOrElse { emptyList() }

        val pendingTransfers = runCatching {
            val text = ApiClient.get("/api/warehouses/$warehouseId/stock/transfers/pending")
            val root = ApiClient.parseJsonObject(text)
            ((root?.get("data") as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                TransferRow(
                    id = o.stringField("id"),
                    fromWarehouseName = o.stringField("fromWarehouseName").ifEmpty { "Noma'lum ombor" },
                    productName = o.stringField("productName").ifEmpty { "Mahsulot #${o.stringField("productId").take(8)}" },
                    quantity = (o["quantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    createdAt = o.stringField("createdAt").take(16).replace("T", " "),
                )
            }
        }.getOrElse { emptyList() }

        val capacityObj = runCatching {
            ApiClient.parseJsonObject(ApiClient.get("/api/warehouses/$warehouseId/stock/capacity"))?.get("data") as? JsonObject
        }.getOrElse { null }
        val capacityM3 = (capacityObj?.get("capacityM3") as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
        val usedVolumeM3 = (capacityObj?.get("usedVolumeM3") as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
        val capacityPct = (capacityObj?.get("capacityPct") as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

        DetailData(
            stockLines = stockLines,
            stockTotal = stockTotal,
            lowAlerts = lowAlerts,
            pendingTransfers = pendingTransfers,
            capacityM3 = capacityM3,
            usedVolumeM3 = usedVolumeM3,
            capacityPct = capacityPct,
        )
    }

    suspend fun fetchTabData(t: DetailTab, sPage: Int = 1, hPage: Int = 1): DetailData = withContext(Dispatchers.IO) {
        val shared = fetchShared(stockSearch, sPage)
        when (t) {
            DetailTab.HISTORY -> {
                val histText = runCatching {
                    ApiClient.get("/api/warehouses/$warehouseId/stock/history?page=${hPage - 1}&size=$pageSize")
                }.getOrElse { null }
                val histObj = histText?.let { ApiClient.parseJsonObject(it)?.let { r -> ApiClient.dataObject(r) } }
                val histTotal = histObj?.totalElementsString()?.toIntOrNull() ?: 0
                val movements = (histObj?.itemsOrContentArray() ?: JsonArray(emptyList()))
                    .mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        MovementRow(
                            id = o.stringField("id"),
                            movementType = o.stringField("movementType"),
                            status = o.stringField("status").ifEmpty { "CONFIRMED" },
                            productName = o.stringField("productName").ifEmpty { "Mahsulot #${o.stringField("productId").take(8)}" },
                            quantity = (o["quantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            actorUsername = o.stringField("actorUsername"),
                            createdAt = o.stringField("createdAt").take(16).replace("T", " "),
                        )
                    }
                shared.copy(movements = movements, movementsTotal = histTotal)
            }
            DetailTab.LOTS -> {
                val lots = runCatching {
                    val ls = ApiClient.get("/api/warehouses/$warehouseId/stock/lots")
                    val root = ApiClient.parseJsonObject(ls)
                    (root?.get("data") as? JsonArray)?.mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        StockLotRow(
                            id = o.stringField("id"),
                            purchaseId = o.stringField("purchaseId"),
                            productName = o.stringField("productName").ifEmpty { "Mahsulot #${o.stringField("productId").take(8)}" },
                            quantity = (o["quantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            unitCost = (o["unitCost"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            totalCost = (o["totalCost"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            receivedDate = o.stringField("receivedDate").take(16).replace("T", " "),
                        )
                    } ?: emptyList()
                }.getOrElse { emptyList() }
                shared.copy(lots = lots)
            }
            DetailTab.PROFIT -> {
                val profitObj = runCatching {
                    ApiClient.get("/api/warehouses/$warehouseId/stock/report/profit")
                }.getOrElse { null }?.let { ApiClient.parseJsonObject(it) }?.let { ApiClient.dataObject(it) }
                val profitLines = (profitObj?.get("lines") as? JsonArray)?.mapNotNull { el ->
                    val o = el as? JsonObject ?: return@mapNotNull null
                    ProfitLine(
                        productName = o.stringField("productName").ifEmpty { "Mahsulot" },
                        soldQuantity = (o["soldQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                        salePrice = (o["salePrice"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                        revenue = (o["revenue"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                        cost = (o["cost"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                        profit = (o["profit"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                    )
                } ?: emptyList()
                val totalRevenue = (profitObj?.get("totalRevenue") as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                val totalCost = (profitObj?.get("totalCost") as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                val totalProfit = (profitObj?.get("totalProfit") as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                shared.copy(
                    profitLines = profitLines,
                    totalRevenue = totalRevenue,
                    totalCost = totalCost,
                    totalProfit = totalProfit,
                )
            }
            DetailTab.ADMINS -> {
                val admins = runCatching {
                    val adminIds = (ApiClient.parseJsonObject(ApiClient.get("/api/warehouses/$warehouseId/admins"))?.get("data") as? JsonArray)
                        ?.mapNotNull { (it as? JsonPrimitive)?.content }
                        ?: emptyList()
                    val userArr = (ApiClient.parseJsonObject(ApiClient.get("/api/admin/users/by-role?role=ROLE_ADMIN"))?.get("data") as? JsonArray)
                        ?: JsonArray(emptyList())
                    adminIds.map { id ->
                        val u = userArr.mapNotNull { el -> el as? JsonObject }.find { it.stringField("id") == id }
                        AdminInfo(
                            id = id,
                            username = u?.stringField("username").orEmpty(),
                            email = u?.stringField("email").orEmpty(),
                        )
                    }
                }.getOrElse { emptyList() }
                shared.copy(admins = admins)
            }
            DetailTab.ORDERS -> {
                val ordersObj = runCatching {
                    ApiClient.get("/api/admin/orders?warehouseId=$warehouseId&page=${ordersPage - 1}&size=$pageSize")
                }.getOrElse { null }?.let { ApiClient.parseJsonObject(it)?.let { r -> ApiClient.dataObject(r) } }
                val ordersTotal = ordersObj?.totalElementsString()?.toIntOrNull() ?: 0
                val orders = (ordersObj?.itemsOrContentArray() ?: JsonArray(emptyList()))
                    .mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        val itemsArr = o["items"] as? JsonArray
                        WarehouseOrder(
                            id = o.stringField("id"),
                            orderNumber = o.stringField("orderNumber").ifEmpty { "#${o.stringField("id").take(8)}" },
                            status = o.stringField("status").ifEmpty { "PENDING" },
                            totalAmount = (o["totalAmount"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                            currency = o.stringField("currency").ifEmpty { "UZS" },
                            createdAt = o.stringField("createdAt").take(16).replace("T", " "),
                            itemCount = itemsArr?.size ?: 0,
                        )
                    }
                shared.copy(orders = orders, ordersTotal = ordersTotal)
            }
            else -> shared
        }
    }

    fun mergeDetailData(old: DetailData, fresh: DetailData, active: DetailTab): DetailData {
        var merged = old.copy(
            stockLines = fresh.stockLines,
            stockTotal = fresh.stockTotal,
            lowAlerts = fresh.lowAlerts,
            pendingTransfers = fresh.pendingTransfers,
            capacityM3 = fresh.capacityM3,
            usedVolumeM3 = fresh.usedVolumeM3,
            capacityPct = fresh.capacityPct,
        )
        when (active) {
            DetailTab.STOCK -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.LOW_STOCK -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.HISTORY -> {
                merged = merged.copy(
                    movements = fresh.movements,
                    movementsTotal = fresh.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.LOTS -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = fresh.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.PROFIT -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = fresh.profitLines,
                    totalRevenue = fresh.totalRevenue,
                    totalCost = fresh.totalCost,
                    totalProfit = fresh.totalProfit,
                    admins = old.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.ADMINS -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = fresh.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.ORDERS -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    orders = fresh.orders,
                    ordersTotal = fresh.ordersTotal,
                )
            }
            else -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
        }
        return merged
    }

    fun reload(sPage: Int = stockPage, hPage: Int = histPage) {
        scope.launch {
            loading = true
            val active = tab
            val fresh = if (active == DetailTab.STOCK) {
                fetchShared(stockSearch, sPage)
            } else {
                fetchTabData(active, sPage, hPage)
            }
            data = mergeDetailData(data, fresh, active)
            loadedTabs = loadedTabs + active
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        reload()
        GatewayRealtimeHub.dashboardRefresh.drop(1).collect {
            reload()
        }
    }

    var prevStockSearch by remember { mutableStateOf(stockSearch) }
    LaunchedEffect(stockSearch) {
        if (stockSearch != prevStockSearch) {
            prevStockSearch = stockSearch
            delay(350)
            stockPage = 1
            reload(1)
        }
    }

    val bottomMetrics = computeBottomMetrics(data.stockLines, data.stockTotal)
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        // ── Breadcrumb ────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, null, modifier = Modifier.size(16.dp).clickable { onBack() }, tint = MexaWarehouseColors.indigoAccent)
            Text(
                "Omborlar",
                fontSize = 13.sp,
                color = MexaWarehouseColors.indigoAccent,
                modifier = Modifier.clickable { onBack() },
            )
            Text("›", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            Text(warehouseName, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header card ───────────────────────────────────────────────────
            Surface(
                Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MexaWarehouseColors.surfaceLowest,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(warehouseName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                            val (bg, fg, lbl) = if (warehouseActive)
                                Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "FAOL")
                            else
                                Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
                            Box(Modifier.background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(14.dp))
                                Text(warehouseLocation.ifEmpty { "Manzil ko'rsatilmagan" }, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            }
                            if (warehouseCapacity > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Inventory, null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(14.dp))
                                    Text("${formatNum(warehouseCapacity)} m³ umumiy sig'im", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box {
                            Button(
                                onClick = { showCreatePurchase = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kirim qo'shish", fontWeight = FontWeight.SemiBold)
                            }
                            if (data.pendingTransfers.isNotEmpty()) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-8).dp)
                                        .background(Color(0xFFEF4444), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        data.pendingTransfers.size.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { actionsMenuExpanded = true },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Amallar",
                                    tint = MexaWarehouseColors.textMuted,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = actionsMenuExpanded,
                                onDismissRequest = { actionsMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                            Text("Transfer qilish", fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showTransfer = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                            Text("Tuzatish", fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        adjustTarget = null
                                        showAdjust = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                            Text("Tahrirlash", fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showEditDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                            Text("Admin tayinlash", fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showAssignAdmin = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.danger)
                                            Text("O'chirish", fontSize = 14.sp, color = MexaWarehouseColors.danger)
                                        }
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showDeleteConfirm = true
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                    .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DetailTab.entries.forEach { t ->
                    val label = when (t) {
                        DetailTab.STOCK -> "Stok ko'rinishi"
                        DetailTab.LOW_STOCK -> "Kam stok"
                        DetailTab.HISTORY -> "Harakat tarixi"
                        DetailTab.LOTS -> "Lotlar"
                        DetailTab.PROFIT -> "Foyda"
                        DetailTab.ADMINS -> "Adminlar"
                        DetailTab.INVENTORY -> "Inventarizatsiya"
                        DetailTab.RETURNS -> "Qaytarmalar"
                        DetailTab.ORDERS -> "Buyurtmalar"
                    }
                    val badge = when (t) {
                        DetailTab.LOW_STOCK -> if (data.lowAlerts.isNotEmpty()) data.lowAlerts.size.toString() else null
                        DetailTab.ORDERS -> if (data.ordersTotal > 0) data.ordersTotal.toString() else null
                        else -> null
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tab == t) MexaWarehouseColors.indigoAccent else Color.Transparent)
                            .clickable {
                                tab = t
                                if (t !in loadedTabs) reload()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (tab == t) Color.White else MexaWarehouseColors.textMuted,
                            )
                            if (badge != null) {
                                Box(
                                    Modifier.background(Color(0xFFEF4444), RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
                                ) {
                                    Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // ── Tab content ───────────────────────────────────────────────────
            if (loading) {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                }
            } else {
                when (tab) {
                    DetailTab.STOCK -> StockTab(
                        lines = data.stockLines,
                        total = data.stockTotal,
                        page = stockPage,
                        pageSize = pageSize,
                        search = stockSearch,
                        onSearch = { stockSearch = it },
                        onPage = { p -> stockPage = p; reload() },
                        onPagePrev = { if (stockPage > 1) { stockPage--; reload() } },
                        onPageNext = { if (stockPage * pageSize < data.stockTotal) { stockPage++; reload() } },
                        onPageSizeChange = { s -> pageSize = s; stockPage = 1; reload() },
                        onFirst = { stockPage = 1; reload() },
                        onLast = { val tp = if (data.stockTotal == 0) 1 else (data.stockTotal + pageSize - 1) / pageSize; stockPage = tp; reload() },
                        onTransfer = { showTransfer = true },
                        onAdjust = { line -> adjustTarget = line; showAdjust = true },
                        onEditMinStock = { line -> editMinTarget = line; showEditMinStock = true },
                    )
                    DetailTab.LOW_STOCK -> LowStockTab(data.lowAlerts)
                    DetailTab.HISTORY -> HistoryTab(
                        rows = data.movements,
                        total = data.movementsTotal,
                        page = histPage,
                        pageSize = pageSize,
                        onPageSizeChange = { s -> pageSize = s; histPage = 1; reload() },
                        onPage = { p -> histPage = p; reload() },
                        onPrev = { if (histPage > 1) { histPage--; reload() } },
                        onNext = { if (histPage * pageSize < data.movementsTotal) { histPage++; reload() } },
                        onFirst = { histPage = 1; reload(1) },
                        onLast = { val tp = if (data.movementsTotal == 0) 1 else (data.movementsTotal + pageSize - 1) / pageSize; histPage = tp; reload(tp) },
                    )
                    DetailTab.LOTS -> LotsTab(
                        lots = data.lots,
                        isSuperAdmin = AuthSession.isSuperAdmin,
                        onCancelLot = { purchaseId ->
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        ApiClient.delete("/api/warehouses/$warehouseId/purchases/$purchaseId")
                                    }
                                    reload()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Lot bekor qilishda xatolik: ${e.message}")
                                }
                            }
                        },
                    )
                    DetailTab.PROFIT -> ProfitTab(data.profitLines, data.totalRevenue, data.totalCost, data.totalProfit)
                    DetailTab.ADMINS -> AdminsTab(data.admins)
                    DetailTab.INVENTORY -> InventoryTab(
                        warehouseId = warehouseId,
                        onChanged = { reload() },
                    )
                    DetailTab.RETURNS -> ReturnsTab(
                        warehouseId = warehouseId,
                        onChanged = { reload() },
                    )
                    DetailTab.ORDERS -> OrdersTab(
                        orders = data.orders,
                        total = data.ordersTotal,
                        page = ordersPage,
                        pageSize = pageSize,
                        onPageSizeChange = { s -> pageSize = s; ordersPage = 1; reload() },
                        onPrev = { if (ordersPage > 1) { ordersPage--; reload() } },
                        onNext = { if (ordersPage * pageSize < data.ordersTotal) { ordersPage++; reload() } },
                        onFirst = { ordersPage = 1; reload() },
                        onLast = { val tp = if (data.ordersTotal == 0) 1 else (data.ordersTotal + pageSize - 1) / pageSize; ordersPage = tp; reload() },
                        onPageChange = { p -> ordersPage = p; reload() },
                    )
                }
            }

            // ── Bottom stat cards ─────────────────────────────────────────────
            if (!loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // JORIY STOK
                    StatBottomCard(Modifier.weight(1f)) {
                        Text("JORIY STOK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(formatNum(bottomMetrics.currentUnits.toInt()), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                            Text("dona", fontSize = 14.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                    // ENG KO'P HARAKAT
                    StatBottomCard(Modifier.weight(1f)) {
                        Text("ENG KO'P HARAKAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(10.dp))
                        val top = data.stockLines.maxByOrNull { it.quantity }
                        if (top != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(36.dp).background(MexaWarehouseColors.statIndigoBg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Inventory, null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(top.productName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatNum(top.quantity.toInt())} ta stokda", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                }
                            }
                        } else {
                            Text("Ma'lumot yo'q", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }
                    // SIG'IM
                    StatBottomCard(Modifier.weight(1f)) {
                        Text("SIG'IM (m³)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { data.capacityPct / 100f },
                                    modifier = Modifier.size(56.dp),
                                    color = when {
                                        data.capacityPct >= 90 -> Color(0xFFEF4444)
                                        data.capacityPct >= 70 -> Color(0xFFF59E0B)
                                        else -> MexaWarehouseColors.indigoAccent
                                    },
                                    strokeWidth = 6.dp,
                                    trackColor = MexaWarehouseColors.borderSubtle,
                                )
                                Text("${data.capacityPct}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                            }
                            Column {
                                Text("${formatVolume(data.usedVolumeM3)} / ${formatVolume(data.capacityM3)} m³ band", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                                Text(
                                    if (data.capacityM3 <= 0) "ombor sig'imi kiritilmagan"
                                    else "${formatVolume(data.capacityM3)} m³ umumiy sig'im",
                                    fontSize = 12.sp, color = MexaWarehouseColors.textMuted,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    if (showAssignAdmin) {
        AssignAdminDialog(
            warehouseId = warehouseId,
            onDismiss = { showAssignAdmin = false },
            onSaved = {
                showAssignAdmin = false
                reload()
            },
        )
    }

    if (showCreatePurchase) {
        PurchaseCreateDialog(
            warehouseId = warehouseId,
            pendingTransfers = data.pendingTransfers,
            onTransfersChanged = { reload() },
            onDismiss = { showCreatePurchase = false },
            onSaved = {
                showCreatePurchase = false
                reload()
            },
        )
    }

    if (showTransfer) {
        TransferStockDialog(
            warehouseId = warehouseId,
            onDismiss = { showTransfer = false },
            onSaved = {
                showTransfer = false
                reload()
            },
        )
    }

    if (showAdjust) {
        AdjustStockDialog(
            warehouseId = warehouseId,
            initialProductId = adjustTarget?.productId,
            initialProductName = adjustTarget?.productName,
            onDismiss = { showAdjust = false },
            onSaved = {
                showAdjust = false
                reload()
            },
        )
    }

    if (showEditMinStock) {
        editMinTarget?.let { line ->
            EditMinStockDialog(
                warehouseId = warehouseId,
                productId = line.productId,
                productName = line.productName,
                currentMinStock = line.minStock,
                onDismiss = { showEditMinStock = false },
                onSaved = {
                    showEditMinStock = false
                    reload()
                },
            )
        }
    }

    if (showEditDialog) {
        EditWarehouseDialog(
            warehouseId = warehouseId,
            initialName = warehouseName,
            initialCapacity = warehouseCapacity,
            initialActive = warehouseActive,
            onDismiss = { showEditDialog = false },
            onSaved = {
                showEditDialog = false
                onWarehouseUpdated()
            },
        )
    }

    if (showDeleteConfirm) {
        DeleteWarehouseConfirmDialog(
            warehouseName = warehouseName,
            isDeleting = isDeleting,
            isForceDeleting = isForceDeleting,
            error = deleteError,
            onDismiss = { showDeleteConfirm = false; isDeleting = false; isForceDeleting = false; deleteError = null },
            onConfirm = {
                isDeleting = true
                deleteError = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            ApiClient.delete("/api/warehouses/$warehouseId")
                        }
                        showDeleteConfirm = false
                        onBack()
                    } catch (e: Exception) {
                        isDeleting = false
                        deleteError = e.message ?: "O'chirishda xatolik yuz berdi"
                        snackbarHostState.showSnackbar(deleteError ?: "O'chirishda xatolik yuz berdi")
                    }
                }
            },
            onForceConfirm = {
                isForceDeleting = true
                deleteError = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            ApiClient.delete("/api/warehouses/$warehouseId/force")
                        }
                        showDeleteConfirm = false
                        onBack()
                    } catch (e: Exception) {
                        isForceDeleting = false
                        deleteError = e.message ?: "Forsmajor o'chirishda xatolik yuz berdi"
                        snackbarHostState.showSnackbar(deleteError ?: "Forsmajor o'chirishda xatolik yuz berdi")
                    }
                }
            },
        )
    }
}

// ─── Stock Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun StockTab(
    lines: List<StockLine>,
    total: Int,
    page: Int,
    pageSize: Int,
    search: String,
    onSearch: (String) -> Unit,
    onPage: (Int) -> Unit,
    onPagePrev: () -> Unit,
    onPageNext: () -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onTransfer: () -> Unit,
    onAdjust: (StockLine) -> Unit,
    onEditMinStock: (StockLine) -> Unit,
) {
    var pageSizeExpanded by remember { mutableStateOf(false) }
    val filtered = if (search.isBlank()) lines else lines.filter { it.productName.contains(search, true) }
    val totalPages = if (total == 0) 1 else (total + pageSize - 1) / pageSize

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Search + button row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                placeholder = { Text("Mahsulot nomi yoki SKU bo'yicha qidirish...", fontSize = 13.sp) },
                modifier = Modifier.width(320.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            Button(
                onClick = onTransfer,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Stok o'tkazish", fontWeight = FontWeight.SemiBold)
            }
        }

        // Table
        Surface(
            Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MexaWarehouseColors.surfaceLowest,
        ) {
            Column {
                // Header
                Row(
                    Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    TableHead("MAHSULOT NOMI", Modifier.weight(2.5f))
                    TableHead("UMUMIY STOK", Modifier.weight(1f))
                    TableHead("BAND QILINGAN", Modifier.weight(1f))
                    TableHead("MAVJUD", Modifier.weight(1f))
                    TableHead("MINIMAL STOK", Modifier.weight(1f))
                    TableHead("HAJM (m³)", Modifier.weight(1f))
                    TableHead("HOLAT", Modifier.weight(0.8f))
                    TableHead("AMALLAR", Modifier.weight(0.6f))
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("Mahsulot topilmadi", color = MexaWarehouseColors.textMuted)
                    }
                } else {
                    Column {
                        filtered.forEachIndexed { idx, line ->
                            StockRow(line = line, onAdjust = onAdjust, onEditMinStock = onEditMinStock)
                            if (idx < filtered.lastIndex) {
                                HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                            }
                        }
                    }
                }
                // Pagination (Products sahifasidagi dizayn)
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val from = if (total == 0) 0 else (page - 1) * pageSize + 1
                    val to = minOf(page * pageSize, total)
                    Text(
                        "$from - $to / $total ta mahsulot",
                        fontSize = 13.sp,
                        color = MexaWarehouseColors.textMuted,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("QATORLAR:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                            Box {
                                Surface(
                                    onClick = { pageSizeExpanded = true },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                                    color = MexaWarehouseColors.surfaceLowest,
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(pageSize.toString(), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, fontWeight = FontWeight.Medium)
                                        Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(14.dp), tint = MexaWarehouseColors.textMuted)
                                    }
                                }
                                DropdownMenu(
                                    expanded = pageSizeExpanded,
                                    onDismissRequest = { pageSizeExpanded = false },
                                ) {
                                    listOf(10, 20, 50, 100).forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s.toString(), fontSize = 13.sp) },
                                            onClick = { onPageSizeChange(s); pageSizeExpanded = false },
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StockPageNavBtn("⏮", page > 1) { onFirst() }
                            StockPageNavBtn("‹", page > 1) { onPagePrev() }

                            val half = 2
                            val start = (page - 1 - half).coerceAtLeast(0)
                            val end = (page - 1 + half).coerceAtMost(totalPages - 1)

                            if (start > 0) {
                                StockPageNumBtn(1, page == 1) { onPage(1) }
                                if (start > 1) StockEllipsisLabel()
                            }
                            for (p in start..end) {
                                StockPageNumBtn(p + 1, p + 1 == page) { onPage(p + 1) }
                            }
                            if (end < totalPages - 1) {
                                if (end < totalPages - 2) StockEllipsisLabel()
                                StockPageNumBtn(totalPages, page == totalPages) { onPage(totalPages) }
                            }

                            StockPageNavBtn("›", page < totalPages) { onPageNext() }
                            StockPageNavBtn("⏭", page < totalPages) { onLast() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockPageNavBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(
                1.dp,
                if (enabled) MexaWarehouseColors.outlineVariant else MexaWarehouseColors.borderSubtle,
                RoundedCornerShape(6.dp),
            )
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, color = if (enabled) MexaWarehouseColors.textPrimary else MexaWarehouseColors.textMuted)
    }
}

@Composable
private fun StockPageNumBtn(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(if (selected) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.surfaceLowest, RoundedCornerShape(6.dp))
            .border(1.dp, if (selected) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.outlineVariant, RoundedCornerShape(6.dp))
            .then(if (!selected) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MexaWarehouseColors.textPrimary,
        )
    }
}

@Composable
private fun StockEllipsisLabel() {
    Text("…", fontSize = 13.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(horizontal = 2.dp))
}

@Composable
private fun StockRow(line: StockLine, onAdjust: (StockLine) -> Unit, onEditMinStock: (StockLine) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(2.5f)) {
            Text(line.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ID: ${line.productId.take(8)}...", fontSize = 11.sp, color = MexaWarehouseColors.textCaption)
        }
        Text("${formatNum(line.quantity.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
        Text("${formatNum(line.reserved.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        Text(
            "${formatNum(line.available.toInt())} dona",
            Modifier.weight(1f),
            fontSize = 13.sp,
            color = if (line.isLow) Color(0xFFDC2626) else MexaWarehouseColors.textPrimary,
            fontWeight = if (line.isLow) FontWeight.SemiBold else FontWeight.Normal,
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${formatNum(line.minStock.toInt())} dona",
                fontSize = 13.sp,
                color = MexaWarehouseColors.textPrimary,
            )
            if (line.minStockOverride != null) {
                Box(
                    Modifier.background(Color(0xFF6366F1).copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("ombor", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                }
            }
        }
        Text("${formatVolume(line.volumeM3)}", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        Box(Modifier.weight(0.8f)) {
            if (line.statusOk) {
                Icon(Icons.Default.CheckCircle, null, tint = MexaWarehouseColors.statusActiveFg, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.weight(0.6f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = { onEditMinStock(line) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, null, tint = Color(0xFF6366F1), modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = { onAdjust(line) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Settings, null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ─── Low Stock Tab ─────────────────────────────────────────────────────────────

@Composable
private fun LowStockTab(alerts: List<StockLine>) {
    Surface(
        Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                TableHead("MAHSULOT NOMI", Modifier.weight(2f))
                TableHead("MAVJUD STOK", Modifier.weight(1f))
                TableHead("BAND QILINGAN", Modifier.weight(1f))
                TableHead("MINIMAL STOK", Modifier.weight(1f))
            }
            if (alerts.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MexaWarehouseColors.statusActiveFg)
                        Text("Barcha mahsulotlar yetarli miqdorda", color = MexaWarehouseColors.textMuted)
                    }
                }
            } else {
                alerts.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Text(line.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                        }
                        Text("${formatNum(line.available.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                        Text("${formatNum(line.reserved.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        Text("${formatNum(line.minStock.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.5f)))
                }
            }
        }
    }
}

// ─── History Tab ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    rows: List<MovementRow>,
    total: Int,
    page: Int,
    pageSize: Int,
    onPageSizeChange: (Int) -> Unit,
    onPage: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
) {
    var pageSizeExpanded by remember { mutableStateOf(false) }
    val totalPages = if (total == 0) 1 else (total + pageSize - 1) / pageSize
    Surface(
        Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp)) {
                TableHead("MAHSULOT", Modifier.weight(2f))
                TableHead("HARAKAT TURI", Modifier.weight(1.5f))
                TableHead("MIQDOR", Modifier.weight(1f))
                TableHead("XODIM", Modifier.weight(1f))
                TableHead("SANA", Modifier.weight(1.2f))
            }
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Harakat tarixi bo'sh", color = MexaWarehouseColors.textMuted)
                }
            } else {
                rows.forEach { row ->
                    val (typeColor, typeLabel) = movementLabel(row.movementType)
                    val (statusColor, statusLabel) = movementStatusLabel(row.status)
                    val isSignedType = row.movementType == "ADJUSTMENT" || row.movementType == "INVENTORY"
                    val (qtyText, qtyColor) = when {
                        row.quantity < 0 -> "-${formatNum((-row.quantity).toInt())}" to Color(0xFFDC2626)
                        isSignedType -> "+${formatNum(row.quantity.toInt())}" to Color(0xFF16A34A)
                        else -> formatNum(row.quantity.toInt()) to MexaWarehouseColors.textPrimary
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.productName, Modifier.weight(2f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = typeColor)
                            }
                            if (statusLabel != null) {
                                Box(Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                }
                            }
                        }
                        Text("$qtyText dona", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = qtyColor)
                        Text(row.actorUsername.ifEmpty { "—" }, Modifier.weight(1f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        Text(row.createdAt.ifEmpty { "—" }, Modifier.weight(1.2f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                }
            }
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Jami $total ta harakat", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("QATORLAR:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                        Box {
                            Surface(
                                onClick = { pageSizeExpanded = true },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                                color = MexaWarehouseColors.surfaceLowest,
                                modifier = Modifier.height(34.dp),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(pageSize.toString(), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, fontWeight = FontWeight.Medium)
                                    Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(14.dp), tint = MexaWarehouseColors.textMuted)
                                }
                            }
                            DropdownMenu(
                                expanded = pageSizeExpanded,
                                onDismissRequest = { pageSizeExpanded = false },
                            ) {
                                listOf(10, 20, 50, 100).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.toString(), fontSize = 13.sp) },
                                        onClick = { onPageSizeChange(s); pageSizeExpanded = false },
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StockPageNavBtn("⏮", page > 1) { onFirst() }
                        StockPageNavBtn("‹", page > 1) { onPrev() }

                        val half = 2
                        val start = (page - 1 - half).coerceAtLeast(0)
                        val end = (page - 1 + half).coerceAtMost(totalPages - 1)

                        if (start > 0) {
                            StockPageNumBtn(1, page == 1) { onFirst() }
                            if (start > 1) StockEllipsisLabel()
                        }
                        for (p in start..end) {
                            StockPageNumBtn(p + 1, p + 1 == page) { onPage(p + 1) }
                        }
                        if (end < totalPages - 1) {
                            if (end < totalPages - 2) StockEllipsisLabel()
                            StockPageNumBtn(totalPages, page == totalPages) { onLast() }
                        }

                        StockPageNavBtn("›", page < totalPages) { onNext() }
                        StockPageNavBtn("⏭", page < totalPages) { onLast() }
                    }
                }
            }
        }
    }
}

// ─── Lots Tab ──────────────────────────────────────────────────────────────────

private data class LotGroup(
    val key: String,
    val lines: List<StockLotRow>,
)

private fun groupLotsByPurchase(lots: List<StockLotRow>): List<LotGroup> {
    val byPurchase = LinkedHashMap<String, MutableList<StockLotRow>>()
    for (lot in lots.sortedWith(compareBy { it.receivedDate })) {
        val key = lot.purchaseId.ifEmpty { lot.id }
        byPurchase.getOrPut(key) { mutableListOf() }.add(lot)
    }
    return byPurchase.map { (key, lines) -> LotGroup(key, lines) }
}

@Composable
private fun LotsTab(
    lots: List<StockLotRow>,
    isSuperAdmin: Boolean,
    onCancelLot: (String) -> Unit,
) {
    val groups = remember(lots) { groupLotsByPurchase(lots) }
    val lotCount = groups.size
    val totalValue = lots.sumOf { it.totalCost }
    var cancelTarget by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 860.dp
        val lotW = if (compact) 3.2f else 2.2f
        val numW = if (compact) 1.1f else 1.5f
        val actW = 0.6f
        val nameLines = if (compact) 2 else 1

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBottomCard(Modifier.weight(1f)) {
                    Text("JAMI LOTLAR (KIRIMLAR)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatNum(lotCount), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                        Text("ta", fontSize = 14.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                StatBottomCard(Modifier.weight(1f)) {
                    Text("LOTLAR TANNARXI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${formatMoney(totalValue)} so'm",
                        fontSize = if (compact) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = MexaWarehouseColors.surfaceLowest,
            ) {
                Column {
                    Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        TableHead("LOT (KIRIM)", Modifier.weight(lotW))
                        TableHead("MIQDOR", Modifier.weight(numW))
                        TableHead("BIRLIK TANNARXI", Modifier.weight(numW))
                        TableHead("UMUMIY TANNARX", Modifier.weight(numW))
                        TableHead("KIRIM SANASI", Modifier.weight(numW))
                        TableHead(if (isSuperAdmin) "AMALLAR" else "", Modifier.weight(actW))
                    }
                    if (groups.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Lotlar topilmadi — hali kirim qayd etilmagan", color = MexaWarehouseColors.textMuted)
                        }
                    } else {
                        LazyColumn(Modifier.height(minOf(groups.size * 76 + 96, if (compact) 360 else 460).dp)) {
                            itemsIndexed(groups) { gIdx, group ->
                                val groupQty = group.lines.sumOf { it.quantity }
                                val groupValue = group.lines.sumOf { it.totalCost }
                                val purchaseId = group.lines.firstOrNull()?.purchaseId.orEmpty()
                                Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(Modifier.weight(lotW), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("#${gIdx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.indigoAccent)
                                        Text("Kirim", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("${formatNum(groupQty.toInt())}", Modifier.weight(numW), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("—", Modifier.weight(numW), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                                    Text("${formatMoney(groupValue)}", Modifier.weight(numW), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(group.lines.first().receivedDate.ifEmpty { "—" }, Modifier.weight(numW), fontSize = 12.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Box(Modifier.weight(actW), contentAlignment = Alignment.CenterEnd) {
                                        if (isSuperAdmin && purchaseId.isNotEmpty()) {
                                            IconButton(
                                                onClick = { cancelTarget = purchaseId },
                                                modifier = Modifier.size(30.dp),
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Kirimni bekor qilish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                                group.lines.forEach { lot ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("• ${lot.productName}", Modifier.weight(lotW), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, maxLines = nameLines, overflow = TextOverflow.Ellipsis)
                                        Text("${formatNum(lot.quantity.toInt())}", Modifier.weight(numW), fontSize = 12.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${formatMoney(lot.unitCost)}", Modifier.weight(numW), fontSize = 12.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${formatMoney(lot.totalCost)}", Modifier.weight(numW), fontSize = 12.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.weight(numW))
                                        Spacer(Modifier.weight(actW))
                                    }
                                }
                                if (gIdx < groups.lastIndex) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    cancelTarget?.let { purchaseId ->
        Dialog(onDismissRequest = { cancelTarget = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MexaWarehouseColors.surfaceLowest,
                tonalElevation = 4.dp,
                modifier = Modifier.widthIn(min = 380.dp, max = 520.dp),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Kirimni bekor qilish", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text(
                        "Ushbu kirim (lot) bekor qilinadi va tegishli stock miqdori qaytariladi. Bu amalni tasdiqlaysizmi?",
                        fontSize = 13.sp,
                        color = MexaWarehouseColors.textMuted,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(onClick = { cancelTarget = null }, shape = RoundedCornerShape(8.dp)) {
                            Text("Bekor qilish")
                        }
                        Button(
                            onClick = {
                                cancelTarget = null
                                onCancelLot(purchaseId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Tasdiqlash", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Profit Tab ────────────────────────────────────────────────────────────────

@Composable
private fun ProfitTab(lines: List<ProfitLine>, totalRevenue: Double, totalCost: Double, totalProfit: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBottomCard(Modifier.weight(1f)) {
                Text("DAROMAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(10.dp))
                Text("${formatMoney(totalRevenue)} so'm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
            }
            StatBottomCard(Modifier.weight(1f)) {
                Text("XARAJAT (COGS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(10.dp))
                Text("${formatMoney(totalCost)} so'm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            }
            StatBottomCard(Modifier.weight(1f)) {
                Text("FOYDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "${formatMoney(totalProfit)} so'm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                )
            }
        }

        Surface(
            Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = MexaWarehouseColors.surfaceLowest,
        ) {
            Column {
                Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    TableHead("MAHSULOT", Modifier.weight(2.2f))
                    TableHead("SOTILGAN", Modifier.weight(0.9f))
                    TableHead("SOTUV NARXI", Modifier.weight(1f))
                    TableHead("DAROMAD", Modifier.weight(1.1f))
                    TableHead("TANNARX", Modifier.weight(1.1f))
                    TableHead("FOYDA", Modifier.weight(1.1f))
                }
                if (lines.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("Foyda ma'lumotlari yo'q — hali sotuvlar qayd etilmagan", color = MexaWarehouseColors.textMuted)
                    }
                } else {
                    lines.forEach { line ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(line.productName, Modifier.weight(2.2f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatNum(line.soldQuantity.toInt())} dona", Modifier.weight(0.9f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            Text("${formatMoney(line.salePrice)}", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                            Text("${formatMoney(line.revenue)}", Modifier.weight(1.1f), fontSize = 13.sp, color = Color(0xFF16A34A))
                            Text("${formatMoney(line.cost)}", Modifier.weight(1.1f), fontSize = 13.sp, color = Color(0xFFDC2626))
                            Text(
                                "${formatMoney(line.profit)}",
                                Modifier.weight(1.1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (line.profit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                    }
                }
            }
        }
    }
}

// ─── Admins Tab ────────────────────────────────────────────────────────────────

@Composable
private fun AdminsTab(admins: List<AdminInfo>) {
    Surface(
        Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mas'ul adminlar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
            if (admins.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Hech qanday admin tayinlanmagan", color = MexaWarehouseColors.textMuted)
                }
            } else {
                admins.forEach { admin ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).background(MexaWarehouseColors.statIndigoBg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                admin.username.ifEmpty { "Admin ID: ${admin.id.take(8)}..." },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MexaWarehouseColors.textPrimary,
                            )
                            if (admin.email.isNotEmpty()) {
                                Text(admin.email, fontSize = 11.sp, color = MexaWarehouseColors.textCaption)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(
    orders: List<WarehouseOrder>,
    total: Int,
    page: Int,
    pageSize: Int,
    onPageSizeChange: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onPageChange: ((Int) -> Unit)? = null,
) {
    val totalPages = if (total == 0) 1 else (total + pageSize - 1) / pageSize
    Surface(
        Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp)) {
                TableHead("BUYURTMA", Modifier.weight(1.5f))
                TableHead("HOLAT", Modifier.weight(1f))
                TableHead("SUMMA", Modifier.weight(1f))
                TableHead("MAHSULOT", Modifier.weight(0.8f))
                TableHead("SANA", Modifier.weight(1.2f))
            }
            if (orders.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Bu omborga kelib tushgan buyurtma yo'q", color = MexaWarehouseColors.textMuted)
                }
            } else {
                orders.forEach { order ->
                    val (statusColor, statusLabel) = orderStatusLabel(order.status)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(order.orderNumber, Modifier.weight(1.5f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.weight(1f)) {
                            Box(Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                        Text("${formatMoney(order.totalAmount)} ${order.currency}", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                        Text("${order.itemCount} tur", Modifier.weight(0.8f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        Text(order.createdAt.ifEmpty { "—" }, Modifier.weight(1.2f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                }
            }
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val from = if (total == 0) 0 else (page - 1) * pageSize + 1
                val to = minOf(page * pageSize, total)
                Text(
                    "$from - $to / $total ta buyurtma",
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // QATORLAR selector — boshqa jadvallar bilan bir xil
                    var sizeExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "QATORLAR:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.textMuted,
                        )
                        androidx.compose.foundation.layout.Box {
                            Surface(
                                onClick = { sizeExpanded = true },
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                                color = MexaWarehouseColors.surfaceLowest,
                                modifier = Modifier.height(34.dp),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        pageSize.toString(),
                                        fontSize = 13.sp,
                                        color = MexaWarehouseColors.textPrimary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Icon(
                                        androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MexaWarehouseColors.textMuted,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = sizeExpanded,
                                onDismissRequest = { sizeExpanded = false },
                            ) {
                                listOf(5, 10, 20, 50).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.toString(), fontSize = 13.sp) },
                                        onClick = { onPageSizeChange(s); sizeExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PageNavBtn("⏮", page > 1) { onFirst() }
                        PageNavBtn("‹", page > 1) { onPrev() }
                        val half = 2
                        val start = (page - half).coerceAtLeast(1)
                        val end = (page + half).coerceAtMost(totalPages)
                        if (start > 1) {
                            PageNumBtn(1, page == 1) { onPageChange?.invoke(1) ?: onFirst() }
                            if (start > 2) EllipsisLabel()
                        }
                        for (p in start..end) {
                            PageNumBtn(p, p == page) { onPageChange?.invoke(p) ?: run { /* fallback */ } }
                        }
                        if (end < totalPages) {
                            if (end < totalPages - 1) EllipsisLabel()
                            PageNumBtn(totalPages, page == totalPages) { onPageChange?.invoke(totalPages) ?: onLast() }
                        }
                        PageNavBtn("›", page < totalPages) { onNext() }
                        PageNavBtn("⏭", page < totalPages) { onLast() }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageNavBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(
                1.dp,
                if (enabled) MexaWarehouseColors.outlineVariant else MexaWarehouseColors.borderSubtle,
                RoundedCornerShape(6.dp),
            )
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MexaWarehouseColors.textPrimary else MexaWarehouseColors.textMuted.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun PageNumBtn(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.surfaceLowest,
                RoundedCornerShape(6.dp),
            )
            .border(
                1.dp,
                if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.outlineVariant,
                RoundedCornerShape(6.dp),
            )
            .then(if (!selected) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MexaWarehouseColors.textPrimary,
        )
    }
}

@Composable
private fun EllipsisLabel() {
    Text(
        "…",
        fontSize = 13.sp,
        color = MexaWarehouseColors.textMuted,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

private fun orderStatusLabel(status: String): Pair<Color, String> = when (status.uppercase()) {
    "PENDING" -> Color(0xFFD97706) to "KUTILMOQDA"
    "CONFIRMED" -> Color(0xFF2563EB) to "TASDIQLANGAN"
    "PROCESSING" -> Color(0xFF7C3AED) to "JARAYONDA"
    "SHIPPED" -> Color(0xFF0284C7) to "YUBORILGAN"
    "DELIVERED" -> Color(0xFF16A34A) to "YETKAZILGAN"
    "CANCELLED" -> Color(0xFFDC2626) to "BEKOR QILINGAN"
    "REFUNDED" -> Color(0xFF6B7280) to "QAYTARILGAN"
    else -> MexaWarehouseColors.textMuted to status
}

// ─── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun StatBottomCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier.border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
internal fun TableHead(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MexaWarehouseColors.textMuted,
        letterSpacing = 0.3.sp,
    )
}

internal fun formatNum(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun formatVolume(value: Double): String {
    if (value <= 0) return "0"
    return if (value >= 100) String.format("%.1f", value) else String.format("%.2f", value)
}

// ─── Bottom metrics ────────────────────────────────────────────────────────────

private data class BottomMetrics(
    val currentUnits: Double,
    val stockedProducts: Int,
    val visibleProducts: Int,
) {
    val coveragePct: Int
        get() = if (stockedProducts <= 0) 0 else minOf(100, visibleProducts * 100 / stockedProducts)
}

private fun computeBottomMetrics(lines: List<StockLine>, stockTotal: Int): BottomMetrics =
    BottomMetrics(
        currentUnits = lines.sumOf { it.quantity },
        stockedProducts = stockTotal,
        visibleProducts = lines.size,
    )

private fun formatMoney(value: Double): String {
    val rounded = String.format("%.0f", value)
    return rounded.reversed().chunked(3).joinToString(" ").reversed()
}

private fun movementLabel(type: String): Pair<Color, String> = when (type) {
    "PURCHASE_IN"  -> Color(0xFF16A34A) to "KIRIM"
    "SALES_OUT"    -> Color(0xFFDC2626) to "CHIQIM"
    "TRANSFER_IN"  -> Color(0xFF2563EB) to "TRANSFER +"
    "TRANSFER_OUT" -> Color(0xFF7C3AED) to "TRANSFER -"
    "ADJUSTMENT"   -> Color(0xFFF59E0B) to "TUZATISH"
    "RETURN_IN"    -> Color(0xFF0891B2) to "QAYTIM"
    "INVENTORY"    -> Color(0xFF475569) to "INVENTAR"
    else           -> Color(0xFF64748B) to type
}

private fun movementStatusLabel(status: String): Pair<Color, String?> = when (status) {
    "PENDING"  -> Color(0xFFD97706) to "KUTILMOQDA"
    "CANCELLED" -> Color(0xFFDC2626) to "BEKOR"
    "CONFIRMED", "" -> Color(0xFF16A34A) to null
    else -> Color(0xFF64748B) to status
}

package mexa.club.desktop_app.market.ui.warehouses

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import mexa.club.desktop_app.market.ui.components.PaginationBar
import mexa.club.desktop_app.market.ui.orders.OrderDetailDialog
import mexa.club.desktop_app.market.ui.products.ProductRowData
import mexa.club.desktop_app.market.ui.products.ProductViewDialog
import mexa.club.desktop_app.market.ui.products.SimpleDatePickerDialog
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
    val reason: String = "",
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

private data class WarehouseCourierInfo(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val active: Boolean,
    val currentDeliveries: Int,
)

/** Kuryer bo'yicha yetkazmalar statistikasi (delivery-service'dan). */
private data class CourierStats(
    val total: Long = 0,
    val active: Long = 0,
    val delivered: Long = 0,
    val failed: Long = 0,
    val returned: Long = 0,
    val today: Long = 0,
)

/** Kuryerning bitta yetkazmasi (buyurtma holati bilan). */
private data class CourierDeliveryRow(
    val id: String,
    val orderId: String,
    val status: String,
    val recipientName: String,
    val deliveryAddress: String,
    val createdAt: String,
    val failReason: String,
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
    val couriers: List<WarehouseCourierInfo> = emptyList(),
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
    val courierStats: Map<String, CourierStats> = emptyMap(),
)

private enum class DetailTab { STOCK, LOW_STOCK, HISTORY, LOTS, PROFIT, COURIERS, ADMINS, INVENTORY, RETURNS, ORDERS }

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
    // Client-side sahifalash uchun tab holatlari (PaginationBar)
    var lowStockPage by remember { mutableStateOf(0) }
    var lotsPage by remember { mutableStateOf(0) }
    var profitPage by remember { mutableStateOf(0) }
    var couriersPage by remember { mutableStateOf(0) }
    var adminsPage by remember { mutableStateOf(0) }
    var stockSearch by remember { mutableStateOf("") }
    // Harakatlar tarixi: qidiruv + filtrlar (search, movementType, dateFrom, dateTo)
    var histSearch by remember { mutableStateOf("") }
    var histType by remember { mutableStateOf("") }
    var histDateFrom by remember { mutableStateOf("") }
    var histDateTo by remember { mutableStateOf("") }
    // Lotlar: qidiruv + sana filtri (client-side)
    var lotSearch by remember { mutableStateOf("") }
    var lotDateFrom by remember { mutableStateOf("") }
    var lotDateTo by remember { mutableStateOf("") }
    var showAssignAdmin by remember { mutableStateOf(false) }
    var showAssignCourier by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showCreatePurchase by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var adjustTarget by remember { mutableStateOf<StockLine?>(null) }
    var showEditMinStock by remember { mutableStateOf(false) }
    var editMinTarget by remember { mutableStateOf<StockLine?>(null) }
    var viewingProduct by remember { mutableStateOf<ProductRowData?>(null) }
    var viewingOrder by remember { mutableStateOf<String?>(null) }
    var viewingMovement by remember { mutableStateOf<MovementRow?>(null) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isForceDeleting by remember { mutableStateOf(false) }
    var pageSize by remember { mutableStateOf(10) }
    var ordersPage by remember { mutableStateOf(1) }
    var loadedTabs by remember { mutableStateOf(setOf(DetailTab.STOCK)) }
    var visitedTabs by remember { mutableStateOf(setOf(DetailTab.STOCK)) }
    // Filtr/qidiruv/sahifa o'zgarishida eski kontent saqlanib, fonda yangilanadi:
    // refreshCount > 0 bo'lganda faqat yupqa progress bar ko'rinadi (silkinishsiz).
    var refreshCount by remember { mutableStateOf(0) }
    // Birinchi marta yuklangan tablar — keyingi reload'lar kontentni almashtirmaydi.
    var loadedOnce by remember { mutableStateOf(setOf<DetailTab>()) }
    // Real-time: har bir SSE/pfon yangilashida o'zgaradi → Inventarizatsiya/Qaytarmalar
    // tablari ham o'z ma'lumotini qayta yuklaydi (ular alohida fetch qiladi).
    var syncTick by remember { mutableStateOf(0) }

    suspend fun fetchShared(search: String = stockSearch, sPage: Int = 1): DetailData = withContext(Dispatchers.IO) {
        val searchQ = search.trim()

        val stockText = runCatching {
            val url = buildString {
                append("/api/warehouses/$warehouseId/stock?page=${sPage - 1}&size=$pageSize")
                if (searchQ.isNotEmpty()) append("&search=${java.net.URLEncoder.encode(searchQ, "UTF-8")}")
            }
            ApiClient.get(url)
        }.getOrElse { throw it }

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
                    val histUrl = buildString {
                        append("/api/warehouses/$warehouseId/stock/history?page=${hPage - 1}&size=$pageSize")
                        val q = histSearch.trim()
                        if (q.isNotEmpty()) append("&search=${java.net.URLEncoder.encode(q, "UTF-8")}")
                        if (histType.isNotEmpty()) append("&movementType=$histType")
                        val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
                        if (histDateFrom.trim().matches(datePattern)) append("&dateFrom=${histDateFrom.trim()}T00:00:00")
                        if (histDateTo.trim().matches(datePattern)) append("&dateTo=${histDateTo.trim()}T23:59:59")
                    }
                    ApiClient.get(histUrl)
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
                            reason = o.stringField("reason"),
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
            DetailTab.COURIERS -> {
                val couriers = runCatching {
                    val text = ApiClient.get("/api/admin/couriers/by-warehouse?warehouseId=$warehouseId")
                    val arr = (ApiClient.parseJsonObject(text)?.get("data") as? JsonArray)
                        ?: JsonArray(emptyList())
                    arr.mapNotNull { el ->
                        val c = el as? JsonObject ?: return@mapNotNull null
                        val id = c.stringField("id").ifEmpty { return@mapNotNull null }
                        WarehouseCourierInfo(
                            id = id,
                            userId = c.stringField("userId"),
                            name = c.stringField("name"),
                            phone = c.stringField("phone"),
                            active = c.stringField("active").toBooleanStrictOrNull() ?: true,
                            currentDeliveries = c.stringField("currentDeliveries").toIntOrNull() ?: 0,
                        )
                    }
                }.getOrElse { emptyList() }
                // Har bir kuryer uchun yetkazmalar statistikasi (jami/faol/yetkazilgan/bugun)
                val courierStats = runCatching {
                    val text = ApiClient.get("/api/admin/couriers/stats/by-warehouse?warehouseId=$warehouseId")
                    val arr = (ApiClient.parseJsonObject(text)?.get("data") as? JsonArray)
                        ?: JsonArray(emptyList())
                    arr.mapNotNull { el ->
                        val s = el as? JsonObject ?: return@mapNotNull null
                        val cid = s.stringField("courierId").ifEmpty { return@mapNotNull null }
                        cid to CourierStats(
                            total = s.stringField("total").toLongOrNull() ?: 0L,
                            active = s.stringField("active").toLongOrNull() ?: 0L,
                            delivered = s.stringField("delivered").toLongOrNull() ?: 0L,
                            failed = s.stringField("failed").toLongOrNull() ?: 0L,
                            returned = s.stringField("returned").toLongOrNull() ?: 0L,
                            today = s.stringField("today").toLongOrNull() ?: 0L,
                        )
                    }.toMap()
                }.getOrElse { emptyMap() }
                shared.copy(couriers = couriers, courierStats = courierStats)
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
                    couriers = old.couriers,
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
                    couriers = old.couriers,
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
                    couriers = old.couriers,
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
                    couriers = old.couriers,
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
                    couriers = old.couriers,
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
                    couriers = old.couriers,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
            DetailTab.COURIERS -> {
                merged = merged.copy(
                    movements = old.movements,
                    movementsTotal = old.movementsTotal,
                    lots = old.lots,
                    profitLines = old.profitLines,
                    totalRevenue = old.totalRevenue,
                    totalCost = old.totalCost,
                    totalProfit = old.totalProfit,
                    admins = old.admins,
                    couriers = fresh.couriers,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                    courierStats = fresh.courierStats,
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
                    couriers = old.couriers,
                    orders = old.orders,
                    ordersTotal = old.ordersTotal,
                )
            }
        }
        return merged
    }

    /**
     * @param silent fon yangilashi (SSE/polling/mutatsiya): spinner chiqmaydi,
     *   tab unmount bo'lmaydi, xatolikda eski ma'lumot saqlanib qoladi.
     *   Har qanday reload'dan keyin faol tab yangilangan bo'ladi, qolgan tablar
     *   keyingi ochilganda qayta yuklanadi — shu bilan barcha qisimlar real-time.
     *
     *   Birinchi yuklanish (tab hali loadedOnce'da bo'lmasa) — to'liq spinner.
     *   Qolgan hollarda (filter, qidiruv, sahifa) kontent joyida turadi,
     *   faqat refreshing indikatori yoqiladi — sahifa "silkinmaydi".
     */
    fun reload(sPage: Int = stockPage, hPage: Int = histPage, silent: Boolean = false) {
        scope.launch {
            if (silent && loading) return@launch
            val active = tab
            val blocking = !silent && active !in loadedOnce
            var ownsIndicator = false
            try {
                if (blocking) {
                    loading = true
                } else if (!silent) {
                    refreshCount++
                    ownsIndicator = true
                }
                val fresh = try {
                    if (active == DetailTab.STOCK) {
                        fetchShared(stockSearch, sPage)
                    } else {
                        fetchTabData(active, sPage, hPage)
                    }
                } catch (e: Exception) {
                    // Bloklanmaydigan yangilashda tarmoq xatosi → eski ma'lumot qolsin
                    if (!blocking) return@launch
                    DetailData()
                }
                data = mergeDetailData(data, fresh, active)
                loadedTabs = setOf(active)
                loadedOnce = loadedOnce + active
            } finally {
                if (ownsIndicator) refreshCount--
                else if (blocking) loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
        GatewayRealtimeHub.dashboardRefresh.drop(1).collect {
            syncTick++
            reload(silent = true)
        }
    }

    // SSE uzilib qolsa ham ma'lumot yangi qolsin: 30 soniyada bir jim fon yangilashi.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            syncTick++
            reload(silent = true)
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

    // Tarix filtrlari o'zgarsa → debounce bilan 1-sahifadan qayta yuklash
    val histFilterKey = "$histSearch|$histType|$histDateFrom|$histDateTo"
    var prevHistFilterKey by remember { mutableStateOf(histFilterKey) }
    LaunchedEffect(histFilterKey) {
        if (histFilterKey != prevHistFilterKey) {
            prevHistFilterKey = histFilterKey
            delay(400)
            histPage = 1
            reload()
        }
    }

    val bottomMetrics = computeBottomMetrics(data.stockLines, data.stockTotal)
    val refreshing = refreshCount > 0
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
                                            Icon(Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                            Text("Kuryer biriktirish", fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        actionsMenuExpanded = false
                                        showAssignCourier = true
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
                        DetailTab.COURIERS -> "Kuryerlar"
                        DetailTab.ADMINS -> "Adminlar"
                        DetailTab.INVENTORY -> "Inventarizatsiya"
                        DetailTab.RETURNS -> "Qaytarmalar"
                        DetailTab.ORDERS -> "Buyurtmalar"
                    }
                    val badge = when (t) {
                        DetailTab.LOW_STOCK -> if (data.lowAlerts.isNotEmpty()) data.lowAlerts.size.toString() else null
                        DetailTab.COURIERS -> if (data.couriers.isNotEmpty()) data.couriers.size.toString() else null
                        DetailTab.ORDERS -> if (data.ordersTotal > 0) data.ordersTotal.toString() else null
                        else -> null
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tab == t) MexaWarehouseColors.indigoAccent else Color.Transparent)
                            .clickable {
                                // Birinchi marta ochilsa — spinner; oldin ochilgan bo'lsa
                                // eski ko'rinish saqlanib, ma'lumot fonda yangilanadi.
                                val firstVisit = t !in visitedTabs
                                visitedTabs = visitedTabs + t
                                tab = t
                                if (t !in loadedTabs) reload(silent = !firstVisit)
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
                Box(Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing))) {
                    if (refreshing) {
                        // Yupqa progress bar — kontentni siqmaydi, tepada overlay bo'lib turadi
                        LinearProgressIndicator(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp),
                            color = MexaWarehouseColors.indigoAccent,
                            trackColor = Color.Transparent,
                        )
                    }
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
                        onViewProduct = { line ->
                            viewingProduct = ProductRowData(
                                id = line.productId,
                                name = line.productName,
                                barcode = "",
                                categoryName = "",
                                brandName = "",
                                unit = "dona",
                                active = true,
                                tags = emptyList(),
                            )
                        },
                    )
                    DetailTab.LOW_STOCK -> LowStockTab(
                        alerts = data.lowAlerts,
                        onAdjust = { line -> adjustTarget = line; showAdjust = true },
                        onEditMinStock = { line -> editMinTarget = line; showEditMinStock = true },
                        page = lowStockPage,
                        pageSize = pageSize,
                        onPage = { lowStockPage = it },
                        onPageSizeChange = { s -> pageSize = s; lowStockPage = 0 },
                    )
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
                        onView = { row -> viewingMovement = row },
                        search = histSearch,
                        onSearchChange = { histSearch = it },
                        typeFilter = histType,
                        onTypeFilterChange = { histType = it },
                        dateFrom = histDateFrom,
                        onDateFromChange = { histDateFrom = it },
                        dateTo = histDateTo,
                        onDateToChange = { histDateTo = it },
                    )
                    DetailTab.LOTS -> LotsTab(
                        lots = data.lots,
                        isSuperAdmin = AuthSession.isSuperAdmin,
                        page = lotsPage,
                        pageSize = pageSize,
                        onPage = { lotsPage = it },
                        onPageSizeChange = { s -> pageSize = s; lotsPage = 0 },
                        search = lotSearch,
                        onSearchChange = { lotSearch = it; lotsPage = 0 },
                        dateFrom = lotDateFrom,
                        onDateFromChange = { lotDateFrom = it; lotsPage = 0 },
                        dateTo = lotDateTo,
                        onDateToChange = { lotDateTo = it; lotsPage = 0 },
                        onCancelLot = { purchaseId, reason ->
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val enc = java.net.URLEncoder.encode(reason, "UTF-8")
                                        ApiClient.delete("/api/warehouses/$warehouseId/purchases/$purchaseId?reason=$enc")
                                    }
                                    reload(silent = true)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Lot bekor qilishda xatolik: ${e.message}")
                                }
                            }
                        },
                        onDeleteSingleLot = { lotId, reason ->
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val enc = java.net.URLEncoder.encode(reason, "UTF-8")
                                        ApiClient.delete("/api/warehouses/$warehouseId/stock/lots/$lotId?reason=$enc")
                                    }
                                    reload(silent = true)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Lot o'chirishda xatolik: ${e.message}")
                                }
                            }
                        },
                    )
                    DetailTab.PROFIT -> ProfitTab(
                        lines = data.profitLines,
                        totalRevenue = data.totalRevenue,
                        totalCost = data.totalCost,
                        totalProfit = data.totalProfit,
                        page = profitPage,
                        pageSize = pageSize,
                        onPage = { profitPage = it },
                        onPageSizeChange = { s -> pageSize = s; profitPage = 0 },
                    )
                    DetailTab.COURIERS -> WarehouseCouriersTab(
                        couriers = data.couriers,
                        stats = data.courierStats,
                        page = couriersPage,
                        pageSize = pageSize,
                        onPage = { couriersPage = it },
                        onPageSizeChange = { s -> pageSize = s; couriersPage = 0 },
                    )
                    DetailTab.ADMINS -> AdminsTab(
                        admins = data.admins,
                        page = adminsPage,
                        pageSize = pageSize,
                        onPage = { adminsPage = it },
                        onPageSizeChange = { s -> pageSize = s; adminsPage = 0 },
                        onRemove = { admin ->
                            scope.launch {
                                try {
                                    val remaining = data.admins.filter { it.id != admin.id }.map { it.id }
                                    val body = "{\"adminUserIds\":[${remaining.joinToString(",") { "\"$it\"" }}]}"
                                    withContext(Dispatchers.IO) {
                                        ApiClient.put("/api/warehouses/$warehouseId/admins", body)
                                    }
                                    reload(silent = true)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Adminni olib tashlashda xatolik: ${e.message}")
                                }
                            }
                        },
                    )
                    DetailTab.INVENTORY -> InventoryTab(
                        warehouseId = warehouseId,
                        onChanged = { syncTick++; reload(silent = true) },
                        syncKey = syncTick,
                    )
                    DetailTab.RETURNS -> ReturnsTab(
                        warehouseId = warehouseId,
                        onChanged = { syncTick++; reload(silent = true) },
                        syncKey = syncTick,
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
                        onView = { order -> viewingOrder = order.id },
                    )
                }
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
                reload(silent = true)
            },
        )
    }

    if (showAssignCourier) {
        AssignWarehouseCourierDialog(
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            onDismiss = { showAssignCourier = false },
            onSaved = { reload(silent = true) },
        )
    }

    if (showCreatePurchase) {
        PurchaseCreateDialog(
            warehouseId = warehouseId,
            pendingTransfers = data.pendingTransfers,
            onTransfersChanged = { reload(silent = true) },
            onDismiss = { showCreatePurchase = false },
            onSaved = {
                showCreatePurchase = false
                reload(silent = true)
            },
        )
    }

    viewingProduct?.let { product ->
        ProductViewDialog(
            product = product,
            onDismiss = { viewingProduct = null },
        )
    }

    viewingMovement?.let { row ->
        MovementDetailDialog(
            row = row,
            onDismiss = { viewingMovement = null },
        )
    }

    viewingOrder?.let { orderId ->
        OrderDetailDialog(
            orderId = orderId,
            onDismiss = { viewingOrder = null },
            onUpdated = {
                viewingOrder = null
                reload(silent = true)
            },
        )
    }

    if (showTransfer) {
        TransferStockDialog(
            warehouseId = warehouseId,
            onDismiss = { showTransfer = false },
            onSaved = {
                showTransfer = false
                reload(silent = true)
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
                reload(silent = true)
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
                    reload(silent = true)
                },
            )
        }
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
    onViewProduct: (StockLine) -> Unit = {},
) {
    val filtered = if (search.isBlank()) lines else lines.filter { it.productName.contains(search, true) }

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
                            StockRow(line = line, onAdjust = onAdjust, onEditMinStock = onEditMinStock, onViewProduct = onViewProduct)
                            if (idx < filtered.lastIndex) {
                                HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                PaginationBar(
                    page = (page - 1).coerceAtLeast(0),
                    pageSize = pageSize,
                    totalElements = total,
                    infoText = run {
                        val from = if (total == 0) 0 else (page - 1) * pageSize + 1
                        val to = minOf(page * pageSize, total)
                        "$from - $to / $total ta mahsulot"
                    },
                    onPageChange = { onPage(it + 1) },
                    onPageSizeChange = onPageSizeChange,
                )
            }
        }
    }
}

@Composable
private fun StockRow(
    line: StockLine,
    onAdjust: (StockLine) -> Unit,
    onEditMinStock: (StockLine) -> Unit,
    onViewProduct: (StockLine) -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onViewProduct(line) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(2.5f)) {
            Text(line.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.indigoAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun LowStockTab(
    alerts: List<StockLine>,
    onAdjust: (StockLine) -> Unit,
    onEditMinStock: (StockLine) -> Unit,
    page: Int = 0,
    pageSize: Int = 10,
    onPage: (Int) -> Unit = {},
    onPageSizeChange: (Int) -> Unit = {},
) {
    val totalPages = if (alerts.isEmpty()) 1 else ((alerts.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(0, totalPages - 1)
    val paged = alerts.drop(safePage * pageSize).take(pageSize)
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
                TableHead("AMALLAR", Modifier.weight(0.6f))
            }
            if (alerts.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MexaWarehouseColors.statusActiveFg)
                        Text("Barcha mahsulotlar yetarli miqdorda", color = MexaWarehouseColors.textMuted)
                    }
                }
            } else {
                paged.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Text(line.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                        }
                        Text("${formatNum(line.available.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                        Text("${formatNum(line.reserved.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        Text("${formatNum(line.minStock.toInt())} dona", Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        Row(Modifier.weight(0.6f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = { onEditMinStock(line) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Edit, "Minimal stokni tahrirlash", tint = Color(0xFF6366F1), modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = { onAdjust(line) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Settings, "Stokni tuzatish", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.5f)))
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                val fromIdx = safePage * pageSize + 1
                val toIdx = minOf((safePage + 1) * pageSize, alerts.size)
                PaginationBar(
                    page = safePage,
                    pageSize = pageSize,
                    totalElements = alerts.size,
                    infoText = "$fromIdx - $toIdx / ${alerts.size} ta mahsulot",
                    onPageChange = onPage,
                    onPageSizeChange = onPageSizeChange,
                )
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
    onView: (MovementRow) -> Unit = {},
    search: String = "",
    onSearchChange: (String) -> Unit = {},
    typeFilter: String = "",
    onTypeFilterChange: (String) -> Unit = {},
    dateFrom: String = "",
    onDateFromChange: (String) -> Unit = {},
    dateTo: String = "",
    onDateToChange: (String) -> Unit = {},
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val hasActiveFilter = search.isNotBlank() || typeFilter.isNotEmpty() || dateFrom.isNotBlank() || dateTo.isNotBlank()
    Surface(
        Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column {
            // ── Qidiruv + filtrlar paneli ──────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Mahsulot, xodim yoki sabab bo'yicha...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.textMuted) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                Box {
                    OutlinedButton(onClick = { typeMenuExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            if (typeFilter.isEmpty()) "Barcha turlar" else movementLabel(typeFilter).second,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (typeFilter.isEmpty()) MexaWarehouseColors.textPrimary else movementLabel(typeFilter).first,
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.textMuted)
                    }
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Barcha turlar", fontSize = 13.sp) },
                            onClick = { onTypeFilterChange(""); typeMenuExpanded = false },
                        )
                        HISTORY_MOVEMENT_TYPES.forEach { t ->
                            val (typeColor, typeLabel) = movementLabel(t)
                            DropdownMenuItem(
                                text = { Text(typeLabel, fontSize = 13.sp, color = typeColor, fontWeight = FontWeight.SemiBold) },
                                onClick = { onTypeFilterChange(t); typeMenuExpanded = false },
                            )
                        }
                    }
                }
                FilterDateField(value = dateFrom, onChange = onDateFromChange, placeholder = "Dan: tanlash...")
                FilterDateField(value = dateTo, onChange = onDateToChange, placeholder = "Gacha: tanlash...")
                if (hasActiveFilter) {
                    IconButton(
                        onClick = {
                            onSearchChange("")
                            onTypeFilterChange("")
                            onDateFromChange("")
                            onDateToChange("")
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.Close, "Filtrlarni tozalash", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                    }
                }
            }
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(horizontal = 20.dp, vertical = 12.dp)) {
                TableHead("MAHSULOT", Modifier.weight(2f))
                TableHead("HARAKAT TURI", Modifier.weight(1.5f))
                TableHead("MIQDOR", Modifier.weight(1f))
                TableHead("XODIM", Modifier.weight(1f))
                TableHead("SANA", Modifier.weight(1.2f))
                TableHead("AMALLAR", Modifier.weight(0.5f))
            }
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (hasActiveFilter) "Filtrlar bo'yicha natija topilmadi" else "Harakat tarixi bo'sh",
                        color = MexaWarehouseColors.textMuted,
                    )
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
                        Column(Modifier.weight(2f)) {
                            Text(row.productName, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (row.reason.isNotBlank()) {
                                Text(
                                    "Sabab: ${row.reason}",
                                    fontSize = 11.sp,
                                    color = MexaWarehouseColors.textMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
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
                        Box(Modifier.weight(0.5f), contentAlignment = Alignment.CenterStart) {
                            IconButton(onClick = { onView(row) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Visibility, "Tafsilotlarni ko'rish", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                }
            }
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            PaginationBar(
                page = (page - 1).coerceAtLeast(0),
                pageSize = pageSize,
                totalElements = total,
                infoText = "Jami $total ta harakat",
                onPageChange = { onPage(it + 1) },
                onPageSizeChange = onPageSizeChange,
            )
        }
    }
}

@Composable
private fun MovementDetailDialog(row: MovementRow, onDismiss: () -> Unit) {
    val (typeColor, typeLabel) = movementLabel(row.movementType)
    val (statusColor, statusLabel) = movementStatusLabel(row.status)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 380.dp, max = 520.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Harakat tafsiloti", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                MovementDetailRow("ID", row.id)
                MovementDetailRow("Mahsulot", row.productName)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Harakat turi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                    Box(Modifier.background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = typeColor)
                    }
                }
                if (statusLabel != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Holat", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        Box(Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                }
                MovementDetailRow("Miqdor", "${row.quantity.toInt()} dona")
                MovementDetailRow("Xodim", row.actorUsername.ifEmpty { "—" })
                MovementDetailRow("Sana", row.createdAt.ifEmpty { "—" })
                if (row.reason.isNotBlank()) {
                    MovementDetailRow("Sabab", row.reason)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Yopish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        Text(
            value,
            Modifier.weight(1f),
            fontSize = 13.sp,
            color = MexaWarehouseColors.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
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
    onCancelLot: (String, String) -> Unit,
    onDeleteSingleLot: (String, String) -> Unit,
    page: Int = 0,
    pageSize: Int = 10,
    onPage: (Int) -> Unit = {},
    onPageSizeChange: (Int) -> Unit = {},
    search: String = "",
    onSearchChange: (String) -> Unit = {},
    dateFrom: String = "",
    onDateFromChange: (String) -> Unit = {},
    dateTo: String = "",
    onDateToChange: (String) -> Unit = {},
) {
    val lotDatePattern = remember { Regex("""\d{4}-\d{2}-\d{2}""") }
    val hasActiveFilter = search.isNotBlank() || dateFrom.isNotBlank() || dateTo.isNotBlank()
    val filteredLots = remember(lots, search, dateFrom, dateTo) {
        val q = search.trim()
        val d1 = dateFrom.trim()
        val d2 = dateTo.trim()
        lots.filter { lot ->
            val searchOk = q.isEmpty() || lot.productName.contains(q, true) || lot.purchaseId.contains(q, true)
            val fromOk = d1.isEmpty() || !lot.receivedDate.matches(lotDatePattern) || lot.receivedDate >= d1
            val toOk = d2.isEmpty() || !lot.receivedDate.matches(lotDatePattern) || lot.receivedDate <= d2
            searchOk && fromOk && toOk
        }
    }
    val groups = remember(filteredLots) { groupLotsByPurchase(filteredLots) }
    val lotCount = remember(lots) { groupLotsByPurchase(lots).size }
    val totalValue = lots.sumOf { it.totalCost }
    val totalPages = if (groups.isEmpty()) 1 else ((groups.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(0, totalPages - 1)
    val pagedGroups = groups.drop(safePage * pageSize).take(pageSize)
    var cancelTarget by remember { mutableStateOf<String?>(null) }
    var deleteLotTarget by remember { mutableStateOf<String?>(null) }

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
                    // ── Qidiruv + sana filtri paneli ────────────────────────────
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Mahsulot yoki kirim ID bo'yicha...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.textMuted) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                        )
                        FilterDateField(value = dateFrom, onChange = onDateFromChange, placeholder = "Dan: tanlash...")
                        FilterDateField(value = dateTo, onChange = onDateToChange, placeholder = "Gacha: tanlash...")
                        if (hasActiveFilter) {
                            IconButton(
                                onClick = {
                                    onSearchChange("")
                                    onDateFromChange("")
                                    onDateToChange("")
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Default.Close, "Filtrlarni tozalash", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
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
                            Text(
                                if (hasActiveFilter) "Filtrlar bo'yicha natija topilmadi" else "Lotlar topilmadi — hali kirim qayd etilmagan",
                                color = MexaWarehouseColors.textMuted,
                            )
                        }
                    } else {
                        LazyColumn(Modifier.heightIn(max = minOf(pagedGroups.sumOf { (it.lines.size + 1) * 46 } + 16, if (compact) 360 else 460).dp)) {
                            itemsIndexed(pagedGroups) { gIdx, group ->
                                val groupNo = safePage * pageSize + gIdx + 1
                                val groupQty = group.lines.sumOf { it.quantity }
                                val groupValue = group.lines.sumOf { it.totalCost }
                                val purchaseId = group.lines.firstOrNull()?.purchaseId.orEmpty()
                                Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(Modifier.weight(lotW), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("#$groupNo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.indigoAccent)
                                        Text("Kirim", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("${formatNum(groupQty.toInt())}", Modifier.weight(numW), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("—", Modifier.weight(numW), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                                    Text("${formatMoney(groupValue)}", Modifier.weight(numW), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(group.lines.first().receivedDate.ifEmpty { "—" }, Modifier.weight(numW), fontSize = 12.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Box(Modifier.weight(actW), contentAlignment = Alignment.CenterEnd) {
                                        if (isSuperAdmin) {
                                            val singleLotId = group.lines.firstOrNull()?.id.orEmpty()
                                            if (purchaseId.isNotEmpty() || singleLotId.isNotEmpty()) {
                                                IconButton(
                                                    onClick = {
                                                        if (purchaseId.isNotEmpty()) cancelTarget = purchaseId
                                                        else deleteLotTarget = singleLotId
                                                    },
                                                    modifier = Modifier.size(30.dp),
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Lotni o'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                                                }
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
                                        Box(Modifier.weight(actW), contentAlignment = Alignment.CenterEnd) {
                                            if (isSuperAdmin) {
                                                IconButton(
                                                    onClick = { deleteLotTarget = lot.id },
                                                    modifier = Modifier.size(30.dp),
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Lotni o'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                if (gIdx < pagedGroups.lastIndex) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                                }
                            }
                        }
                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                        val fromIdx = safePage * pageSize + 1
                        val toIdx = minOf((safePage + 1) * pageSize, groups.size)
                        PaginationBar(
                            page = safePage,
                            pageSize = pageSize,
                            totalElements = groups.size,
                            infoText = "$fromIdx - $toIdx / ${groups.size} ta kirim",
                            onPageChange = onPage,
                            onPageSizeChange = onPageSizeChange,
                        )
                    }
                }
            }
        }
    }

    cancelTarget?.let { purchaseId ->
        var cancelReason by remember { mutableStateOf("") }
        val cancelReasonValid = cancelReason.isNotBlank()
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
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Bekor qilish sababi *") },
                        placeholder = { Text("Sababni kiriting...") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        isError = !cancelReasonValid,
                        supportingText = {
                            if (!cancelReasonValid) Text("Sabab majburiy — o'chirish tarixiga yoziladi")
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                                val reason = cancelReason.trim()
                                cancelTarget = null
                                onCancelLot(purchaseId, reason)
                            },
                            enabled = cancelReasonValid,
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

    deleteLotTarget?.let { lotId ->
        var deleteReason by remember { mutableStateOf("") }
        val deleteReasonValid = deleteReason.isNotBlank()
        Dialog(onDismissRequest = { deleteLotTarget = null }) {
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
                    Text("Lotni o'chirish", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text(
                        "Ushbu lot o'chiriladi va tegishli stock miqdori qaytariladi. Harakat tarixida kim o'chirgani va sababi ko'rinadi. Tasdiqlaysizmi?",
                        fontSize = 13.sp,
                        color = MexaWarehouseColors.textMuted,
                    )
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("O'chirish sababi *") },
                        placeholder = { Text("Sababni kiriting...") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        isError = !deleteReasonValid,
                        supportingText = {
                            if (!deleteReasonValid) Text("Sabab majburiy — o'chirish tarixiga yoziladi")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(onClick = { deleteLotTarget = null }, shape = RoundedCornerShape(8.dp)) {
                            Text("Bekor qilish")
                        }
                        Button(
                            onClick = {
                                val reason = deleteReason.trim()
                                deleteLotTarget = null
                                onDeleteSingleLot(lotId, reason)
                            },
                            enabled = deleteReasonValid,
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
private fun ProfitTab(
    lines: List<ProfitLine>,
    totalRevenue: Double,
    totalCost: Double,
    totalProfit: Double,
    page: Int = 0,
    pageSize: Int = 10,
    onPage: (Int) -> Unit = {},
    onPageSizeChange: (Int) -> Unit = {},
) {
    val totalPages = if (lines.isEmpty()) 1 else ((lines.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(0, totalPages - 1)
    val paged = lines.drop(safePage * pageSize).take(pageSize)
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
                    paged.forEach { line ->
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
                    HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                    val fromIdx = safePage * pageSize + 1
                    val toIdx = minOf((safePage + 1) * pageSize, lines.size)
                    PaginationBar(
                        page = safePage,
                        pageSize = pageSize,
                        totalElements = lines.size,
                        infoText = "$fromIdx - $toIdx / ${lines.size} ta mahsulot",
                        onPageChange = onPage,
                        onPageSizeChange = onPageSizeChange,
                    )
                }
            }
        }
    }
}

// ─── Couriers Tab ──────────────────────────────────────────────────────────────

@Composable
private fun WarehouseCouriersTab(
    couriers: List<WarehouseCourierInfo>,
    stats: Map<String, CourierStats> = emptyMap(),
    page: Int = 0,
    pageSize: Int = 10,
    onPage: (Int) -> Unit = {},
    onPageSizeChange: (Int) -> Unit = {},
) {
    val totalPages = if (couriers.isEmpty()) 1 else ((couriers.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(0, totalPages - 1)
    val paged = couriers.drop(safePage * pageSize).take(pageSize)
    var detailCourier by remember { mutableStateOf<WarehouseCourierInfo?>(null) }

    Surface(
        Modifier.fillMaxWidth().border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Biriktirilgan kuryerlar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
            if (couriers.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Hech qanday kuryer biriktirilmagan", color = MexaWarehouseColors.textMuted)
                }
            } else {
                paged.forEach { courier ->
                    val s = stats[courier.id]
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { detailCourier = courier }
                            .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(28.dp).background(MexaWarehouseColors.statIndigoBg, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DeliveryDining, null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(16.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        courier.name.ifEmpty { "Kuryer ID: ${courier.id.take(8)}..." },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MexaWarehouseColors.textPrimary,
                                    )
                                    if (!courier.active) {
                                        CourierStatChip("NOFAOL", Color(0xFFDC2626))
                                    }
                                }
                                Text(
                                    buildString {
                                        append(courier.phone.ifEmpty { "—" })
                                        append(" • ${courier.currentDeliveries} ta faol yetkazma")
                                    },
                                    fontSize = 10.sp,
                                    color = MexaWarehouseColors.textCaption,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (s != null) {
                                CourierStatChip("Jami ${s.total}", Color(0xFF64748B))
                                CourierStatChip("Faol ${s.active}", MexaWarehouseColors.indigoAccent)
                                CourierStatChip("Yetkazildi ${s.delivered}", Color(0xFF16A34A))
                                if (s.failed > 0) CourierStatChip("Muvaffaqiyatsiz ${s.failed}", Color(0xFFDC2626))
                                if (s.returned > 0) CourierStatChip("Qaytarilgan ${s.returned}", Color(0xFFF59E0B))
                                if (s.today > 0) CourierStatChip("Bugun ${s.today}", Color(0xFF0284C7))
                            }
                            Icon(Icons.Default.ChevronRight, "Tafsilotlar", tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                val fromIdx = safePage * pageSize + 1
                val toIdx = minOf((safePage + 1) * pageSize, couriers.size)
                PaginationBar(
                    page = safePage,
                    pageSize = pageSize,
                    totalElements = couriers.size,
                    infoText = "$fromIdx - $toIdx / ${couriers.size} ta kuryer",
                    onPageChange = onPage,
                    onPageSizeChange = onPageSizeChange,
                )
            }
        }
    }

    detailCourier?.let { c ->
        CourierDetailDialog(courier = c, stats = stats[c.id], onDismiss = { detailCourier = null })
    }
}

@Composable
private fun CourierStatChip(text: String, fg: Color) {
    Box(Modifier.background(fg.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

private fun deliveryStatusLabel(status: String): Pair<Color, String> = when (status.uppercase()) {
    "PENDING" -> Color(0xFF64748B) to "KUTILMOQDA"
    "ASSIGNED" -> Color(0xFF7C3AED) to "BIRIKTIRILGAN"
    "PICKED_UP" -> Color(0xFF0284C7) to "OLINGAN"
    "IN_TRANSIT" -> Color(0xFFF59E0B) to "YO'LDA"
    "DELIVERED" -> Color(0xFF16A34A) to "YETKAZILDI"
    "FAILED" -> Color(0xFFDC2626) to "MUVAFFAQIYATSIZ"
    "RETURNED" -> Color(0xFFEA580C) to "QAYTARILGAN"
    else -> MexaWarehouseColors.textMuted to status
}

/** Kuryer tafsiloti: yetkazmalar statistikasi + oxirgi buyurtmalar (holati bilan). */
@Composable
private fun CourierDetailDialog(
    courier: WarehouseCourierInfo,
    stats: CourierStats?,
    onDismiss: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var deliveries by remember { mutableStateOf<List<CourierDeliveryRow>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courier.id) {
        loading = true
        try {
            val text = withContext(Dispatchers.IO) {
                ApiClient.get("/api/admin/deliveries?courierId=${courier.id}&page=0&size=20")
            }
            val arr = (ApiClient.parseJsonObject(text)?.get("data") as? JsonArray)
                ?: JsonArray(emptyList())
            deliveries = arr.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                CourierDeliveryRow(
                    id = o.stringField("id"),
                    orderId = o.stringField("orderId"),
                    status = o.stringField("status").ifEmpty { "PENDING" },
                    recipientName = o.stringField("recipientName"),
                    deliveryAddress = o.stringField("deliveryAddress"),
                    createdAt = o.stringField("createdAt").take(16).replace("T", " "),
                    failReason = o.stringField("failReason"),
                )
            }
            loadError = null
        } catch (e: Exception) {
            loadError = e.message
        }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 420.dp, max = 640.dp),
        ) {
            Column(
                Modifier.padding(24.dp).heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Kuryer tafsiloti", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).background(MexaWarehouseColors.statIndigoBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DeliveryDining, null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(courier.name.ifEmpty { "Kuryer ID: ${courier.id.take(8)}..." }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            CourierStatChip(if (courier.active) "FAOL" else "NOFAOL", if (courier.active) Color(0xFF16A34A) else Color(0xFFDC2626))
                        }
                        Text(courier.phone.ifEmpty { "—" }, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                }

                if (stats != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CourierStatBox("JAMI", stats.total.toString(), Color(0xFF64748B), Modifier.weight(1f))
                        CourierStatBox("FAOL", stats.active.toString(), MexaWarehouseColors.indigoAccent, Modifier.weight(1f))
                        CourierStatBox("YETKAZILDI", stats.delivered.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CourierStatBox("MUVAFFAQIYATSIZ", stats.failed.toString(), Color(0xFFDC2626), Modifier.weight(1f))
                        CourierStatBox("QAYTARILGAN", stats.returned.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                        CourierStatBox("BUGUN", stats.today.toString(), Color(0xFF0284C7), Modifier.weight(1f))
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                Text("Oxirgi buyurtmalar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }
                    loadError != null -> Text("Yuklashda xatolik: $loadError", fontSize = 12.sp, color = MexaWarehouseColors.danger)
                    deliveries.isEmpty() -> Text("Bu kuryerda hali buyurtma yo'q", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                    else -> deliveries.forEach { d ->
                        val (statusColor, statusLabel) = deliveryStatusLabel(d.status)
                        Row(
                            Modifier.fillMaxWidth()
                                .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Buyurtma #${d.orderId.take(8)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                if (d.recipientName.isNotEmpty()) {
                                    Text(d.recipientName, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (d.deliveryAddress.isNotEmpty()) {
                                    Text(d.deliveryAddress, fontSize = 11.sp, color = MexaWarehouseColors.textCaption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(d.createdAt, fontSize = 10.sp, color = MexaWarehouseColors.textCaption)
                                if (d.failReason.isNotBlank() && (d.status.equals("FAILED", true) || d.status.equals("RETURNED", true))) {
                                    Text("Sabab: ${d.failReason}", fontSize = 10.sp, color = MexaWarehouseColors.danger, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Box(Modifier.background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Yopish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourierStatBox(label: String, value: String, fg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(fg.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun AdminsTab(
    admins: List<AdminInfo>,
    onRemove: (AdminInfo) -> Unit = {},
    page: Int = 0,
    pageSize: Int = 10,
    onPage: (Int) -> Unit = {},
    onPageSizeChange: (Int) -> Unit = {},
) {
    val totalPages = if (admins.isEmpty()) 1 else ((admins.size + pageSize - 1) / pageSize)
    val safePage = page.coerceIn(0, totalPages - 1)
    val paged = admins.drop(safePage * pageSize).take(pageSize)
    var removeTarget by remember { mutableStateOf<AdminInfo?>(null) }

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
                paged.forEach { admin ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
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
                        IconButton(
                            onClick = { removeTarget = admin },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Olib tashlash", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                val fromIdx = safePage * pageSize + 1
                val toIdx = minOf((safePage + 1) * pageSize, admins.size)
                PaginationBar(
                    page = safePage,
                    pageSize = pageSize,
                    totalElements = admins.size,
                    infoText = "$fromIdx - $toIdx / ${admins.size} ta admin",
                    onPageChange = onPage,
                    onPageSizeChange = onPageSizeChange,
                )
            }
        }
    }

    removeTarget?.let { admin ->
        Dialog(onDismissRequest = { removeTarget = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MexaWarehouseColors.surfaceLowest,
                tonalElevation = 4.dp,
                modifier = Modifier.widthIn(min = 380.dp, max = 520.dp),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Adminni olib tashlash", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text(
                        "\"${admin.username.ifEmpty { "Admin" }}\" ushbu ombordan olib tashlanadi. Tasdiqlaysizmi?",
                        fontSize = 13.sp,
                        color = MexaWarehouseColors.textMuted,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                        OutlinedButton(onClick = { removeTarget = null }, shape = RoundedCornerShape(8.dp)) {
                            Text("Bekor qilish")
                        }
                        Button(
                            onClick = {
                                removeTarget = null
                                onRemove(admin)
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
    onView: (WarehouseOrder) -> Unit = {},
) {
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
                TableHead("AMALLAR", Modifier.weight(0.5f))
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
                        Box(Modifier.weight(0.5f), contentAlignment = Alignment.CenterStart) {
                            IconButton(onClick = { onView(order) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Visibility, "Buyurtmani ko'rish", tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.4f)))
                }
            }
            HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
            PaginationBar(
                page = (page - 1).coerceAtLeast(0),
                pageSize = pageSize,
                totalElements = total,
                pageSizeOptions = listOf(5, 10, 20, 50),
                infoText = run {
                    val from = if (total == 0) 0 else (page - 1) * pageSize + 1
                    val to = minOf(page * pageSize, total)
                    "$from - $to / $total ta buyurtma"
                },
                onPageChange = { onPageChange?.invoke(it + 1) },
                onPageSizeChange = onPageSizeChange,
            )
        }
    }
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

private val HISTORY_MOVEMENT_TYPES = listOf(
    "PURCHASE_IN", "SALES_OUT", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT",
    "RETURN_IN", "INVENTORY", "LOT_DELETE", "PURCHASE_DELETE",
)

// Chegirma sanalaridagidek: maydon bosilganda DatePicker ochiladi, × bilan tozalanadi
@Composable
private fun FilterDateField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.width(150.dp).border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp)).clickable { pickerOpen = true },
        shape = RoundedCornerShape(8.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                fontSize = 13.sp,
                color = if (value.isNotBlank()) Color.Unspecified else MexaWarehouseColors.textMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (value.isNotBlank()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Tozalash",
                    tint = MexaWarehouseColors.textMuted,
                    modifier = Modifier.size(16.dp).clickable { onChange("") },
                )
            }
        }
    }
    if (pickerOpen) {
        SimpleDatePickerDialog(
            current = value,
            onConfirm = { onChange(it) },
            onDismiss = { pickerOpen = false },
        )
    }
}

private fun movementLabel(type: String): Pair<Color, String> = when (type) {
    "PURCHASE_IN"  -> Color(0xFF16A34A) to "KIRIM"
    "PURCHASE_DELETE" -> Color(0xFFDC2626) to "LOT O'CHIRILDI"
    "LOT_DELETE" -> Color(0xFFDC2626) to "LOT O'CHIRILDI"
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

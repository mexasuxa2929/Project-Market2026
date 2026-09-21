package mexa.club.desktop_app.market.api

import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.market.ui.components.RecentOrderTableRow
import mexa.club.desktop_app.market.ui.components.buildRecentOrderRowFromApi

data class DashboardLoadResult(
    val users: String,
    val warehouses: String,
    val products: String,
    val todayOrders: String,
    val orders: List<RecentOrderTableRow>,
    val totalOrders: String,
    val partialErrors: List<String>,
)

private suspend fun safeEndpoint(
    label: String,
    errors: MutableList<String>,
    block: suspend () -> String,
): String = runCatching { block() }.getOrElse { e ->
    errors += "$label: ${e.message ?: "xatolik"}"
    "—"
}

suspend fun loadDashboardData(page: Int = 0, size: Int = 5): DashboardLoadResult {
    val json = ApiClient.json()
    val errors = mutableListOf<String>()

    val totalUsers = safeEndpoint("Foydalanuvchilar", errors) {
        totalFromApiPageBody(ApiClient.get("/api/admin/users?page=0&size=1"), json)
    }
    delay(50)
    val totalWh = safeEndpoint("Omborlar", errors) {
        totalFromApiPageBody(ApiClient.get("/api/warehouses?page=0&size=1"), json)
    }
    delay(50)
    val totalPr = safeEndpoint("Mahsulotlar", errors) {
        val productsPath = "/api/products?page=0&size=1"
        val productsUrl = ApiClient.fullPath(productsPath)
        val productsToken = SessionManager.token.takeIf { it.isNotEmpty() }
            ?: AuthSession.accessToken?.takeIf { it.isNotBlank() }
        val maskedProductsAuth = productsToken?.take(10)?.let { "Bearer $it…" } ?: "<no token>"

        val productsBody = try {
            ApiClient.get(productsPath)
        } catch (e: GatewayApiException) {
            throw e
        }
        val productsCount = totalFromApiPageBody(productsBody, json)
        productsCount
    }
    delay(50)

    val todayTotal = safeEndpoint("Buyurtma statistikasi", errors) {
        val sText = ApiClient.get("/api/admin/orders/stats")
        val sEnv = ApiClient.parseJsonObject(sText)
        val sData = sEnv?.let { ApiClient.dataObject(it) }
        val todayObj = sData?.get("today") as? JsonObject
        (todayObj?.get("total") as? JsonPrimitive)?.content ?: "—"
    }
    delay(50)

    var totalEl = "—"
    var ordRows = emptyList<RecentOrderTableRow>()
    runCatching {
        val oText = ApiClient.get("/api/admin/orders?page=$page&size=$size")
        totalEl = totalFromApiPageBody(oText, json)
        val oEnv = ApiClient.parseJsonObject(oText)
        val oData = oEnv?.let { ApiClient.dataObject(it) }
        val items = oData?.itemsOrContentArray() ?: JsonArray(emptyList())
        ordRows = items.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            buildRecentOrderRowFromApi(
                orderNumber = o.stringField("orderNumber"),
                userId = o.stringField("userId"),
                totalAmount = o.stringField("totalAmount"),
                status = o.stringField("status"),
                createdAt = o.stringField("createdAt"),
            )
        }
    }.onFailure { e ->
        errors += "Buyurtmalar: ${e.message ?: "xatolik"}"
    }

    return DashboardLoadResult(
        users = totalUsers,
        warehouses = totalWh,
        products = totalPr,
        todayOrders = todayTotal,
        orders = ordRows,
        totalOrders = totalEl,
        partialErrors = errors,
    )
}

package mexa.club.desktop_app.market.api

import kotlinx.serialization.json.JsonPrimitive

/**
 * Gateway Redis → SSE (`/api/realtime/stream`) orqali keladigan
 * [realtime-lib] mutatsiya xabarlari: `path`, `method`, `service`, …
 */
fun realtimeMutationAffectsSuperAdminDashboard(envelopeJson: String): Boolean {
    val root = ApiClient.parseJsonObject(envelopeJson) ?: return false
    val path = (root["path"] as? JsonPrimitive)?.content ?: return false
    val p = path.lowercase()
    return p.contains("/admin/users")
        || p.contains("/warehouses")
        || p.contains("/products")
        || p.contains("/admin/orders")
        || p.contains("/api/orders")
        || p.contains("/orders")
        || p.contains("/stock")
        || p.contains("/purchase")
        || p.contains("/inventory")
        || p.contains("/users")
}

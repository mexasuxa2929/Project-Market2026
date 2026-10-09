package mexa.club.desktop_app.market.api

import kotlinx.serialization.json.JsonPrimitive

/**
 * Gateway Redis → SSE (`/api/realtime/stream`) orqali keladigan
 * realtime xabarlari: `resource` (server normalizatsiyasi), `path`, `method`, `service`, …
 *
 * Server `[mexa.club.realtime.RealtimePaths]` orqali `resource` qo'shadi:
 * ID segmentlaridan qutilgan holda barqaror prefiks. Eski payload'larda
 * `resource` bo'lmasa, path client tomonida bir xil qoida bilan normalizatsiya qilinadi.
 */
private val WATCHED_RESOURCES = listOf(
    "/admin/users",
    "/warehouses",
    "/products",
    "/admin/orders",
    "/api/orders",
    "/orders",
    "/stock",
    "/purchase",
    "/inventory",
    "/returns",
    "/couriers",
    "/admins",
    "/users",
    "/roles",
    "/pricing",
    "/delivery-zones",
    "/templates",
    "/notifications",
    "/reviews",
    "/promotions",
    "/analytics",
    "/groups",
    "/search",
)

fun realtimeMutationAffectsSuperAdminDashboard(envelopeJson: String): Boolean {
    val root = ApiClient.parseJsonObject(envelopeJson) ?: return false
    val resource = (root["resource"] as? JsonPrimitive)?.content
        ?: (root["path"] as? JsonPrimitive)?.content?.let { normalizeResourcePath(it) }
        ?: return false
    val p = resource.lowercase()
    return WATCHED_RESOURCES.any { p.contains(it) }
}

/** Server normalizatsiyasiz eski xabarlar uchun fallback (RealtimePaths bilan bir xil qoida). */
fun normalizeResourcePath(path: String): String {
    if (path.isEmpty()) return path
    return path.split("/").joinToString("/") { segment ->
        if (segment.isIdSegment()) "{id}" else segment
    }
}

private fun String.isIdSegment(): Boolean {
    if (isEmpty()) return false
    if (UUID_REGEX.matches(this)) return true
    if (HEX_ID_REGEX.matches(this)) return true
    return all { it.isDigit() }
}

private val UUID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val HEX_ID_REGEX = Regex("^[0-9a-fA-F]{16,}$")

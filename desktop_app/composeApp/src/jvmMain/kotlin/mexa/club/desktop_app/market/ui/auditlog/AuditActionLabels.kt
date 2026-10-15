package mexa.club.desktop_app.market.ui.auditlog

import androidx.compose.ui.graphics.Color
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Audit amal/obyekt kodlari → odam tushunadigan nomlar ─────────────────────
// Backend (/api/audit-logs) faqat tizim kodlarini yuboradi (masalan STOCK_SALES_OUT).
// Bu fayl ikki bosqichli avtomatik tizim:
//   1. Aniq lug'at — backend da bugun mavjud 18 ta kod uchun tahrirlangan nom.
//   2. Token generatori — ertaga backend yangi kod qo'shsa (masalan STOCK_RESERVE),
//      kod bo'laklarga bo'linib avtomatik o'zbekcha nom yig'iladi.
// Shuning uchun hech qanday kod «nomsiz» qolmaydi; tizim kodi hamisha ostida ko'rinadi.

// Backend da yozilgan barcha amallar (warehouse-service dagi auditLogService.log chaqiruvlari).
private val AUDIT_ACTION_LABELS = mapOf(
    "STOCK_SALES_OUT" to "Sotuv chiqimi",
    "STOCK_SALES_OUT_REVERSE" to "Sotuv bekor qilindi (zaxira qaytdi)",
    "STOCK_ADJUST" to "Zaxirani qo'lda to'g'rilash",
    "STOCK_UPSERT" to "Zaxira yaratish / yangilash",
    "STOCK_MIN_STOCK_UPDATE" to "Minimal zaxira yangilandi",
    "STOCK_LINE_DELETE" to "Zaxira qatori o'chirildi",
    "LOT_DELETE" to "Partiya (lot) o'chirildi",
    "STOCK_TRANSFER_OUT" to "Ko'chirish: jo'natildi",
    "STOCK_TRANSFER_IN" to "Ko'chirish: qabul qilindi",
    "STOCK_TRANSFER_IN_CONFIRMED" to "Ko'chirish tasdiqlandi",
    "STOCK_TRANSFER_IN_REJECTED" to "Ko'chirish rad etildi",
    "STOCK_RETURN_CREATE" to "Qaytarish so'rovi yaratildi",
    "INVENTORY_START" to "Inventarizatsiya boshlandi",
    "INVENTORY_ADJUST" to "Inventarizatsiya tuzatmasi",
    "INVENTORY_COMPLETE" to "Inventarizatsiya yakunlandi",
    "INVENTORY_CANCEL" to "Inventarizatsiya bekor qilindi",
    "PURCHASE_DELETE" to "Kirim o'chirildi",
    // STOCK_RETURN tafsilotga qarab (approved/rejected) pastda aniqlanadi.
)

// Yangi/noma'lum kodlar uchun so'zma-so'z tarjima lug'ati (SNAKE_CASE bo'laklari).
private val AUDIT_ACTION_TOKENS = mapOf(
    "STOCK" to "zaxira",
    "STOCKS" to "zaxiralar",
    "SALES" to "sotuv",
    "SALE" to "sotuv",
    "OUT" to "chiqim",
    "IN" to "kirim",
    "REVERSE" to "bekor qilindi (qaytarildi)",
    "REVERSED" to "bekor qilindi (qaytarildi)",
    "TRANSFER" to "ko'chirish",
    "TRANSFERS" to "ko'chirishlar",
    "CONFIRM" to "tasdiqlash",
    "CONFIRMED" to "tasdiqlandi",
    "REJECT" to "rad etish",
    "REJECTED" to "rad etildi",
    "RETURN" to "qaytarish",
    "RETURNS" to "qaytarishlar",
    "RETURNED" to "qaytarildi",
    "CREATE" to "yaratish",
    "CREATED" to "yaratildi",
    "ADJUST" to "to'g'rilash",
    "ADJUSTED" to "to'g'rilandi",
    "ADJUSTMENT" to "to'g'rilash",
    "UPSERT" to "yaratish / yangilash",
    "UPDATE" to "yangilash",
    "UPDATED" to "yangilandi",
    "MIN" to "minimal",
    "MAX" to "maksimal",
    "LINE" to "qator",
    "LINES" to "qatorlar",
    "DELETE" to "o'chirish",
    "DELETED" to "o'chirildi",
    "LOT" to "partiya",
    "LOTS" to "partiyalar",
    "START" to "boshlanishi",
    "STARTED" to "boshlandi",
    "COMPLETE" to "yakunlash",
    "COMPLETED" to "yakunlandi",
    "CANCEL" to "bekor qilish",
    "CANCELLED" to "bekor qilindi",
    "PURCHASE" to "kirim",
    "PURCHASES" to "kirimlar",
    "INVENTORY" to "inventarizatsiya",
    "RESERVE" to "band qilish",
    "RESERVED" to "band qilindi",
    "RELEASE" to "bo'shatish",
    "RELEASED" to "bo'shatildi",
    "MERGE" to "birlashtirish",
    "MERGED" to "birlashtirildi",
    "SPLIT" to "bo'lish",
    "MOVE" to "ko'chirish",
    "MOVED" to "ko'chirildi",
    "COUNT" to "sanoq",
    "RECOUNT" to "qayta sanoq",
    "CORRECTION" to "tuzatish",
    "APPROVE" to "tasdiqlash",
    "APPROVED" to "tasdiqlandi",
    "ASSIGN" to "biriktirish",
    "ASSIGNED" to "biriktirildi",
    "UNASSIGN" to "bo'shatish",
    "WAREHOUSE" to "ombor",
    "WAREHOUSES" to "omborlar",
    "SESSION" to "sessiya",
    "QUANTITY" to "miqdor",
    "PRICE" to "narx",
    "PRODUCT" to "mahsulot",
    "PRODUCTS" to "mahsulotlar",
    "ORDER" to "buyurtma",
    "ORDERS" to "buyurtmalar",
    "USER" to "foydalanuvchi",
    "ROLE" to "rol",
    "STATUS" to "holat",
    "SYNC" to "sinxronlash",
    "IMPORT" to "import",
    "EXPORT" to "eksport",
    "REFUND" to "pulni qaytarish",
    "PAYMENT" to "to'lov",
    "DELIVERY" to "yetkazib berish",
    "COURIER" to "kuryer",
    "DISCOUNT" to "chegirma",
    "PROMOTION" to "aksiya",
    "NOTIFICATION" to "bildirishnoma",
    "REPORT" to "hisobot",
    "SETTING" to "sozlama",
    "SETTINGS" to "sozlamalar",
    "BACKUP" to "zaxira nusxa",
    "RESTORE" to "tiklash",
    "AUDIT" to "audit",
    "LOG" to "jurnal",
)

// Backend da uchraydigan barcha obyekt turlari.
private val AUDIT_TARGET_LABELS = mapOf(
    "WAREHOUSE_STOCK" to "Ombor zaxirasi",
    "STOCK_LOT" to "Partiya (lot)",
    "STOCK_RETURN" to "Qaytarish",
    "INVENTORY_SESSION" to "Inventarizatsiya",
    "PURCHASE" to "Kirim",
)

/**
 * Amal kodi → odam tushunadigan nom. Har doim biror nom qaytaradi:
 * avval aniq lug'at, keyin tafsilot tahlili, oxirida token generatori.
 */
fun auditActionFriendlyName(action: String, details: String): String {
    if (action.isBlank()) return "—"
    if (action == "STOCK_RETURN") {
        return when {
            details.contains("approved", ignoreCase = true) -> "Qaytarish tasdiqlandi"
            details.contains("rejected", ignoreCase = true) -> "Qaytarish rad etildi"
            else -> "Qaytarish"
        }
    }
    AUDIT_ACTION_LABELS[action]?.let { return it }
    return generateLabelFromTokens(action, AUDIT_ACTION_TOKENS) ?: "Noma'lum amal"
}

/** Obyekt turi kodi → odam tushunadigan nom. Har doim biror nom qaytaradi. */
fun auditTargetFriendlyName(targetType: String): String {
    if (targetType.isBlank()) return "—"
    AUDIT_TARGET_LABELS[targetType]?.let { return it }
    return generateLabelFromTokens(targetType, AUDIT_TARGET_TOKENS) ?: "Noma'lum obyekt"
}

// Obyekt turlari uchun ham amal tokenlari ishlaydi (TRANSFER, RESERVE kabi
// kelajakdagi turlar avtomatik tarjima qilinishi uchun) + maxsus qo'shimchalar.
private val AUDIT_TARGET_TOKENS: Map<String, String> = AUDIT_ACTION_TOKENS + mapOf(
    "ITEM" to "qator",
    "ITEMS" to "qatorlar",
    "BATCH" to "partiya",
    "RECORD" to "yozuv",
    "RECORDS" to "yozuvlar",
)

/**
 * SNAKE_CASE kodni bo'laklab, har bir bo'lakni lug'atdan tarjima qilib nom yig'adi.
 * Masalan: STOCK_RESERVE → «Zaxira band qilish».
 * Hech bir bo'lak tanilmasa null qaytaradi (chaqiruvchi «Noma'lum ...» ko'rsatadi).
 * Tanilmagan yakka bo'laklar o'qilishi uchun kichik harfda o'z holicha qoldiriladi.
 */
private fun generateLabelFromTokens(code: String, tokens: Map<String, String>): String? {
    val parts = code.split("_").filter { it.isNotBlank() }
    if (parts.isEmpty()) return null
    var recognized = 0
    val words = parts.map { part ->
        val hit = tokens[part]
        if (hit != null) {
            recognized++
            hit
        } else {
            part.lowercase()
        }
    }
    if (recognized == 0) return null
    return words.joinToString(" ").replaceFirstChar { it.uppercaseChar() }
}

/** Amal toifasi bo'yicha rangli nuqta (tez ko'z bilan ajratish uchun). */
fun auditActionColor(action: String): Color = when {
    action.startsWith("STOCK_SALES") -> MexaWarehouseColors.blueBadgeFg
    action.startsWith("STOCK_TRANSFER") -> MexaWarehouseColors.indigoAccent
    action.startsWith("STOCK_RETURN") -> MexaWarehouseColors.amberBadgeFg
    action.startsWith("INVENTORY") -> MexaWarehouseColors.statusActiveFg
    action.contains("DELETE") || action.contains("REJECT") || action.contains("CANCEL") -> MexaWarehouseColors.danger
    action.startsWith("PURCHASE") -> MexaWarehouseColors.tertiaryContainer
    else -> MexaWarehouseColors.outline
}

package mexa.club.desktop_app.market.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Stringni to'g'ri escape qilingan JSON string literalga aylantiradi (qo'shtirnoqlar bilan).
 * Misol: `a"b\c` → `"a\"b\\c"`. JSON body qurishda user kiritgan matnlarni shu yerda o'tkazing,
 * aks holda qo'shtirnoq yoki newline JSON strukturasini buzadi.
 */
fun String.jsonString(): String = Json.encodeToString(JsonPrimitive(this))

fun JsonObject.itemsOrContentArray(): JsonArray =
    (this["items"] as? JsonArray) ?: (this["content"] as? JsonArray) ?: JsonArray(emptyList())

fun JsonObject.totalElementsString(): String {
    val el = this["totalElements"] ?: return "—"
    if (el is JsonNull) return "—"
    return (el as? JsonPrimitive)?.content ?: "—"
}

/**
 * JSON fielddan string oladi.
 * JsonNull (JSON null qiymat) → ""  qaytaradi, "null" string emas.
 * kotlinx.serialization 1.8.x da JsonNull extends JsonPrimitive,
 * shuning uchun alohida tekshiruv kerak.
 */
fun JsonObject.stringField(key: String): String {
    val el = this[key] ?: return ""
    if (el is JsonNull) return ""
    return (el as? JsonPrimitive)?.content?.let {
        if (it == "null") "" else it
    } ?: ""
}

/**
 * Gateway javobi: `{"success":true,"data":{...}}` yoki shop `/api/products` kabi to‘g‘ridan-to‘g‘ri `[...]` massiv.
 * `data` ichida `totalElements` bo‘lmasa, `content` / `items` uzunligi (faqat to‘liq ro‘yxat endpointlari uchun).
 */
fun totalFromApiPageBody(body: String, json: Json): String {
    val root: JsonElement = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return "—"
    return when (root) {
        is JsonArray -> root.size.toString()
        is JsonObject -> totalFromEnvelopeData(root["data"]) ?: totalFromEnvelopeData(root) ?: "—"
        else -> "—"
    }
}

private fun totalFromEnvelopeData(data: JsonElement?): String? {
    return when (data) {
        is JsonObject -> {
            val te = (data["totalElements"] as? JsonPrimitive)?.content?.trim().orEmpty()
            if (te.isNotEmpty()) return te
            val arr = (data["content"] as? JsonArray) ?: (data["items"] as? JsonArray)
            if (arr != null) return arr.size.toString()
            null
        }
        is JsonArray -> data.size.toString()
        else -> null
    }
}

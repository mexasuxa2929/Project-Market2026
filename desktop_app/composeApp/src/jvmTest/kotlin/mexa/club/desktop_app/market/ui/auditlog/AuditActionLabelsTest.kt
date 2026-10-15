package mexa.club.desktop_app.market.ui.auditlog

import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AuditActionLabels avtomatik nomlash tizimi testlari.
 * 1) Backend dagi 18 ta real kod → kutilgan aniq nom.
 * 2) Kelajakdagi noma'lum kodlar → token generatoridan tushunarli nom.
 * 3) Hech qachon bo'sh/«nomsiz» qaytmasligi.
 */
class AuditActionLabelsTest {

    // ─── 1. Backend dagi barcha real kodlar (aniq lug'at) ────────────────────

    @Test
    fun `backenddagi barcha amallar aniq nom qaytaradi`() {
        val expected = mapOf(
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
        )
        assertEquals("Lug'atda 17 ta aniq kod bo'lishi kerak", 17, expected.size)
        expected.forEach { (code, label) ->
            assertEquals(code, label, auditActionFriendlyName(code, ""))
        }
    }

    @Test
    fun `STOCK_RETURN tafsilotga qarab farqlanadi`() {
        assertEquals("Qaytarish tasdiqlandi", auditActionFriendlyName("STOCK_RETURN", "approved"))
        assertEquals("Qaytarish rad etildi", auditActionFriendlyName("STOCK_RETURN", "rejected"))
        assertEquals("Qaytarish", auditActionFriendlyName("STOCK_RETURN", ""))
        assertEquals("Qaytarish", auditActionFriendlyName("STOCK_RETURN", "reason=test"))
    }

    @Test
    fun `backenddagi barcha obyekt turlari aniq nom qaytaradi`() {
        val expected = mapOf(
            "WAREHOUSE_STOCK" to "Ombor zaxirasi",
            "STOCK_LOT" to "Partiya (lot)",
            "STOCK_RETURN" to "Qaytarish",
            "INVENTORY_SESSION" to "Inventarizatsiya",
            "PURCHASE" to "Kirim",
        )
        expected.forEach { (code, label) ->
            assertEquals(code, label, auditTargetFriendlyName(code))
        }
    }

    // ─── 2. Kelajakdagi noma'lum kodlar (token generatori) ────────────────────

    @Test
    fun `yangi kodlar avtomatik tushunarli nom oladi`() {
        assertEquals("Zaxira band qilish", auditActionFriendlyName("STOCK_RESERVE", ""))
        assertEquals("Partiya birlashtirish", auditActionFriendlyName("LOT_MERGE", ""))
        assertEquals(
            "Inventarizatsiya qayta sanoq",
            auditActionFriendlyName("INVENTORY_RECOUNT", ""),
        )
        assertEquals(
            "Ombor ko'chirish",
            auditTargetFriendlyName("WAREHOUSE_TRANSFER"),
        )
    }

    @Test
    fun `umuman notanish kod Noma'lum deb chiqadi, bo'sh emas`() {
        assertEquals("Noma'lum amal", auditActionFriendlyName("XYZ_QQQ", ""))
        assertEquals("Noma'lum obyekt", auditTargetFriendlyName("XYZ_QQQ"))
    }

    @Test
    fun `bosh qiymat tire qaytaradi`() {
        assertEquals("—", auditActionFriendlyName("", ""))
        assertEquals("—", auditTargetFriendlyName(""))
    }

    // ─── 3. Ranglar ───────────────────────────────────────────────────────────

    @Test
    fun `amal toifasi togri rang oladi`() {
        assertEquals(MexaWarehouseColors.blueBadgeFg, auditActionColor("STOCK_SALES_OUT"))
        assertEquals(MexaWarehouseColors.indigoAccent, auditActionColor("STOCK_TRANSFER_IN"))
        assertEquals(MexaWarehouseColors.danger, auditActionColor("LOT_DELETE"))
        assertEquals(MexaWarehouseColors.statusActiveFg, auditActionColor("INVENTORY_START"))
    }

    // ─── 4. Xavfsizlik: nom har doim boladi ───────────────────────────────────

    @Test
    fun `ixtiyoriy kod hech qachon bosh qaytarmaydi`() {
        val samples = listOf(
            "STOCK_SALES_OUT",
            "STOCK_RETURN",
            "STOCK_RESERVE",
            "FUTURE_NEW_ACTION",
            "A",
            "",
        )
        samples.forEach { code ->
            val label = auditActionFriendlyName(code, "approved")
            assertTrue("'$code' uchun nom bo'sh!", label.isNotBlank())
        }
    }
}

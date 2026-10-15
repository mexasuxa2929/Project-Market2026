package com.example.mobile_app

import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.repository.parseProductPage
import com.example.mobile_app.data.repository.unwrapProductDetail
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Product-detail o'rami testi: gateway admin yo'nalishda
 * {"success":true,"data":{...}} qaytaradi — shu ochilishi shart
 * (aks holda id=null → "Noto'g'ri mahsulot" xatosi).
 */
class ProductDetailUnwrapTest {

    private val gson = Gson()

    // Real server javobi shakli (shop-service: xom Product)
    private val rawJson = """
        {"id":"bed26153-1550-40c3-a0a5-68e910052532","name":"Premium Smartphone X1 Pro Max",
        "barcode":"MSX1P20260923001","basePrice":13175000.0,"active":true}
    """.trimIndent()

    private fun parse(body: String): Product =
        gson.fromJson(unwrapProductDetail(gson.fromJson(body, com.google.gson.JsonElement::class.java)), Product::class.java)

    @Test
    fun rawProduct_parsesWithId() {
        val p = parse(rawJson)
        assertEquals("bed26153-1550-40c3-a0a5-68e910052532", p.id)
        assertEquals("Premium Smartphone X1 Pro Max", p.name)
    }

    @Test
    fun wrappedProduct_unwrapsAndParsesWithId() {
        // product-service o'rami (admin yo'nalish)
        val wrapped = """{"success":true,"data":$rawJson}"""
        val p = parse(wrapped)
        assertEquals("bed26153-1550-40c3-a0a5-68e910052532", p.id)
        assertEquals("Premium Smartphone X1 Pro Max", p.name)
    }

    @Test
    fun failureWrapper_throwsServerMessage() {
        val body = """{"success":false,"data":null,"error":{"code":"NOT_FOUND","message":"Product not found"}}"""
        try {
            parse(body)
            fail("xatolik otilishi kerak edi")
        } catch (e: IllegalStateException) {
            assertEquals("Product not found", e.message)
        }
    }

    private fun page(body: String) =
        parseProductPage(gson.fromJson(body, com.google.gson.JsonElement::class.java), gson)

    private val rawPage = """
        {"content":[$rawJson],"page":0,"totalElements":1,"totalPages":1,
        "nextCursor":null,"hasMore":false}
    """.trimIndent()

    @Test
    fun rawPage_parsesContent() {
        val p = page(rawPage)
        assertEquals(1, p.content.size)
        assertEquals("bed26153-1550-40c3-a0a5-68e910052532", p.content[0].id)
        assertFalse(p.hasMore)
    }

    @Test
    fun wrappedPage_unwrapsAndParsesContent() {
        // product-service o'rami (admin yo'nalish) — tavsiyalar yo'qolmasligi uchun
        val p = page("""{"success":true,"data":$rawPage}""")
        assertEquals(1, p.content.size)
        assertEquals("bed26153-1550-40c3-a0a5-68e910052532", p.content[0].id)
    }
}

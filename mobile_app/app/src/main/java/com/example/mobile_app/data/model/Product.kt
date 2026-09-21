package com.example.mobile_app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ProductColorSibling(
    val id: String,
    val color: String? = null,
    val colorCode: String? = null,
    val barcode: String,
    val imageUrl: String? = null,
    val active: Boolean = true
)

@Immutable
data class Product(
    val id: String,
    val name: String,
    val barcode: String? = null,
    val color: String? = null,
    val colorCode: String? = null,
    val groupId: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val brandId: String? = null,
    val brandName: String? = null,
    val manufacturerId: String? = null,
    val manufacturerName: String? = null,
    val unit: String? = null,
    val weight: Double = 0.0,
    val minStock: Int? = null,
    val leadTimeDays: Int? = null,
    val deliveryDaysMin: Int? = null,
    val deliveryDaysMax: Int? = null,
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val packageType: String? = null,
    val sku: String? = null,
    val fragile: Boolean = false,
    val active: Boolean = true,
    val description: String? = null,
    val shortDescription: String? = null,
    val material: String? = null,
    val countryOfOrigin: String? = null,
    val warrantyMonths: Int? = null,
    val colors: List<String>? = null,
    val imageUrls: List<String>? = null,
    val siblingColors: List<ProductColorSibling>? = null,
    val tags: List<String>? = null,
    val basePrice: Double? = null,
    val totalStock: Int? = null,
    val inStock: Boolean? = null,
    val avgRating: Double? = null,
    val reviewCount: Long? = null,
    val discountPercent: Int = 0,
    val nameTranslations: Map<String, String>? = null,
    val descriptionTranslations: Map<String, String>? = null,
    val shortDescriptionTranslations: Map<String, String>? = null,
    val materialTranslations: Map<String, String>? = null,
    val countryOfOriginTranslations: Map<String, String>? = null,
    val manufacturerNameTranslations: Map<String, String>? = null
) {
    /** Birinchi rasmning THUMB URL'i (default) — avval thumb_ variantini qidiradi. */
    val thumbnail: String?
        get() = imageUrls.orEmpty()
            .firstOrNull { it.contains("/thumb_") }
            ?.let { it }
            ?: imageUrls.orEmpty().firstOrNull()?.ensureThumbUrl()

    /** Barcha THUMB URL'lar (ro'yxatdagi barchasi THUMB) */
    val thumbImageUrls: List<String>
        get() = imageUrls.orEmpty().map { it.ensureThumbUrl() }

    /** Barcha MEDIUM URL'lar (thumb_ -> medium_ almashtirish) */
    val mediumImageUrls: List<String>
        get() = imageUrls.orEmpty().map { it.replace("/thumb_", "/medium_") }

    /** Barcha ORIGINAL URL'lar (thumb_ -> original_ almashtirish) */
    val originalImageUrls: List<String>
        get() = imageUrls.orEmpty().map { it.replace("/thumb_", "/original_") }

    /** i-chi rasmning MEDIUM URL'i */
    fun mediumImageUrl(index: Int = 0): String? {
        val thumb = imageUrls?.getOrNull(index) ?: return null
        return thumb.replace("/thumb_", "/medium_")
    }

    /** i-chi rasmning ORIGINAL URL'i */
    fun originalImageUrl(index: Int = 0): String? {
        val thumb = imageUrls?.getOrNull(index) ?: return null
        return thumb.replace("/thumb_", "/original_")
    }

    val displayBrand: String
        get() = brandName ?: categoryName ?: manufacturerName ?: "Unknown"

    val hasSiblingColors: Boolean
        get() = siblingColors.orEmpty().isNotEmpty()
}

/** URL ni THUMB versiyasiga aylantiradi: /original_ yoki /medium_ → /thumb_ */
fun String.ensureThumbUrl(): String {
    if (contains("/thumb_")) return this
    if (contains("/original_")) return replace("/original_", "/thumb_")
    if (contains("/medium_")) return replace("/medium_", "/thumb_")
    return this
}

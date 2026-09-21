package com.example.mobile_app.data.local

import androidx.room.Entity

/**
 * Katalog mahsulotlarining offline keshi.
 * queryKey — har bir qidiruv so'rovi uchun alohida ketma-ketlik saqlanadi
 * (bo'sh qidiruv = umumiy katalog, "iphone" = qidiruv natijalari).
 * json — lokalizatsiyasiz RAW Product (til almashganda qayta lokalizatsiya qilinadi).
 */
@Entity(tableName = "catalog_products", primaryKeys = ["queryKey", "id"])
data class CatalogProductEntity(
    val queryKey: String,
    val id: String,
    val json: String
)
package com.example.mobile_app.util

import com.example.mobile_app.data.model.Brand
import com.example.mobile_app.data.model.Category
import com.example.mobile_app.data.model.Product

fun Product.localizedName(lang: Language): String = nameTranslations?.get(lang.code) ?: name

fun Product.localizedDescription(lang: Language): String? = descriptionTranslations?.get(lang.code) ?: description

fun Product.localizedShortDescription(lang: Language): String? = shortDescriptionTranslations?.get(lang.code) ?: shortDescription

fun Product.localizedMaterial(lang: Language): String? = materialTranslations?.get(lang.code) ?: material

fun Product.localizedCountryOfOrigin(lang: Language): String? = countryOfOriginTranslations?.get(lang.code) ?: countryOfOrigin

fun Product.localizedManufacturerName(lang: Language): String? = manufacturerNameTranslations?.get(lang.code) ?: manufacturerName

fun Product.localizedDisplayBrand(lang: Language): String =
    brandName ?: categoryName ?: localizedManufacturerName(lang) ?: "Unknown"

fun Category.localizedName(lang: Language): String = nameTranslations?.get(lang.code) ?: name

fun Brand.localizedName(lang: Language): String = nameTranslations?.get(lang.code) ?: name

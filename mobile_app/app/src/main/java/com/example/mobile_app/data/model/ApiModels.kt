package com.example.mobile_app.data.model

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

/** Backend universal wrapper: { "success": true, "data": ... } */
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String? = null,
    val message: String? = null
)

/** Paginated response: { "content": [...], "page": 0, "totalElements": 10, "totalPages": 1, "nextCursor": "...", "hasMore": true } */
data class PagePayload<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("nextCursor") val nextCursor: String? = null,
    @SerializedName("hasMore") val hasMore: Boolean = false
)

@Immutable
data class Category(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val parentId: String? = null,
    val parentName: String? = null,
    val active: Boolean = true,
    val nameTranslations: Map<String, String>? = null
)

@Immutable
data class Brand(
    val id: String,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val logoUrl: String? = null,
    val nameTranslations: Map<String, String>? = null
)

/** Homepage bundle: GET api/shops/home — 4 round-trip o'rniga bitta so'rov */
data class HomeBundleData(
    val trending: List<Product> = emptyList(),
    val trendingHasMore: Boolean = false,
    val trendingNextCursor: String? = null,
    val recommended: List<RecommendedProduct> = emptyList(),
    val brands: List<Brand> = emptyList(),
    val categories: List<Category> = emptyList()
)

data class HomeApiResponse(
    val success: Boolean = false,
    val data: HomeBundleData? = null
)

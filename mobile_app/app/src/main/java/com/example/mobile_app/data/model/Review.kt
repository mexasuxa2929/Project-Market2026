package com.example.mobile_app.data.model

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import java.util.UUID

// ── Public review (product sahifasida ko'rinadigan) ──────────────────────────
@Immutable
data class PublicReview(
    val id: String,
    val username: String?,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

// ── Mening reviewim ───────────────────────────────────────────────────────────
data class MyReview(
    val id: String,
    val productId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val canEdit: Boolean = true,
)

// ── Review yozish so'rovi ─────────────────────────────────────────────────────
data class CreateReviewRequest(
    val productId: String,
    val rating: Int,
    val comment: String?,
)

// ── Reyting statistikasi ──────────────────────────────────────────────────────
data class RatingStats(
    val productId: String,
    val avgRating: Double,
    val reviewCount: Long,
    val distribution: Map<String, Long>,  // "1"→5, "2"→3, ...
) {
    /** Taqsimotni Int kalitiga aylantirish */
    fun distributionInt(): Map<Int, Long> =
        distribution.mapKeys { it.key.toIntOrNull() ?: 0 }
}

// ── Rekomendatsiya ────────────────────────────────────────────────────────────
@Immutable
data class RecommendedProduct(
    val productId: String,
    val name: String,
    val thumbnail: String?,
    val categoryName: String?,
    val brandName: String?,
    val basePrice: Double?,
    val avgRating: Double,
    val reviewCount: Long,
) {
    val thumbUrl: String?
        get() = thumbnail?.ensureThumbUrl()
}

// ── API wrapper'lar ───────────────────────────────────────────────────────────
data class ReviewListApiResponse(
    val success: Boolean = false,
    val data: ReviewPageData?,
)

data class ReviewPageData(
    val content: List<PublicReview> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

data class RatingStatsApiResponse(
    val success: Boolean = false,
    val data: RatingStats?,
)

data class MyReviewApiResponse(
    val success: Boolean = false,
    val data: MyReview?,
)

data class RecommendedApiResponse(
    val success: Boolean = false,
    val data: List<RecommendedProduct>?,
)

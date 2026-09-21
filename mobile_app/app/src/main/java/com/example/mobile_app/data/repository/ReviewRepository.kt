package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.CreateReviewRequest
import com.example.mobile_app.data.model.MyReview
import com.example.mobile_app.data.model.PublicReview
import com.example.mobile_app.data.model.RatingStats
import com.example.mobile_app.data.model.RecommendedProduct
import com.example.mobile_app.data.remote.ReviewApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ReviewRepository(private val api: ReviewApiService) {

    fun getProductReviews(productId: String, page: Int = 0, size: Int = 20): Flow<Result<List<PublicReview>>> = flow {
        runCatching { api.getProductReviews(productId, page, size) }
            .fold(
                onSuccess = { emit(Result.success(it.data?.content ?: emptyList())) },
                onFailure = { emit(Result.failure(it)) }
            )
    }.flowOn(Dispatchers.IO)

    fun getRatingStats(productId: String): Flow<Result<RatingStats>> = flow {
        runCatching { api.getRatingStats(productId) }
            .fold(
                onSuccess = { resp ->
                    if (resp.success && resp.data != null) emit(Result.success(resp.data))
                    else emit(Result.failure(Exception("Reyting ma'lumoti topilmadi")))
                },
                onFailure = { emit(Result.failure(it)) }
            )
    }.flowOn(Dispatchers.IO)

    fun getRecommended(limit: Int = 10, discountOnly: Boolean = false): Flow<Result<List<RecommendedProduct>>> = flow {
        runCatching { api.getRecommended(limit, discountOnly) }
            .fold(
                onSuccess = { emit(Result.success(it.data ?: emptyList())) },
                onFailure = { emit(Result.failure(it)) }
            )
    }.flowOn(Dispatchers.IO)

    fun getMyReview(productId: String): Flow<Result<MyReview?>> = flow {
        runCatching { api.getMyReview(productId) }
            .fold(
                onSuccess = { emit(Result.success(it.data)) },
                onFailure = { emit(Result.success(null)) }  // 404 = review yo'q
            )
    }.flowOn(Dispatchers.IO)

    suspend fun submitReview(productId: String, rating: Int, comment: String?): Result<MyReview> =
        runCatching {
            val resp = api.submitReview(CreateReviewRequest(productId, rating, comment))
            resp.data ?: error("Bo'sh javob")
        }
}

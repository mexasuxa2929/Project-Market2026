package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.MyReviewApiResponse
import com.example.mobile_app.data.model.RatingStatsApiResponse
import com.example.mobile_app.data.model.RecommendedApiResponse
import com.example.mobile_app.data.model.ReviewListApiResponse
import com.example.mobile_app.data.model.CreateReviewRequest
import com.example.mobile_app.data.model.MyReview
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApiService {

    /** Mahsulot reviewlari ro'yxati (ommaviy) */
    @GET("api/shops/products/{productId}/reviews")
    suspend fun getProductReviews(
        @Path("productId") productId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ReviewListApiResponse

    /** Mahsulot reyting statistikasi */
    @GET("api/shops/products/{productId}/rating")
    suspend fun getRatingStats(
        @Path("productId") productId: String,
    ): RatingStatsApiResponse

    /** Rekomendatsiya qilingan mahsulotlar (bosh sahifa: discountOnly=true — faqat chegirmalilar) */
    @GET("api/shops/products/recommended")
    suspend fun getRecommended(
        @Query("limit") limit: Int = 10,
        @Query("discountOnly") discountOnly: Boolean = false,
    ): RecommendedApiResponse

    /** Mening bu mahsulot uchun reviewim (yo'q bo'lsa 404) */
    @GET("api/shops/me/reviews/{productId}")
    suspend fun getMyReview(
        @Path("productId") productId: String,
    ): MyReviewApiResponse

    /** Review yaratish yoki yangilash */
    @POST("api/shops/me/reviews")
    suspend fun submitReview(
        @Body request: CreateReviewRequest,
    ): MyReviewApiResponse
}

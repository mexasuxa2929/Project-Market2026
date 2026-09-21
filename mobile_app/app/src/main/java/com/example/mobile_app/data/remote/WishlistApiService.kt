package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.wishlist.WishlistAddRequest
import com.example.mobile_app.data.model.wishlist.WishlistApiWrapper
import com.example.mobile_app.data.model.wishlist.WishlistListWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WishlistApiService {

    @GET("api/shops/me/wishlist")
    suspend fun listWishlist(): Response<WishlistListWrapper>

    @POST("api/shops/me/wishlist")
    suspend fun addToWishlist(@Body body: WishlistAddRequest): Response<WishlistApiWrapper>

    @DELETE("api/shops/me/wishlist/{productId}")
    suspend fun removeFromWishlist(@Path("productId") productId: String): Response<WishlistApiWrapper>

    @GET("api/shops/me/wishlist/{productId}/check")
    suspend fun checkWishlist(@Path("productId") productId: String): Response<WishlistApiWrapper>
}

package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.cart.CartApiWrapper
import com.example.mobile_app.data.model.cart.CartLineApiWrapper
import com.example.mobile_app.data.model.cart.CartQuantityRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface CartApiService {

    @GET("api/shops/me/cart")
    suspend fun getCart(): Response<CartApiWrapper>

    @PUT("api/shops/me/cart/items/{productId}")
    suspend fun updateCartItem(
        @Path("productId") productId: String,
        @Body body: CartQuantityRequest
    ): Response<CartLineApiWrapper>

    @DELETE("api/shops/me/cart/items/{productId}")
    suspend fun removeCartItem(@Path("productId") productId: String): Response<Unit>

    @DELETE("api/shops/me/cart")
    suspend fun clearCart(): Response<Unit>
}

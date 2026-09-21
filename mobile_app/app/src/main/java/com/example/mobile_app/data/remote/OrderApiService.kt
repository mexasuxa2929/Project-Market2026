package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.order.CreateOrderRequest
import com.example.mobile_app.data.model.order.OrderApiWrapper
import com.example.mobile_app.data.model.order.OrderPageApiWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApiService {

    @GET("api/shops/me/orders")
    suspend fun getOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<OrderPageApiWrapper>

    @GET("api/shops/me/orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): Response<OrderApiWrapper>

    @POST("api/shops/me/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderApiWrapper>

    @POST("api/shops/me/orders/{id}/cancel")
    suspend fun cancelOrder(
        @Path("id") id: String,
        @Body body: Map<String, String?>
    ): Response<OrderApiWrapper>
}

package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.ApiResponse
import com.example.mobile_app.data.model.Brand
import com.example.mobile_app.data.model.Category
import com.example.mobile_app.data.model.HomeApiResponse
import com.example.mobile_app.data.model.PagePayload
import com.example.mobile_app.data.model.Product
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Shop-service: PagePayload<Product> qaytaradi (content, nextCursor, hasMore, ...)
    @GET("api/products")
    suspend fun getProducts(
        @Query("search") search: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("sort") sort: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int = 20
    ): PagePayload<Product>

    @GET("api/products/{id}")
    suspend fun getProductById(
        @Path("id") id: String
    ): Product

    // Homepage bundle (2-bosqich): trending + recommended + brands + categories — bitta so'rov
    @GET("api/shops/home")
    suspend fun getHome(): HomeApiResponse

    // Product-service: ApiResponse<PagePayload<Category>> qaytaradi
    @GET("api/categories")
    suspend fun getCategories(
        @Query("active") active: Boolean = true,
        @Query("size") size: Int = 100
    ): ApiResponse<PagePayload<Category>>

    // Product-service: brendlar ro'yxati
    @GET("api/brands")
    suspend fun getBrands(
        @Query("active") active: Boolean = true,
        @Query("size") size: Int = 8
    ): ApiResponse<PagePayload<Brand>>
}

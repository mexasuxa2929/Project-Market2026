package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.address.AddressApiWrapper
import com.example.mobile_app.data.model.address.AddressListWrapper
import com.example.mobile_app.data.model.address.CoverageCheckApiWrapper
import com.example.mobile_app.data.model.address.CreateAddressRequest
import com.example.mobile_app.data.model.address.GeoCoverageApiWrapper
import com.example.mobile_app.data.model.address.GeoReverseApiWrapper
import com.example.mobile_app.data.model.address.GeoSearchApiWrapper
import com.example.mobile_app.data.model.address.UpdateAddressRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AddressApiService {

    @GET("api/shops/me/addresses")
    suspend fun listAddresses(): Response<AddressListWrapper>

    @POST("api/shops/me/addresses")
    suspend fun createAddress(@Body body: CreateAddressRequest): Response<AddressApiWrapper>

    @PUT("api/shops/me/addresses/{id}")
    suspend fun updateAddress(@Path("id") id: String, @Body body: UpdateAddressRequest): Response<AddressApiWrapper>

    @DELETE("api/shops/me/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: String): Response<AddressApiWrapper>

    @PATCH("api/shops/me/addresses/{id}/default")
    suspend fun setDefaultAddress(@Path("id") id: String): Response<AddressApiWrapper>

    /** Manzil qidiruv: matn → nomzodlar (name + lat + lng) */
    @GET("api/shops/geo/search")
    suspend fun searchPlaces(@Query("query") query: String): GeoSearchApiWrapper

    /** Teskari geokodlash: nuqta → manzil nomi */
    @GET("api/shops/geo/reverse")
    suspend fun reversePlace(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): GeoReverseApiWrapper

    /** Xizmat hududlari: faol geo-poligonlar (xaritada chiziladi) */
    @GET("api/shops/geo/coverage")
    suspend fun coverageZones(): GeoCoverageApiWrapper

    /** Nuqta xizmat hududidami */
    @GET("api/shops/geo/check")
    suspend fun checkCoverage(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): CoverageCheckApiWrapper
}

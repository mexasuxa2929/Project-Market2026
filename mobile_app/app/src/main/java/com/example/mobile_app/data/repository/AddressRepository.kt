package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.address.AddressResponse
import com.example.mobile_app.data.model.address.CoverageCheckResult
import com.example.mobile_app.data.model.address.CreateAddressRequest
import com.example.mobile_app.data.model.address.GeoCoverageZone
import com.example.mobile_app.data.model.address.GeoPlace
import com.example.mobile_app.data.model.address.UpdateAddressRequest
import com.example.mobile_app.data.remote.AddressApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AddressRepository(private val api: AddressApiService) {

    fun listAddresses(): Flow<Result<List<AddressResponse>>> = flow {
        try {
            val response = api.listAddresses()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    emit(Result.success(body.data))
                } else {
                    emit(Result.failure(Exception(body?.error?.message ?: "Failed to load addresses")))
                }
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun createAddress(request: CreateAddressRequest): Result<Unit> = try {
        val response = api.createAddress(request)
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.body()?.error?.message ?: "HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateAddress(id: String, request: UpdateAddressRequest): Result<Unit> = try {
        val response = api.updateAddress(id, request)
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.body()?.error?.message ?: "HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteAddress(id: String): Result<Unit> = try {
        val response = api.deleteAddress(id)
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.body()?.error?.message ?: "HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun setDefaultAddress(id: String): Result<Unit> = try {
        val response = api.setDefaultAddress(id)
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.body()?.error?.message ?: "HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Manzil qidiruv: matn → nomzodlar (name + lat + lng) */
    suspend fun searchPlaces(query: String): Result<List<GeoPlace>> = try {
        val resp = api.searchPlaces(query)
        if (resp.success) {
            Result.success(resp.data ?: emptyList())
        } else {
            Result.failure(Exception("Qidiruvda xatolik"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Teskari geokodlash: nuqta → nomli manzil */
    suspend fun reversePlace(lat: Double, lng: Double): Result<GeoPlace> = try {
        val resp = api.reversePlace(lat, lng)
        val place = resp.data
        if (resp.success && place != null) {
            Result.success(place)
        } else {
            Result.success(GeoPlace("$lat, $lng", lat, lng))
        }
    } catch (e: Exception) {
        Result.success(GeoPlace("$lat, $lng", lat, lng))
    }

    /** Xizmat hududlari (faol poligonlar) — xaritada chiziladi. */
    suspend fun coverageZones(): Result<List<GeoCoverageZone>> = try {
        val resp = api.coverageZones()
        if (resp.success) {
            Result.success(resp.data ?: emptyList())
        } else {
            Result.success(emptyList())
        }
    } catch (e: Exception) {
        Result.success(emptyList())
    }

    /** Nuqta xizmat hududidami (zona nomi bilan). */
    suspend fun checkCoverage(lat: Double, lng: Double): Result<CoverageCheckResult> = try {
        val resp = api.checkCoverage(lat, lng)
        if (resp.success && resp.data != null) {
            Result.success(resp.data)
        } else {
            Result.success(CoverageCheckResult(served = false, zoneName = null))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

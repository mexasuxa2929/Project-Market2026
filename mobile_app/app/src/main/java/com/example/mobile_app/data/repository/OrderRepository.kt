package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.order.CreateOrderRequest
import com.example.mobile_app.data.model.order.OrderPagePayload
import com.example.mobile_app.data.model.order.OrderResponsePayload
import com.example.mobile_app.data.remote.OrderApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

class OrderRepository(private val orderApiService: OrderApiService) {

    fun getOrders(page: Int = 0, size: Int = 20): Flow<Result<OrderPagePayload>> = flow {
        try {
            val response = orderApiService.getOrders(page, size)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    emit(Result.success(body.data))
                } else {
                    emit(Result.failure(Exception(body?.error?.message ?: "Failed to load orders")))
                }
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getOrderById(id: String): Flow<Result<OrderResponsePayload>> = flow {
        try {
            val response = orderApiService.getOrderById(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    emit(Result.success(body.data))
                } else {
                    emit(Result.failure(Exception(body?.error?.message ?: "Order not found")))
                }
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun createOrder(request: CreateOrderRequest): Result<OrderResponsePayload> = try {
        val response = orderApiService.createOrder(request)
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Buyurtma yaratib bo'lmadi"))
            }
        } else {
            val errorMsg = parseError(response.errorBody()?.string())
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cancelOrder(id: String, reason: String?): Result<OrderResponsePayload> = try {
        val body = mapOf("reason" to reason)
        val response = orderApiService.cancelOrder(id, body)
        if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
        } else {
            val errorMsg = parseError(response.errorBody()?.string())
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parseError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Xatolik yuz berdi"
        return try {
            val json = JSONObject(errorBody)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: json.optString("message", "Xatolik yuz berdi")
        } catch (_: Exception) {
            "Xatolik yuz berdi"
        }
    }
}

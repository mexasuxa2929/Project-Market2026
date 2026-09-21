package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.cart.CartResponse
import com.example.mobile_app.data.remote.CartApiService
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.model.cart.CartQuantityRequest
import com.example.mobile_app.data.model.ensureThumbUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CartRepository(private val cartApiService: CartApiService) {

    fun getCart(): Flow<Result<CartResponse>> = flow {
        try {
            val response = cartApiService.getCart()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    // Backend nisbiy URL beradi (/api/files/...) — absolut + thumb ga aylantirish,
                    // aks holda Coil yuklay olmaydi (qizil xatolik ko'rinadi)
                    val cart = body.data.copy(
                        lines = body.data.lines.map { line ->
                            line.copy(
                                imageUrl = RetrofitClient.toAbsoluteUrl(line.imageUrl)?.ensureThumbUrl()
                            )
                        }
                    )
                    emit(Result.success(cart))
                } else {
                    emit(Result.failure(Exception(body?.error?.message ?: "Failed to load cart")))
                }
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateCartItem(productId: String, quantity: Int): Result<Unit> = try {
        val response = cartApiService.updateCartItem(productId, CartQuantityRequest(quantity))
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeCartItem(productId: String): Result<Unit> = try {
        val response = cartApiService.removeCartItem(productId)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun clearCart(): Result<Unit> = try {
        val response = cartApiService.clearCart()
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

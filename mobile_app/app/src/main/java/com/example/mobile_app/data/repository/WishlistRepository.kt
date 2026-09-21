package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.wishlist.WishlistEntryResponse
import com.example.mobile_app.data.model.ensureThumbUrl
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.remote.WishlistApiService
import com.example.mobile_app.data.model.wishlist.WishlistAddRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class WishlistRepository(private val api: WishlistApiService) {

    fun listWishlist(): Flow<Result<List<WishlistEntryResponse>>> = flow {
        try {
            val response = api.listWishlist()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    // Backend nisbiy URL beradi (/api/files/...) — absolut + thumb ga aylantirish.
                    val entries = body.data.content.map { entry ->
                        entry.copy(
                            imageUrl = RetrofitClient.toAbsoluteUrl(entry.imageUrl)?.ensureThumbUrl()
                        )
                    }
                    emit(Result.success(entries))
                } else {
                    emit(Result.failure(Exception("Failed to load wishlist")))
                }
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun checkFavorite(productId: String): Result<Boolean> = try {
        val response = api.checkWishlist(productId)
        if (response.isSuccessful) {
            val body = response.body()
            val data = body?.data
            when (data) {
                is Boolean -> Result.success(data)
                is Map<*, *> -> Result.success(data["isFavorited"] == true || data["favorited"] == true)
                else -> Result.success(false)
            }
        } else {
            Result.failure(Exception("HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleFavorite(productId: String, currentlyFavorited: Boolean): Result<Unit> = try {
        val response = if (currentlyFavorited) {
            api.removeFromWishlist(productId)
        } else {
            api.addToWishlist(WishlistAddRequest(productId))
        }
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            val errorMsg = response.errorBody()?.string()?.take(200)
                ?: response.message()
                ?: "HTTP ${response.code()}"
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

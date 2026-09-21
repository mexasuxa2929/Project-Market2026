package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.notification.AppNotification
import com.example.mobile_app.data.remote.NotificationApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class NotificationRepository(private val api: NotificationApiService) {

    fun getMyNotifications(): Flow<Result<List<AppNotification>>> = flow {
        try {
            val response = api.getMyNotifications(page = 0, size = 50)
            val wrapper = response.body()
            if (response.isSuccessful && wrapper != null && wrapper.success) {
                val items = wrapper.data?.content ?: emptyList()
                emit(Result.success(items))
            } else {
                val msg = wrapper?.error?.message ?: "Bildirishnomalar yuklanmadi"
                emit(Result.failure(Exception(msg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getUnreadCount(): Flow<Result<Long>> = flow {
        try {
            val response = api.getUnreadCount()
            val wrapper = response.body()
            if (response.isSuccessful && wrapper != null && wrapper.success) {
                emit(Result.success(wrapper.data?.count ?: 0L))
            } else {
                emit(Result.failure(Exception(wrapper?.error?.message ?: "HTTP ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun markRead(id: String): Result<Unit> = try {
        val response = api.markRead(id)
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.body()?.error?.message ?: "HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markAllRead(): Result<Unit> = try {
        val response = api.markAllRead()
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.body()?.error?.message ?: "HTTP ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
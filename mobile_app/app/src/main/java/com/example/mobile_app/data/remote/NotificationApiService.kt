package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.notification.NotificationApiWrapper
import com.example.mobile_app.data.model.notification.SimpleStatusWrapper
import com.example.mobile_app.data.model.notification.UnreadCountWrapper
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("api/notifications/my")
    suspend fun getMyNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<NotificationApiWrapper>

    @GET("api/notifications/my/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountWrapper>

    @PATCH("api/notifications/my/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<SimpleStatusWrapper>

    @PATCH("api/notifications/my/read-all")
    suspend fun markAllRead(): Response<SimpleStatusWrapper>
}
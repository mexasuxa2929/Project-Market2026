package com.example.mobile_app.data.model.notification

import com.example.mobile_app.data.model.cart.ErrorData

data class AppNotification(
    val id: String,
    val channel: String? = null,
    val type: String? = null,
    val recipientEmail: String? = null,
    val recipientPhone: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val status: String? = null,
    val retryCount: Int? = null,
    val errorMessage: String? = null,
    val sentAt: String? = null,
    val createdAt: String? = null,
    val read: Boolean? = null
)

data class NotificationPage(
    val content: List<AppNotification>,
    val page: Int? = null,
    val size: Int? = null,
    val totalElements: Long? = null,
    val totalPages: Int? = null
)

data class NotificationApiWrapper(
    val success: Boolean,
    val data: NotificationPage?,
    val error: ErrorData?
)

data class UnreadCountWrapper(
    val success: Boolean,
    val data: UnreadCountData?,
    val error: ErrorData?
)

data class UnreadCountData(val count: Long?)

data class SimpleStatusWrapper(
    val success: Boolean,
    val data: SimpleStatusData?,
    val error: ErrorData?
)

data class SimpleStatusData(val updated: Boolean?)
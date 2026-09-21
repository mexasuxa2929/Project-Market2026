package com.example.mobile_app.data.model.order

import androidx.compose.runtime.Immutable

@Immutable
data class OrderItemPayload(
    val id: String,
    val productId: String,
    val productName: String?,
    val imageUrl: String?,
    val quantity: Int,
    val unitPrice: Double?,
    val subtotal: Double?
)

@Immutable
data class OrderResponsePayload(
    val id: String,
    val orderNumber: String?,
    val userId: String,
    val status: String,
    val deliveryAddressId: String?,
    val deliveryAddress: String?,
    val subtotal: Double?,
    val deliveryFee: Double?,
    val estimatedDeliveryDays: Int? = null,
    val totalAmount: Double?,
    val currency: String?,
    val note: String?,
    val cancelReason: String?,
    val paymentStatus: String?,
    val paymentId: String?,
    val paymentMethod: String? = null,
    val createdAt: String?,
    val updatedAt: String?,
    val items: List<OrderItemPayload>?
)

data class OrderPagePayload(
    val content: List<OrderResponsePayload>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)

data class OrderApiWrapper(
    val success: Boolean,
    val data: OrderResponsePayload?,
    val error: ErrorPayload?
)

data class OrderPageApiWrapper(
    val success: Boolean,
    val data: OrderPagePayload?,
    val error: ErrorPayload?
)

data class CreateOrderItemRequest(
    val productId: String,
    val quantity: Int
)

data class CreateOrderRequest(
    val deliveryAddressId: String? = null,
    val deliveryAddress: String? = null,
    val note: String? = null,
    val items: List<CreateOrderItemRequest>,
    val discountCode: String? = null,
    val paymentMethod: String? = null
)

data class ErrorPayload(val code: String?, val message: String?)

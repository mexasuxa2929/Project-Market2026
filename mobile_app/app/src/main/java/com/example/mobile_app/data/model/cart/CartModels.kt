package com.example.mobile_app.data.model.cart

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
data class CartLineResponse(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double?,
    val updatedAt: String?,
    val productName: String? = null,
    val imageUrl: String? = null,
    val color: String? = null,
    val colorCode: String? = null
)

@Immutable
data class CartResponse(
    val lines: List<CartLineResponse>,
    val totalQuantity: Int,
    val lineCount: Long
)

data class CartApiWrapper(
    val success: Boolean,
    val data: CartResponse?,
    val error: ErrorData?
)

data class CartQuantityRequest(val quantity: Int)

data class CartLineApiWrapper(
    val success: Boolean,
    val data: CartLineResponse?,
    val error: ErrorData?
)

data class ErrorData(val code: String?, val message: String?)

package com.example.mobile_app.data.model.wishlist

import androidx.compose.runtime.Immutable

@Immutable
data class WishlistEntryResponse(
    val productId: String,
    val addedAt: String?,
    val productName: String? = null,
    val imageUrl: String? = null
)

data class WishlistAddRequest(val productId: String)

data class WishlistApiWrapper(
    val success: Boolean,
    val data: Any?,
    val error: ErrorData?
)

data class WishlistListData(
    val content: List<WishlistEntryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
)

data class WishlistListWrapper(
    val success: Boolean,
    val data: WishlistListData?,
    val error: ErrorData?
)

data class ErrorData(val code: String?, val message: String?)

package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.data.model.cart.CartLineResponse
import com.example.mobile_app.data.model.cart.CartResponse
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.CartRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CartViewModel(private val repository: CartRepository) : ViewModel() {

    private val _cart = MutableStateFlow<UiState<CartResponse>>(UiState.Loading)
    val cart: StateFlow<UiState<CartResponse>> = _cart.asStateFlow()

    init {
        loadCart()
        observeLanguage()
    }

    /** Til o'zgarganda savatni qayta yuklash — nomlar yangi tilda ko'rinadi */
    private fun observeLanguage() {
        viewModelScope.launch {
            // drop(1): init'dagi loadCart() bilan 2 marta so'rov ketmasligi uchun
            com.example.mobile_app.util.AppLanguage.flow.drop(1).collect {
                loadCart()
            }
        }
    }

    fun loadCart() {
        viewModelScope.launch {
            _cart.value = UiState.Loading
            repository.getCart().collect { result ->
                result.fold(
                    onSuccess = { _cart.value = UiState.Success(it) },
                    onFailure = { _cart.value = UiState.Error(it.message ?: "Failed to load cart") }
                )
            }
        }
    }

    /**
     * Jim yangilash: Loading ko'rsatilmaydi, ro'yxat ekranda qoladi.
     * Mutatsiyalardan keyin server haqiqatini olish uchun.
     */
    fun refreshCart() {
        viewModelScope.launch {
            repository.getCart().collect { result ->
                result.fold(
                    onSuccess = { _cart.value = UiState.Success(it) },
                    onFailure = { /* joriy holat saqlanadi */ }
                )
            }
        }
    }

    /** Optimistic jami: server maydonlari lokal qayta hisoblanadi (realtime count uchun) */
    private fun withTotals(lines: List<CartLineResponse>, current: CartResponse): CartResponse =
        current.copy(
            lines = lines,
            totalQuantity = lines.sumOf { it.quantity },
            lineCount = lines.size.toLong()
        )

    /**
     * Miqdorni o'rnatish (absolyut PUT).
     * Loading/Error holatida ham serverga yuboriladi (avval `?: return` bilan
     * no-op bo'lib qolgan edi — audit M-H3). onResult: server muvaffaqiyati (M-H5).
     */
    fun updateItem(productId: String, quantity: Int, onResult: ((Boolean) -> Unit)? = null) {
        val current = (_cart.value as? UiState.Success)?.data
        if (current != null) {
            _cart.value = UiState.Success(withTotals(
                current.lines.map {
                    if (it.productId == productId) it.copy(quantity = quantity) else it
                },
                current
            ))
        }

        viewModelScope.launch {
            val ok = repository.updateCartItem(productId, quantity).isSuccess
            refreshCart()
            onResult?.invoke(ok)
        }
    }

    /**
     * Yangi mahsulot qo'shish: optimistic line darhol ro'yxatda ko'rinadi
     * (unitPrice/nom Product dan olinadi), keyin server bilan sinxronlanadi.
     */
    /**
     * Mahsulotni savatga qo'shish (miqdor +1).
     * Joriy miqdor lokal holatdan olinadi; cart yuklanmagan bo'lsa — serverdan
     * so'raladi (avval `updateItem(id, 1)` chaqirilib mavjud miqdorni 1 ga
     * bosib qo'ygan edi — audit M-H3). onResult: server muvaffaqiyati (M-H5).
     */
    fun addProduct(product: Product, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val loaded = (_cart.value as? UiState.Success)?.data
            val baseQty = if (loaded != null) {
                loaded.lines.find { it.productId == product.id }?.quantity ?: 0
            } else {
                // Cart holati ma'lum emas (Loading/Error) — miqdorni serverdan olamiz
                repository.getCart().first().getOrNull()?.lines
                    ?.find { it.productId == product.id }?.quantity ?: 0
            }
            val newQty = baseQty + 1

            // Optimistic line (faqat cart yuklangan bo'lsa)
            if (loaded != null) {
                val existing = loaded.lines.find { it.productId == product.id }
                val newLines = if (existing != null) {
                    loaded.lines.map {
                        if (it.productId == product.id) it.copy(quantity = newQty) else it
                    }
                } else {
                    loaded.lines + CartLineResponse(
                        productId = product.id,
                        quantity = newQty,
                        unitPrice = product.basePrice,
                        updatedAt = null,
                        productName = product.name,
                        imageUrl = product.thumbnail,
                        color = product.color,
                        colorCode = null
                    )
                }
                _cart.value = UiState.Success(withTotals(newLines, loaded))
            }

            val ok = repository.updateCartItem(product.id, newQty).isSuccess
            refreshCart()
            onResult?.invoke(ok)
        }
    }

    fun removeItem(productId: String) {
        val current = (_cart.value as? UiState.Success)?.data ?: return
        _cart.value = UiState.Success(withTotals(
            current.lines.filter { it.productId != productId },
            current
        ))

        viewModelScope.launch {
            repository.removeCartItem(productId).fold(
                onSuccess = { refreshCart() },
                onFailure = { refreshCart() }
            )
        }
    }

    fun clearCart() {
        val current = (_cart.value as? UiState.Success)?.data ?: return
        _cart.value = UiState.Success(withTotals(emptyList(), current))

        viewModelScope.launch {
            repository.clearCart().fold(
                onSuccess = { refreshCart() },
                onFailure = { refreshCart() }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CartViewModel(CartRepository(RetrofitClient.cartApiService))
            }
        }
    }
}

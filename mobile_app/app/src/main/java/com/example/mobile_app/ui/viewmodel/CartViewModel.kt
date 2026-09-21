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

    fun updateItem(productId: String, quantity: Int) {
        val current = (_cart.value as? UiState.Success)?.data ?: return
        _cart.value = UiState.Success(withTotals(
            current.lines.map {
                if (it.productId == productId) it.copy(quantity = quantity) else it
            },
            current
        ))

        viewModelScope.launch {
            // Har doim serverdan sinxronlash: yangi line/narxlar + rollback
            repository.updateCartItem(productId, quantity).fold(
                onSuccess = { refreshCart() },
                onFailure = { refreshCart() }
            )
        }
    }

    /**
     * Yangi mahsulot qo'shish: optimistic line darhol ro'yxatda ko'rinadi
     * (unitPrice/nom Product dan olinadi), keyin server bilan sinxronlanadi.
     */
    fun addProduct(product: Product) {
        val current = (_cart.value as? UiState.Success)?.data
        if (current != null) {
            val existing = current.lines.find { it.productId == product.id }
            val newLines = if (existing != null) {
                current.lines.map {
                    if (it.productId == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current.lines + CartLineResponse(
                    productId = product.id,
                    quantity = 1,
                    unitPrice = product.basePrice,
                    updatedAt = null,
                    productName = product.name,
                    imageUrl = product.thumbnail,
                    color = product.color,
                    colorCode = null
                )
            }
            _cart.value = UiState.Success(withTotals(newLines, current))
        }

        viewModelScope.launch {
            val qty = ((_cart.value as? UiState.Success)?.data?.lines
                ?.find { it.productId == product.id }?.quantity) ?: 1
            repository.updateCartItem(product.id, qty).fold(
                onSuccess = { refreshCart() },
                onFailure = { refreshCart() }
            )
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

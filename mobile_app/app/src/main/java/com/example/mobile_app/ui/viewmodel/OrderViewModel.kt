package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.data.model.order.CreateOrderRequest
import com.example.mobile_app.data.model.order.OrderResponsePayload
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.OrderRepository
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.util.trNow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _orders = MutableStateFlow<UiState<List<OrderResponsePayload>>>(UiState.Loading)
    val orders: StateFlow<UiState<List<OrderResponsePayload>>> = _orders.asStateFlow()

    private val _orderDetail = MutableStateFlow<UiState<OrderResponsePayload>>(UiState.Loading)
    val orderDetail: StateFlow<UiState<OrderResponsePayload>> = _orderDetail.asStateFlow()

    private val _placing = MutableStateFlow(false)
    val placing: StateFlow<Boolean> = _placing.asStateFlow()

    private val _placeError = MutableStateFlow<String?>(null)
    val placeError: StateFlow<String?> = _placeError.asStateFlow()

    private val _cancellingIds = MutableStateFlow<Set<String>>(emptySet())
    val cancellingIds: StateFlow<Set<String>> = _cancellingIds.asStateFlow()

    /** Bekor qilish xatosi — Snackbar orqali ko'rsatiladi */
    private val _cancelError = MutableStateFlow<String?>(null)
    val cancelError: StateFlow<String?> = _cancelError.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentOrderId: String? = null
    private var currentPage = 0
    private val pageSize = 20
    private val allOrders = mutableListOf<OrderResponsePayload>()

    init {
        loadOrders()
        observeLanguage()
    }

    /** Til o'zgarganda buyurtmalar ro'yxatini (va ochiq buyurtma tafsilotini) qayta yuklash */
    private fun observeLanguage() {
        viewModelScope.launch {
            // drop(1): init'dagi loadOrders() bilan 2 marta so'rov ketmasligi uchun
            com.example.mobile_app.util.AppLanguage.flow.drop(1).collect {
                loadOrders()
                currentOrderId?.let { loadOrderDetail(it) }
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            currentPage = 0
            allOrders.clear()
            _hasMore.value = true
            _orders.value = UiState.Loading
            fetchPage(page = 0, reset = true)
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchPage(page = currentPage + 1, reset = false)
            _isLoadingMore.value = false
        }
    }

    /**
     * Pull-to-refresh / realtime event: ro'yxat ekranda qoladi, Loading ko'rsatilmaydi.
     * Xato bo'lsa joriy holat saqlanadi.
     */
    fun refreshOrders() {
        viewModelScope.launch {
            _isRefreshing.value = true
            currentPage = 0
            fetchPage(page = 0, reset = true)
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchPage(page: Int, reset: Boolean) {
        repository.getOrders(page, pageSize).collect { result ->
            result.fold(
                onSuccess = { data ->
                    if (reset) allOrders.clear()
                    val newIds = data.content.map { it.id }.toSet()
                    allOrders.removeAll { it.id in newIds }
                    allOrders.addAll(data.content)
                    currentPage = data.page
                    _orders.value = UiState.Success(allOrders.toList())
                    _hasMore.value = (data.page + 1) < data.totalPages
                },
                onFailure = {
                    // Ro'yxat bo'sh bo'lsagina Error ko'rsatiladi, aks holda eski data qoladi
                    if (reset && allOrders.isEmpty()) {
                        _orders.value = UiState.Error(it.message ?: trNow("order_load_error"))
                    }
                }
            )
        }
    }

    fun loadOrderDetail(id: String) {
        currentOrderId = id
        viewModelScope.launch {
            _orderDetail.value = UiState.Loading
            repository.getOrderById(id).collect { result ->
                result.fold(
                    onSuccess = { _orderDetail.value = UiState.Success(it) },
                    onFailure = { _orderDetail.value = UiState.Error(it.message ?: trNow("order_load_error")) }
                )
            }
        }
    }

    fun placeOrder(request: CreateOrderRequest, onSuccess: () -> Unit) {
        if (request.deliveryAddressId == null && request.deliveryAddress.isNullOrBlank()) {
            _placeError.value = trNow("checkout_address_required")
            return
        }
        if (request.items.isEmpty()) {
            _placeError.value = trNow("checkout_empty_title")
            return
        }
        viewModelScope.launch {
            _placing.value = true
            _placeError.value = null
            repository.createOrder(request)
                .onSuccess {
                    _placing.value = false
                    loadOrders()
                    onSuccess()
                }
                .onFailure {
                    _placing.value = false
                    _placeError.value = it.message ?: trNow("order_place_error")
                }
        }
    }

    fun cancelOrder(id: String, reason: String? = null) {
        viewModelScope.launch {
            _cancellingIds.value = _cancellingIds.value + id
            _cancelError.value = null
            repository.cancelOrder(id, reason).fold(
                onSuccess = {
                    loadOrders()
                    _orderDetail.value = UiState.Success(it)
                },
                // Bekor qilish xatosi endi yutilmaydi — UI Snackbar'da ko'rsatadi
                onFailure = {
                    _cancelError.value = it.message ?: trNow("order_cancel_failed")
                }
            )
            _cancellingIds.value = _cancellingIds.value - id
        }
    }

    fun consumeCancelError() {
        _cancelError.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                OrderViewModel(OrderRepository(RetrofitClient.orderApiService))
            }
        }
    }
}

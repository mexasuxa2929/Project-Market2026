package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.MexaMarketApp
import com.example.mobile_app.data.local.AppDatabase
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.model.PublicReview
import com.example.mobile_app.data.model.RatingStats
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.ProductRepository
import com.example.mobile_app.data.repository.ReviewRepository
import com.example.mobile_app.data.repository.WishlistRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _product  = MutableStateFlow<UiState<Product>>(UiState.Loading)
    val product: StateFlow<UiState<Product>> = _product.asStateFlow()

    private val _reviews  = MutableStateFlow<List<PublicReview>>(emptyList())
    val reviews: StateFlow<List<PublicReview>> = _reviews.asStateFlow()

    private val _ratingStats = MutableStateFlow<RatingStats?>(null)
    val ratingStats: StateFlow<RatingStats?> = _ratingStats.asStateFlow()

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

    private val _recommendedProducts = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val recommendedProducts: StateFlow<UiState<List<Product>>> = _recommendedProducts.asStateFlow()

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    /** Tanlangan rang-varianti id (default — ko'rsatilayotgan mahsulotning o'zi). */
    private val _selectedVariantId = MutableStateFlow<String?>(null)
    val selectedVariantId: StateFlow<String?> = _selectedVariantId.asStateFlow()

    /** Savatga qo'shiladigan id — aynan select qilingan variant. */
    fun effectiveProductId(): String = _selectedVariantId.value ?: currentProductId.orEmpty()

    fun checkFavorite(productId: String) {
        viewModelScope.launch {
            wishlistRepository.checkFavorite(productId).onSuccess { _isFavorited.value = it }
        }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            val current = _isFavorited.value
            wishlistRepository.toggleFavorite(productId, current).onSuccess {
                _isFavorited.value = !current
            }
        }
    }

    fun load(productId: String) {
        currentProductId = productId
        _selectedVariantId.value = productId
        viewModelScope.launch {
            _product.value = UiState.Loading
            val t1 = async { loadProduct(productId) }
            val t2 = async { loadReviews(productId) }
            val t3 = async { loadRatingStats(productId) }
            val t4 = async { checkFavorite(productId) }
            t1.await(); t2.await(); t3.await(); t4.await()
        }
    }

    private var currentProductId: String? = null

    init {
        viewModelScope.launch {
            com.example.mobile_app.util.AppLanguage.flow.collect {
                currentProductId?.let { loadProduct(it) }
                loadRecommended()
            }
        }
    }

    private suspend fun loadProduct(id: String) {
        productRepository.getProductById(id).collect { result ->
            result.fold(
                onSuccess = { _product.value = UiState.Success(it) },
                onFailure = { _product.value = UiState.Error(it.message ?: "Xatolik") }
            )
        }
    }

    private suspend fun loadReviews(productId: String) {
        reviewRepository.getProductReviews(productId, page = 0, size = 5).collect { result ->
            result.onSuccess { _reviews.value = it }
        }
    }

    private suspend fun loadRatingStats(productId: String) {
        reviewRepository.getRatingStats(productId).collect { result ->
            result.onSuccess { _ratingStats.value = it }
        }
    }

    fun loadRecommended() {
        viewModelScope.launch {
            _recommendedProducts.value = UiState.Loading
            productRepository.getTrendingProducts().collect { result ->
                result.fold(
                    onSuccess = { _recommendedProducts.value = UiState.Success(it) },
                    onFailure = { _recommendedProducts.value = UiState.Error(it.message ?: "Xatolik") }
                )
            }
        }
    }

    /**
     * Rang tanlash — faqat select belgisi ko'chadi (navigatsiya YO'Q, qayta yuklash YO'Q).
     * Mahsulot rasmi, narxi, ranglar ketma-ketligi o'zgarmaydi.
     * Savatga va orderga aynan shu tanlangan variant id tushadi.
     */
    fun selectSiblingColor(productId: String) {
        _selectedVariantId.value = productId
    }

    fun increment() { if (_quantity.value < 99) _quantity.value++ }
    fun decrement() { if (_quantity.value > 1)  _quantity.value-- }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProductDetailViewModel(
                    ProductRepository(
                        RetrofitClient.apiService,
                        AppDatabase.get(MexaMarketApp.instance).catalogProductDao()
                    ),
                    ReviewRepository(RetrofitClient.reviewApiService),
                    WishlistRepository(RetrofitClient.wishlistApiService)
                )
            }
        }
    }
}

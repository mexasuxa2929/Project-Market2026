package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.MexaMarketApp
import com.example.mobile_app.data.local.AppDatabase
import com.example.mobile_app.data.model.Brand
import com.example.mobile_app.data.model.Category
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.model.RecommendedProduct
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.ProductRepository
import com.example.mobile_app.data.repository.ReviewRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeViewModel(
    private val repository: ProductRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _trendingProducts = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val trendingProducts: StateFlow<UiState<List<Product>>> = _trendingProducts.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _brands = MutableStateFlow<UiState<List<Brand>>>(UiState.Loading)
    val brands: StateFlow<UiState<List<Brand>>> = _brands.asStateFlow()

    private val _recommended = MutableStateFlow<List<RecommendedProduct>>(emptyList())
    val recommended: StateFlow<List<RecommendedProduct>> = _recommended.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    // Pull-to-refresh indikatori — eski ro'yxat ekranda qoladi, full-screen Loading ko'rsatilmaydi
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentCursor: String? = null
    // 10 -> 20: sahifa soni 2 baravar kamayadi (11 emas ~6 so'rov), scroll ravonlashadi
    private val pageSize = 20
    private val allProducts = mutableListOf<Product>()

    init {
        loadAll()
        observeLanguage()
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            // drop(1): startdagi joriy qiymat qayta loadAll() chaqirmasligi uchun
            // (aks holda init dagi loadAll() bilan 2 marta = 8 ta so'rov ketadi)
            com.example.mobile_app.util.AppLanguage.flow.drop(1).collect {
                loadAll()
            }
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            currentCursor = null
            _hasMorePages.value = true
            // Birinchi yuklanishda skeleton, keyingilarida eski data + pull-indikator
            val firstLoad = allProducts.isEmpty()
            if (firstLoad) {
                _trendingProducts.value = UiState.Loading
            } else {
                _isRefreshing.value = true
            }
            // 2-bosqich: bitta bundle so'rov — muvaffaqiyatli bo'lsa shu kifoya (1 round-trip)
            val bundleOk = loadHomeBundle()
            if (!bundleOk) {
                // Fallback: eski 4 ta parallel so'rov (backend yangilanmagan bo'lsa ham ishlaydi)
                val t1 = async { loadTrendingProducts(reset = true) }
                val t2 = async { loadBrands() }
                val t3 = async { loadCategories() }
                val t4 = async { loadRecommended() }
                t1.await(); t2.await(); t3.await(); t4.await()
            }
            _isRefreshing.value = false
        }
    }

    /**
     * Bundle muvaffaqiyatli bo'lsa true qaytaradi. Bo'sh trending bo'lsa false —
     * fallback ishga tushadi.
     */
    private suspend fun loadHomeBundle(): Boolean {
        var ok = false
        repository.getHomeBundle().collect { result ->
            result.fold(
                onSuccess = { bundle ->
                    if (bundle.trending.isEmpty()) {
                        ok = false
                        return@fold
                    }
                    val shuffled = bundle.trending.shuffled(Random(System.currentTimeMillis()))
                    allProducts.clear()
                    allProducts.addAll(shuffled)
                    // Keyingi sahifalar eski cursor API orqali yuklanadi
                    currentCursor = bundle.trendingNextCursor
                    _trendingProducts.update { UiState.Success(allProducts.toList()) }
                    _hasMorePages.value = bundle.trendingHasMore
                    if (bundle.brands.isNotEmpty()) {
                        _brands.value = UiState.Success(bundle.brands)
                    }
                    if (bundle.categories.isNotEmpty()) {
                        _categories.value = bundle.categories
                    }
                    if (bundle.recommended.isNotEmpty()) {
                        _recommended.value = bundle.recommended.shuffled(Random(System.currentTimeMillis()))
                    }
                    prefetchThumbnails(shuffled)
                    ok = true
                },
                onFailure = { ok = false }
            )
        }
        return ok
    }

    private suspend fun loadTrendingProducts(reset: Boolean = false) {
        if (reset) {
            currentCursor = null
        }
        repository.getProductsPage(cursor = currentCursor, size = pageSize).collect { result ->
            result.fold(
                onSuccess = { page ->
                    val shuffled = page.products.shuffled(Random(System.currentTimeMillis()))
                    if (reset) {
                        // Eski ro'yxat yangi data kelguncha ekranda qoladi (miltilash yo'q)
                        allProducts.clear()
                    }
                    val newIds = shuffled.map { it.id }.toSet()
                    allProducts.removeAll { it.id in newIds }
                    allProducts.addAll(shuffled)
                    currentCursor = page.nextCursor
                    val snapshot = allProducts.toList()
                    _trendingProducts.update { UiState.Success(snapshot) }
                    _hasMorePages.value = page.hasMore
                    // Keyin ko'rinadigan rasmlarni oldindan keshga yuklash
                    prefetchThumbnails(shuffled)
                },
                onFailure = { error ->
                    // Ro'yxat bo'sh bo'lsagina Error ko'rsatiladi, aks holda eski data qoladi
                    if (reset && allProducts.isEmpty()) {
                        _trendingProducts.update { UiState.Error(error.message ?: "Xatolik") }
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMorePages.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            loadTrendingProducts(reset = false)
            _isLoadingMore.value = false
        }
    }

    private suspend fun loadCategories() {
        repository.getCategories().collect { result ->
            result.onSuccess { _categories.value = it }
        }
    }

    private suspend fun loadBrands() {
        _brands.value = UiState.Loading
        repository.getBrands().collect { result ->
            result.fold(
                onSuccess = { _brands.value = UiState.Success(it) },
                onFailure = { _brands.value = UiState.Error(it.message ?: "Brendlarni yuklashda xatolik") }
            )
        }
    }

    private suspend fun loadRecommended() {
        reviewRepository.getRecommended(10, discountOnly = true).collect { result ->
            result.onSuccess { _recommended.value = it.shuffled(Random(System.currentTimeMillis())) }
        }
    }

    fun refresh() {
        loadAll()
    }

    /**
     * Yangi sahifa rasmlarini UI chizib ulgurmasdan oldin Coil keshiga yuklaydi —
     * scroll qilganda rasm darhol keshdan chiqadi (kutish yo'q).
     */
    private fun prefetchThumbnails(products: List<Product>) {
        if (products.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = MexaMarketApp.instance
                val imageLoader = coil3.SingletonImageLoader.get(context)
                products.take(20).forEach { product ->
                    val url = product.thumbnail ?: return@forEach
                    val request = coil3.request.ImageRequest.Builder(context)
                        .data(url)
                        .size(512, 512)
                        .precision(coil3.size.Precision.INEXACT)
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .build()
                    imageLoader.enqueue(request)
                }
            } catch (_: Exception) {
                // Prefetch ixtiyoriy — xato bo'lsa jim o'tkaziladi
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    ProductRepository(
                        RetrofitClient.apiService,
                        AppDatabase.get(MexaMarketApp.instance).catalogProductDao()
                    ),
                    ReviewRepository(RetrofitClient.reviewApiService)
                )
            }
        }
    }
}

package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mobile_app.MexaMarketApp
import com.example.mobile_app.data.local.AppDatabase
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Bitta kategoriya mahsulotlari sahifasi uchun ViewModel.
 * categoryId ekrandan beriladi — oqim shu kategoriyaga filtrlangan.
 * Qo'shimcha: narx filtri (min/max) — serverda DB bo'yicha filtrlanadi (M-H6).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryProductsViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    data class PriceRange(val min: Double?, val max: Double?)

    private val _priceRange = MutableStateFlow<PriceRange?>(null)
    val priceRange: StateFlow<PriceRange?> = _priceRange.asStateFlow()

    fun setPriceFilter(min: Double?, max: Double?) {
        _priceRange.value = if (min == null && max == null) null else PriceRange(min, max)
    }

    fun productsFor(categoryId: String): Flow<PagingData<Product>> =
        _priceRange.flatMapLatest { range ->
            repository.getProductsPager(
                search = "",
                categoryId = categoryId,
                sort = null,
                minPrice = range?.min,
                maxPrice = range?.max
            )
        }.cachedIn(viewModelScope)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CategoryProductsViewModel(
                    ProductRepository(
                        RetrofitClient.apiService,
                        AppDatabase.get(MexaMarketApp.instance).catalogProductDao()
                    )
                )
            }
        }
    }
}

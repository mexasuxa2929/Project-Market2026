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
import kotlinx.coroutines.flow.Flow

/**
 * Bitta kategoriya mahsulotlari sahifasi uchun ViewModel.
 * categoryId ekrandan beriladi — oqim shu kategoriyaga filtrlangan.
 */
class CategoryProductsViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    fun productsFor(categoryId: String): Flow<PagingData<Product>> =
        repository.getProductsPager(search = "", categoryId = categoryId, sort = null)
            .cachedIn(viewModelScope)

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

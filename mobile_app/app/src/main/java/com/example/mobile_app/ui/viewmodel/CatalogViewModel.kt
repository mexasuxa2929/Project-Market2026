package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mobile_app.MexaMarketApp
import com.example.mobile_app.data.local.AppDatabase
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.ProductRepository
import com.example.mobile_app.util.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(FlowPreview::class, ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
class CatalogViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _selectedSort = MutableStateFlow<String?>(null)
    val selectedSort: StateFlow<String?> = _selectedSort.asStateFlow()

    private val _sortAsc = MutableStateFlow(true)
    val sortAsc: StateFlow<Boolean> = _sortAsc.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    /**
     * Paging 3 oqimi: qidiruv (debounce bilan) va til almashish har birida
     * pager qayta boshlanadi. Room kesh tufayli offline'da ham ishlaydi.
     */
    private val searchAndRefresh = combine(
        _searchQuery.debounce(400).distinctUntilChanged(),
        _refreshTrigger
    ) { query, _ -> query }.distinctUntilChanged()

    val products: Flow<PagingData<Product>> = combine(
        searchAndRefresh,
        _selectedCategoryId,
        _selectedSort,
        _sortAsc,
        AppLanguage.flow
    ) { query, catId, sort, asc, _ ->
        val resolvedSort = when (sort) {
            "price" -> if (asc) "price_asc" else "price_desc"
            "name"  -> if (asc) "name_asc" else "name_desc"
            "newest"-> "newest"
            "rating"-> "rating_desc"
            "popular"-> "popular"
            else -> null
        }
        Triple(query, catId, resolvedSort)
    }
        // Faqat haqiqiy o'zgarishda yangi Pager: Triple data class bo'lgani uchun
        // teng qiymatlar filtre qilinadi (masalan sort==null da sortAsc toggle).
        // StateFlow'lar o'zi distinct, bu — qo'shimcha himoya.
        .distinctUntilChanged()
        .flatMapLatest { (query, catId, sort) -> repository.getProductsPager(query, catId, sort) }
        .cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(categoryId: String?) {
        // Toggle mantiqi VM'da: bir xil kategoriya qayta bosilsa deselect (null),
        // aks holda select. Qiymat o'zgarmasa StateFlow qayta emit qilmaydi,
        // shuning uchun UI'dagi onClick lambda'lar selectedCategoryId'ga bog'lanmaydi
        // va har safar qayta yaratilmaydi (stable reference).
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun onSortSelect(sort: String) {
        _selectedSort.value = if (_selectedSort.value == sort) null else sort
    }

    fun onSortDirectionToggle() {
        _sortAsc.value = !_sortAsc.value
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CatalogViewModel(
                    ProductRepository(
                        RetrofitClient.apiService,
                        AppDatabase.get(MexaMarketApp.instance).catalogProductDao()
                    )
                )
            }
        }
    }
}
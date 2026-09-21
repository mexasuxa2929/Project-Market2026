package com.example.mobile_app.ui.viewmodel

import android.content.Context
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
import kotlinx.coroutines.flow.flowOf

/** Search ekrani: qidiruv natijalari Paging 3 (offline kesh bilan) orqali keladi */
@OptIn(FlowPreview::class, ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    /** Bo'sh so'rov = bo'sh PagingData (ekran "Idle" holatga qaytadi) */
    val results: Flow<PagingData<Product>> = combine(
        _query.debounce(400).distinctUntilChanged(),
        AppLanguage.flow
    ) { q, _ -> q }
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(PagingData.empty())
            else repository.getProductsPager(q)
        }
        .cachedIn(viewModelScope)

    private val prefs
        get() = MexaMarketApp.instance.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

    init {
        loadRecentSearches()
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _query.value = query
        saveRecentSearch(query)
    }

    fun clearRecent() {
        _recentSearches.value = emptyList()
        prefs.edit().remove(KEY_RECENT).apply()
    }

    fun removeRecent(index: Int) {
        val list = _recentSearches.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _recentSearches.value = list
            prefs.edit().putStringSet(KEY_RECENT, list.toSet()).apply()
        }
    }

    private fun loadRecentSearches() {
        val set = prefs.getStringSet(KEY_RECENT, emptySet()) ?: emptySet()
        _recentSearches.value = set.toList().take(MAX_RECENT)
    }

    private fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val list = _recentSearches.value.filter { it != trimmed }.toMutableList()
        list.add(0, trimmed)
        val capped = list.take(MAX_RECENT)
        _recentSearches.value = capped
        prefs.edit().putStringSet(KEY_RECENT, capped.toSet()).apply()
    }

    companion object {
        private const val KEY_RECENT = "recent_searches"
        private const val MAX_RECENT = 8

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    ProductRepository(
                        RetrofitClient.apiService,
                        AppDatabase.get(MexaMarketApp.instance).catalogProductDao()
                    )
                )
            }
        }
    }
}
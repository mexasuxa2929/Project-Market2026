package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.data.model.wishlist.WishlistEntryResponse
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.WishlistRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WishlistViewModel(private val repository: WishlistRepository) : ViewModel() {

    private val _wishlist = MutableStateFlow<UiState<List<WishlistEntryResponse>>>(UiState.Loading)
    val wishlist: StateFlow<UiState<List<WishlistEntryResponse>>> = _wishlist.asStateFlow()

    private val _error = Channel<String>(Channel.BUFFERED)
    val error = _error.receiveAsFlow()

    private val _favoritedIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritedIds: StateFlow<Set<String>> = _favoritedIds.asStateFlow()

    private val toggleMutex = Mutex()

    init {
        loadWishlist()
        observeLanguage()
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            com.example.mobile_app.util.AppLanguage.flow.drop(1).collect {
                loadWishlist()
            }
        }
    }

    fun loadWishlist() {
        viewModelScope.launch {
            _wishlist.value = UiState.Loading
            repository.listWishlist().collect { result ->
                result.fold(
                    onSuccess = {
                        _wishlist.value = UiState.Success(it)
                        _favoritedIds.value = it.map { e -> e.productId }.toSet()
                    },
                    onFailure = {
                        _wishlist.value = UiState.Error(it.message ?: "Xatolik")
                    }
                )
            }
        }
    }

    /** UI uchun qulay overload: joriy holatni VM ning o'zi aniqlaydi (UI'da business logic bo'lmasin). */
    fun toggleFavorite(productId: String) {
        toggleFavorite(productId, productId in _favoritedIds.value)
    }

    fun toggleFavorite(productId: String, currentlyFavorited: Boolean) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val previousIds = _favoritedIds.value

                if (currentlyFavorited) {
                    _favoritedIds.value = previousIds - productId
                } else {
                    _favoritedIds.value = previousIds + productId
                }

                val result = repository.toggleFavorite(productId, currentlyFavorited)
                result.onFailure {
                    _favoritedIds.value = previousIds
                    _error.send(it.message ?: "Sevimlilarni yangilashda xatolik")
                }
                loadWishlist()
            }
        }
    }

    fun removeFromWishlist(productId: String) {
        toggleFavorite(productId, currentlyFavorited = true)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { WishlistViewModel(WishlistRepository(RetrofitClient.wishlistApiService)) }
        }
    }
}

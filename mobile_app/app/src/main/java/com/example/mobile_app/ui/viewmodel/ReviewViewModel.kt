package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mobile_app.data.model.MyReview
import com.example.mobile_app.data.model.PublicReview
import com.example.mobile_app.data.model.RatingStats
import com.example.mobile_app.data.model.RecommendedProduct
import com.example.mobile_app.data.repository.ReviewRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(private val repo: ReviewRepository) : ViewModel() {

    // ── Product reviews ───────────────────────────────────────────────────────
    private val _reviews = MutableStateFlow<UiState<List<PublicReview>>>(UiState.Loading)
    val reviews: StateFlow<UiState<List<PublicReview>>> = _reviews.asStateFlow()

    // ── Rating stats ──────────────────────────────────────────────────────────
    private val _ratingStats = MutableStateFlow<RatingStats?>(null)
    val ratingStats: StateFlow<RatingStats?> = _ratingStats.asStateFlow()

    // ── My review ─────────────────────────────────────────────────────────────
    private val _myReview = MutableStateFlow<MyReview?>(null)
    val myReview: StateFlow<MyReview?> = _myReview.asStateFlow()

    // ── Submit state ──────────────────────────────────────────────────────────
    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    // ── Recommendations ───────────────────────────────────────────────────────
    private val _recommended = MutableStateFlow<List<RecommendedProduct>>(emptyList())
    val recommended: StateFlow<List<RecommendedProduct>> = _recommended.asStateFlow()

    private var currentProductId: String? = null

    init {
        // Til o'zgarganda joriy mahsulot ma'lumotlarini qayta yuklash
        viewModelScope.launch {
            com.example.mobile_app.util.AppLanguage.flow.collect {
                currentProductId?.let { id ->
                    loadProductReviews(id)
                    loadRatingStats(id)
                    loadMyReview(id)
                }
                loadRecommended()
            }
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadProductReviews(productId: String) {
        currentProductId = productId
        viewModelScope.launch {
            _reviews.value = UiState.Loading
            repo.getProductReviews(productId).collect { result ->
                _reviews.value = result.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { UiState.Error(it.message ?: "Xatolik") }
                )
            }
        }
    }

    fun loadRatingStats(productId: String) {
        viewModelScope.launch {
            repo.getRatingStats(productId).collect { result ->
                result.onSuccess { _ratingStats.value = it }
            }
        }
    }

    fun loadMyReview(productId: String) {
        viewModelScope.launch {
            repo.getMyReview(productId).collect { result ->
                result.onSuccess { _myReview.value = it }
            }
        }
    }

    fun loadRecommended(limit: Int = 10) {
        viewModelScope.launch {
            repo.getRecommended(limit).collect { result ->
                result.onSuccess { _recommended.value = it }
            }
        }
    }

    // ── Submit review ─────────────────────────────────────────────────────────

    fun submitReview(productId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _submitState.value = SubmitState.Loading
            repo.submitReview(productId, rating, comment)
                .fold(
                    onSuccess = { review ->
                        _myReview.value = review
                        _submitState.value = SubmitState.Success
                        // Statsni yangilash
                        loadRatingStats(productId)
                        loadProductReviews(productId)
                    },
                    onFailure = { _submitState.value = SubmitState.Error(it.message ?: "Xatolik") }
                )
        }
    }

    fun resetSubmitState() { _submitState.value = SubmitState.Idle }

    sealed class SubmitState {
        data object Idle    : SubmitState()
        data object Loading : SubmitState()
        data object Success : SubmitState()
        data class  Error(val message: String) : SubmitState()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repo: ReviewRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReviewViewModel(repo) as T
    }
}

package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.data.model.notification.AppNotification
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.NotificationRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _notifications = MutableStateFlow<UiState<List<AppNotification>>>(UiState.Loading)
    val notifications: StateFlow<UiState<List<AppNotification>>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        load()
        refreshUnreadCount()
        observeLanguage()
    }

    /** Til o'zgarganda bildirishnomalarni qayta yuklash */
    private fun observeLanguage() {
        viewModelScope.launch {
            com.example.mobile_app.util.AppLanguage.flow.collect {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _notifications.value = UiState.Loading
            repository.getMyNotifications().collect { result ->
                result.fold(
                    onSuccess = {
                        _notifications.value = UiState.Success(it)
                        refreshUnreadCount()
                    },
                    onFailure = { _notifications.value = UiState.Error(it.message ?: "Xatolik yuz berdi") }
                )
            }
        }
    }

    /** Realtime event (NOTIFICATION_CREATED) kelganda jimgina yangilash — spinner ko'rsatmaydi */
    fun refreshSilently() {
        viewModelScope.launch {
            repository.getMyNotifications().collect { result ->
                result.fold(
                    onSuccess = {
                        _notifications.value = UiState.Success(it)
                        refreshUnreadCount()
                    },
                    onFailure = { /* jimgina yangilash xatosi — joriy holat saqlanadi */ }
                )
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount().collect { result ->
                result.onSuccess { _unreadCount.value = it.toInt() }
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            repository.markRead(id).onSuccess {
                val current = _notifications.value
                if (current is UiState.Success) {
                    _notifications.value = UiState.Success(
                        current.data.map {
                            if (it.id == id) it.copy(read = true) else it
                        }
                    )
                }
                refreshUnreadCount()
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead().onSuccess {
                val current = _notifications.value
                if (current is UiState.Success) {
                    _notifications.value = UiState.Success(current.data.map { it.copy(read = true) })
                }
                _unreadCount.value = 0
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NotificationViewModel(NotificationRepository(RetrofitClient.notificationApiService))
            }
        }
    }
}
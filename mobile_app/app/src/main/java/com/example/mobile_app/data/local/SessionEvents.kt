package com.example.mobile_app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sessiya tugaganini butun appga xabar qiladi.
 * TokenAuthenticator refresh muvaffaqiyatsiz bo'lganda expire() chaqiradi,
 * AppNavigation esa foydalanuvchini Login ekraniga qaytaradi.
 */
object SessionEvents {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    fun expire() {
        _sessionExpired.value = true
    }

    fun reset() {
        _sessionExpired.value = false
    }
}
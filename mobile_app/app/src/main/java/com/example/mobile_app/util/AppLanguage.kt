package com.example.mobile_app.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppLanguage {
    var current: Language by mutableStateOf(Language.UZ)

    private val _flow = MutableStateFlow(Language.UZ)
    val flow: StateFlow<Language> = _flow

    fun update(lang: Language) {
        current = lang
        _flow.value = lang
    }
}
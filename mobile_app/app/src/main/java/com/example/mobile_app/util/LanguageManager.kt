package com.example.mobile_app.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class LanguageManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_lang", Context.MODE_PRIVATE)

    val currentLanguage = mutableStateOf(getPersistedLanguage())

    init {
        AppLanguage.update(currentLanguage.value)
    }

    private fun getPersistedLanguage(): Language {
        val code = prefs.getString("lang", "UZ") ?: "UZ"
        return try { Language.valueOf(code) } catch (_: Exception) { Language.UZ }
    }

    fun setLanguage(lang: Language) {
        currentLanguage.value = lang
        AppLanguage.update(lang)
        prefs.edit().putString("lang", lang.name).apply()
    }
}

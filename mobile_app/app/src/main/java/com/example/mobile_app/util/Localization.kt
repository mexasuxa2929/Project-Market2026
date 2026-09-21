package com.example.mobile_app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalLanguage = compositionLocalOf { Language.UZ }

@Composable
fun tr(key: String): String = Translator.get(key, LocalLanguage.current)

/**
 * Composable bo'lmagan joylar (ViewModel, repository) uchun tarjima.
 * AppLanguage.current orqali joriy til o'qiladi — reaktiv emas,
 * shuning uchun til almashganda VM qayta yuklashda yangi tilni oladi.
 */
fun trNow(key: String): String = Translator.get(key, AppLanguage.current)

@Composable
fun LanguageProvider(language: Language, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLanguage provides language, content = content)
}

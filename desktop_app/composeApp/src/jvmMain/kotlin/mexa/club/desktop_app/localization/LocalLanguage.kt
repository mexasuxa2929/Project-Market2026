package mexa.club.desktop_app.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

data class LanguageController(
    val language: AppLanguage,
    val setLanguage: (AppLanguage) -> Unit,
)

val LocalLanguage = compositionLocalOf<LanguageController> {
    error("Warehouse i18n: CompositionLocalProvider(LocalLanguage) bilan o‘rang.")
}

@Composable
fun appLanguage(): AppLanguage = LocalLanguage.current.language

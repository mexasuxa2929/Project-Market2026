package com.example.mobile_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import com.example.mobile_app.navigation.AppNavigation
import com.example.mobile_app.ui.theme.MexaMarketTheme
import com.example.mobile_app.util.LanguageManager
import com.example.mobile_app.util.LanguageProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ilova ochiq (light) dizaynda — status/nav bar ikonkalari doim qora bo'ladi,
        // tepa panelda qora chiziq ko'rinmaydi, hamma ekran bir xil ochiq ohangda.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val languageManager = remember { LanguageManager(this) }
            val language = languageManager.currentLanguage
            MexaMarketTheme {
                LanguageProvider(language = language.value) {
                    AppNavigation(
                        navController = rememberNavController(),
                        languageManager = languageManager
                    )
                }
            }
        }
    }
}

package mexa.club.desktop_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import mexa.club.desktop_app.di.initKoin
import mexa.club.desktop_app.di.shutdownKoin
import mexa.club.desktop_app.market.ui.maps.JcefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = application {
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            JcefManager.init() // ~100MB download first time
        }
        initKoin()
        started = true
    }

    val windowState = rememberWindowState(width = 1200.dp, height = 700.dp)
    Window(
        onCloseRequest = {
            JcefManager.dispose()
            shutdownKoin()
            exitApplication()
        },
        title = "MEXA WAREHOUSE",
        state = windowState,
    ) {
        if (!started) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            App()
        }
    }
}

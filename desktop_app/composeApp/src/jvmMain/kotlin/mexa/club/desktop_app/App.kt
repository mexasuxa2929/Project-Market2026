package mexa.club.desktop_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.auth.TokenRefreshManager
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.LanguageController
import mexa.club.desktop_app.localization.LocalLanguage
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.api.UnauthorizedNotifier
import mexa.club.desktop_app.settings.AppPreferences
import mexa.club.desktop_app.market.session.MarketSessionGate
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.market.session.bootstrapSessionFromGateway
import mexa.club.desktop_app.navigation.DashboardAppScreen
import mexa.club.desktop_app.navigation.LoginAppScreen
import mexa.club.desktop_app.theme.WarehouseTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun App() {
    // O-43: til restartda yo'qolmasin — AppPreferences'dan tiklanadi
    var appLanguage by remember {
        mutableStateOf(
            AppLanguage.entries.firstOrNull { it.name == AppPreferences.languageCode }
                ?: AppLanguage.UZ
        )
    }

    val initialScreen = remember {
        AuthSession.restoreFromDisk()
        if (AuthSession.isLoggedIn && AuthSession.hasAdminAccess) {
            DashboardAppScreen
        } else {
            AuthSession.clear()
            LoginAppScreen
        }
    }

    // S-49: prod'da http (loopback emas) manzil taqiqlanadi — ilova boshlanmaydi.
    if (!AppPreferences.isUrlAllowed(AppPreferences.baseUrl)) {
        InsecureUrlScreen()
        return
    }

    CompositionLocalProvider(
        LocalLanguage provides LanguageController(appLanguage) {
            appLanguage = it
            AppPreferences.languageCode = it.name // O-43: tanlangan til saqlanadi
        },
    ) {
        KoinContext {
            WarehouseTheme {
                val authApi: AuthApiClient = koinInject {
                    parametersOf(AppStrings.authNetwork(appLanguage))
                }
                Navigator(initialScreen) { navigator ->
                    val onDashboard = navigator.lastItem is DashboardAppScreen

                    // O-44: appLanguage kalibdan olib tashlandi — til o'zgarganda
                    // sessiya qayta bootstrap QILINMAYDI (faqat UI matnlari almashadi).
                    LaunchedEffect(onDashboard) {
                        if (onDashboard) {
                            MarketSessionGate.setReady(false)
                            GatewayRealtimeHub.stop()
                            val sessionOk = withContext(Dispatchers.IO) {
                                authApi.refreshAccessOnAppResume()
                            }
                            if (!sessionOk) {
                                MarketSessionGate.setReady(false)
                                SessionManager.clearGatewaySession()
                                AuthSession.clear()
                                mexa.club.desktop_app.market.ui.maps.MapScreenStateHolder.reset() // S-50
                                navigator.replaceAll(LoginAppScreen)
                                TokenRefreshManager.stop()
                                return@LaunchedEffect
                            }
                            if (!AuthSession.hasAdminAccess) {
                                MarketSessionGate.setReady(false)
                                SessionManager.clearGatewaySession()
                                AuthSession.clear()
                                mexa.club.desktop_app.market.ui.maps.MapScreenStateHolder.reset() // S-50
                                navigator.replaceAll(LoginAppScreen)
                                TokenRefreshManager.stop()
                                return@LaunchedEffect
                            }
                            val access = AuthSession.accessToken
                            val refresh = AuthSession.refreshToken
                            if (access.isNullOrBlank()) {
                                MarketSessionGate.setReady(false)
                                AuthSession.clear()
                                SessionManager.clearGatewaySession()
                                mexa.club.desktop_app.market.ui.maps.MapScreenStateHolder.reset() // S-50
                                navigator.replaceAll(LoginAppScreen)
                                TokenRefreshManager.stop()
                                return@LaunchedEffect
                            }
                            SessionManager.setSession(access, refresh, null)
                            withContext(Dispatchers.IO) {
                                bootstrapSessionFromGateway(access, refresh)
                            }
                            MarketSessionGate.setReady(true)
                            // Dashboard birinchi yuklansin; SSE parallel socket ochmasin (Windows).
                            delay(1_500)
                            if (navigator.lastItem is DashboardAppScreen) {
                                GatewayRealtimeHub.start()
                            }
                            TokenRefreshManager.start(authApi) {
                                MarketSessionGate.setReady(false)
                                GatewayRealtimeHub.stop()
                                mexa.club.desktop_app.market.ui.maps.MapScreenStateHolder.reset() // S-50
                                navigator.replaceAll(LoginAppScreen)
                            }
                        } else {
                            MarketSessionGate.setReady(false)
                            GatewayRealtimeHub.stop()
                            TokenRefreshManager.stop()
                        }
                    }

                    if (onDashboard) {
                        SideEffect {
                            // O-44: faqat sessiya o'rnatilmagan bo'lsa yoziladi —
                            // har recompositionda qayta yozilmaydi (401'da clear
                            // keyingi recompositionda bekor bo'lmasligi uchun).
                            if (SessionManager.token.isEmpty()) {
                                AuthSession.accessToken?.takeIf { it.isNotBlank() }?.let { t ->
                                    SessionManager.setSession(
                                        t,
                                        AuthSession.refreshToken,
                                        SessionManager.currentUser,
                                    )
                                }
                            }
                        }
                    }

                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(Unit) {
                        UnauthorizedNotifier.events.collect { msg ->
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = msg,
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        }
                    }

                    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
                        SlideTransition(navigator)
                    }
                }
            }
        }
    }


}

// S-49: xavfsiz bo'lmagan (http, loopback emas) URL topilsa — bloklovchi ekran.
@Composable
private fun InsecureUrlScreen() {
    WarehouseTheme {
        var urlReset by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Server URL xavfsiz emas: ${'$'}{AppPreferences.baseUrl}",
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "Nusxa (https://) yoki lokal (http://127.0.0.1:8080) manzildan foydalaning.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!urlReset) {
                    Button(onClick = { AppPreferences.resetUrl(); urlReset = true }) {
                        Text("Lokal URL ga qaytarish")
                    }
                } else {
                    Text(
                        "Qaytarildi — ilovani qayta ishga tushiring.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

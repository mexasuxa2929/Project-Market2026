package mexa.club.desktop_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var appLanguage by remember { mutableStateOf(AppLanguage.UZ) }

    val initialScreen = remember {
        AuthSession.restoreFromDisk()
        if (AuthSession.isLoggedIn && AuthSession.hasAdminAccess) {
            DashboardAppScreen
        } else {
            AuthSession.clear()
            LoginAppScreen
        }
    }

    CompositionLocalProvider(
        LocalLanguage provides LanguageController(appLanguage) { appLanguage = it },
    ) {
        KoinContext {
            WarehouseTheme {
                val authApi: AuthApiClient = koinInject {
                    parametersOf(AppStrings.authNetwork(appLanguage))
                }
                Navigator(initialScreen) { navigator ->
                    val onDashboard = navigator.lastItem is DashboardAppScreen

                    LaunchedEffect(onDashboard, appLanguage) {
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
                                navigator.replaceAll(LoginAppScreen)
                                TokenRefreshManager.stop()
                                return@LaunchedEffect
                            }
                            if (!AuthSession.hasAdminAccess) {
                                MarketSessionGate.setReady(false)
                                SessionManager.clearGatewaySession()
                                AuthSession.clear()
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
                            AuthSession.accessToken?.takeIf { it.isNotBlank() }?.let { t ->
                                SessionManager.setSession(
                                    t,
                                    AuthSession.refreshToken,
                                    SessionManager.currentUser,
                                )
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

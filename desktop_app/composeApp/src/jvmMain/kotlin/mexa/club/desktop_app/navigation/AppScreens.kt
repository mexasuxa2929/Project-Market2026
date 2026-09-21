package mexa.club.desktop_app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.auth.AuthApiClient
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.auth.TokenRefreshManager
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.session.MarketSessionGate
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.market.ui.layout.SuperAdminDashboardLayout
import mexa.club.desktop_app.ui.auth.EmailVerificationScreen
import mexa.club.desktop_app.ui.auth.LoginScreen
import mexa.club.desktop_app.ui.auth.RegisterScreen
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class VerifyEmailArgs(
    val email: String,
    val username: String,
    val password: String,
)

data object LoginAppScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        LoginScreen(
            modifier = Modifier.fillMaxSize(),
            onLoginSuccess = { navigator.replace(DashboardAppScreen) },
            onNavigateToRegister = { navigator.push(RegisterAppScreen) },
        )
    }
}

data object RegisterAppScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        RegisterScreen(
            modifier = Modifier.fillMaxSize(),
            onRegistered = { email, username, password ->
                navigator.push(VerifyEmailAppScreen(VerifyEmailArgs(email, username, password)))
            },
            onNavigateToLogin = { navigator.pop() },
        )
    }
}

data class VerifyEmailAppScreen(
    private val args: VerifyEmailArgs,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        EmailVerificationScreen(
            modifier = Modifier.fillMaxSize(),
            email = args.email,
            username = args.username,
            password = args.password,
            onVerified = { navigator.replaceAll(LoginAppScreen) },
            onBackToLogin = { navigator.replaceAll(LoginAppScreen) },
        )
    }
}

data object DashboardAppScreen : Screen {
    @Composable
    override fun Content() {
        val lang = appLanguage()
        val scope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        val api: AuthApiClient = koinInject { parametersOf(AppStrings.authNetwork(lang)) }

        SuperAdminDashboardLayout(
            modifier = Modifier.fillMaxSize(),
            onLogout = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val access = AuthSession.accessToken.orEmpty()
                        val refresh = AuthSession.refreshToken.orEmpty()
                        if (access.isNotBlank() && refresh.isNotBlank()) {
                            runCatching { api.logout(access, refresh) }
                        }
                    }
                    MarketSessionGate.setReady(false)
                    GatewayRealtimeHub.stop()
                    SessionManager.clearGatewaySession()
                    AuthSession.clear()
                    TokenRefreshManager.stop()
                    mexa.club.desktop_app.market.ui.maps.MapScreenStateHolder.reset()
                    navigator.replaceAll(LoginAppScreen)
                }
            },
        )
    }
}

package mexa.club.desktop_app.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TokenRefreshManager {

    private var job: Job? = null

    private const val POLL_INTERVAL_MS = 15_000L

    fun start(
        api: AuthApiClient,
        onSessionExpired: () -> Unit,
    ) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            var first = true
            while (isActive) {
                if (!first) {
                    delay(POLL_INTERVAL_MS)
                }
                first = false
                val refresh = AuthSession.refreshToken
                if (refresh.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onSessionExpired() }
                    break
                }
                if (!TokenStore.isAccessValid()) {
                    when (val result = api.refreshTokens(refresh)) {
                        is AuthApiResult.Ok -> {
                            AuthSession.updateTokens(
                                newAccess = result.value.accessToken,
                                newRefresh = result.value.refreshToken ?: refresh,
                                expiresInSeconds = result.value.expiresInSeconds,
                            )
                        }
                        is AuthApiResult.Err -> {
                            // Server aniq rad etganda (400: refresh yaroqsiz/eskirgan,
                            // 401/403: bekor qilingan/bloklangan) — sessiya tugaydi.
                            // Tarmoq xati / 5xx / 429 da esa sessiya saqlanadi va
                            // keyingi poll'da qayta uriniladi (audit T-H3).
                            val definitive =
                                result.httpStatus == 400 ||
                                    result.httpStatus == 401 ||
                                    result.httpStatus == 403
                            if (definitive) {
                                AuthSession.clear()
                                withContext(Dispatchers.Main) { onSessionExpired() }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

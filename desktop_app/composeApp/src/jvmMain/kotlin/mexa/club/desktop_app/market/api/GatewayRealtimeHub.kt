package mexa.club.desktop_app.market.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mexa.club.desktop_app.market.session.MarketSessionGate

/**
 * Admin panel uchun yagona SSE ulanishi.
 */
object GatewayRealtimeHub {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var listenerJob: Job? = null

    private val _dashboardRefresh = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val dashboardRefresh: SharedFlow<Unit> = _dashboardRefresh.asSharedFlow()

    @Volatile
    private var lastDashboardRefreshEmitMs: Long = 0L

    private const val DASHBOARD_REFRESH_DEBOUNCE_MS = 3_000L

    fun start() {
        if (listenerJob?.isActive == true) return
        listenerJob = scope.launch {
            MarketSessionGate.awaitReady()
            while (isActive) {
                // Agar sessiya yopilgan bo'lsa (logout), qayta urinishsiz to'xtash
                if (!MarketSessionGate.ready.value) {
                    delay(1_000)
                    continue
                }
                try {
                    ApiClient.consumeServerSentEvents("/api/realtime/stream") { raw ->
                        val affects = realtimeMutationAffectsSuperAdminDashboard(raw)
                        if (affects) {
                            val now = System.currentTimeMillis()
                            if (now - lastDashboardRefreshEmitMs >= DASHBOARD_REFRESH_DEBOUNCE_MS) {
                                lastDashboardRefreshEmitMs = now
                                scope.launch { _dashboardRefresh.emit(Unit) }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Sessiya yopilgan bo'lsa darhol to'xtash, aks holda 4s kutib qayta urinish
                    if (!isActive || !MarketSessionGate.ready.value) break
                    delay(4_000)
                }
            }
        }
    }

    fun stop() {
        listenerJob?.cancel()
        listenerJob = null
    }
}

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

    private var trailingRefreshJob: Job? = null

    private const val DASHBOARD_REFRESH_DEBOUNCE_MS = 3_000L

    /**
     * Kamida [DASHBOARD_REFRESH_DEBOUNCE_MS] oral bilan emit qiladi (cheklovsiz
     * refresh bo'lmasligi uchun). Lekin oynaga tushgan oxirgi hodisa tashlab
     * yuborilmaydi: oyna yopilgandan so'ng yana bir marta emit bo'ladi (trailing),
     * shu bilan hech qaysi mutatsiya yo'qolmaydi.
     */
    private fun emitRefreshThrottled() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastDashboardRefreshEmitMs
        if (elapsed >= DASHBOARD_REFRESH_DEBOUNCE_MS) {
            lastDashboardRefreshEmitMs = now
            scope.launch { _dashboardRefresh.emit(Unit) }
        } else if (trailingRefreshJob == null) {
            trailingRefreshJob = scope.launch {
                delay(DASHBOARD_REFRESH_DEBOUNCE_MS - elapsed)
                lastDashboardRefreshEmitMs = System.currentTimeMillis()
                _dashboardRefresh.emit(Unit)
                trailingRefreshJob = null
            }
        }
    }

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
                        if (realtimeMutationAffectsSuperAdminDashboard(raw)) {
                            emitRefreshThrottled()
                        }
                    }
                    // Server kanalni yopdi (yoki 200 bo'sh javob) — zudlik bilan
                    // qayta ulanish hot-loop hosil qiladi (audit T-H2), qisqa pauza.
                    delay(1_000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Non-2xx/401/socket xatlari: sessiya yopilgan bo'lsa darhol
                    // to'xtash, aks holda 4s kutib qayta urinish
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

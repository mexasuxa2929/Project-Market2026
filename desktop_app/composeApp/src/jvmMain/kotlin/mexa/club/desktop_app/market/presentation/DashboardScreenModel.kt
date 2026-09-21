package mexa.club.desktop_app.market.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.market.data.DashboardRepository
import mexa.club.desktop_app.market.session.MarketSessionGate
import mexa.club.desktop_app.market.ui.components.RecentOrderTableRow

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val userCount: String = "—",
    val warehouseCount: String = "—",
    val productCount: String = "—",
    val todayOrders: String = "—",
    val orderRows: List<RecentOrderTableRow> = emptyList(),
    val totalOrders: String = "—",
    val orderPage: Int = 0,
    val orderPageSize: Int = 5,
) {
    companion object {
        val Initial = DashboardUiState()
    }
}

/** MVVM: Voyager ScreenModel + StateFlow + Repository. */
class DashboardScreenModel(
    private val repository: DashboardRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(DashboardUiState.Initial)
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    @Volatile private var lastSilentLoadMs: Long = 0L
    private var pollingJob: Job? = null

    private companion object {
        const val SILENT_LOAD_DEBOUNCE_MS = 3_000L
        /** Polling interval — SSE ishlamasa ham 10s da bir yangilanadi */
        const val POLLING_INTERVAL_MS     = 10_000L
    }

    fun load(silent: Boolean = false) {
        val p = _state.value.orderPage
        val s = _state.value.orderPageSize
        screenModelScope.launch {
            refreshMutex.lock()
            try {
                val prev = _state.value
                if (!silent) {
                    _state.update { it.copy(loading = true, error = null) }
                }
                val result = withContext(Dispatchers.IO) {
                    runCatching { repository.loadDashboard(p, s) }
                }
                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    _state.update {
                        it.copy(
                            loading = false,
                            error = when {
                                data.partialErrors.isEmpty() -> null
                                !silent -> data.partialErrors.joinToString("\n")
                                else -> prev.error
                            },
                            userCount = data.users,
                            warehouseCount = data.warehouses,
                            productCount = data.products,
                            todayOrders = data.todayOrders,
                            orderRows = data.orders,
                            totalOrders = data.totalOrders,
                            orderPage = p,
                            orderPageSize = s,
                        )
                    }
                    if (!silent) lastSilentLoadMs = 0L
                } else if (!silent) {
                    _state.update {
                        it.copy(
                            loading = false,
                            error = result.exceptionOrNull()?.message ?: "Ma'lumotlarni yuklab bo'lmadi",
                        )
                    }
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    fun startAfterSessionReady() {
        // Avvalgi polling loop'ni bekor qilamiz — ikkita parallel loop bo'lmasligi uchun
        pollingJob?.cancel()
        pollingJob = screenModelScope.launch {
            MarketSessionGate.awaitReady()
            load(silent = false)

            // Polling: har 10s da silent yangilanish — SSE ishlamasa ham ishlaydi
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                if (MarketSessionGate.ready.value) {
                    load(silent = true)
                }
            }
        }
    }

    fun onRealtimeRefreshTick() {
        screenModelScope.launch {
            MarketSessionGate.awaitReady()
            val now = System.currentTimeMillis()
            if (now - lastSilentLoadMs < SILENT_LOAD_DEBOUNCE_MS) return@launch
            lastSilentLoadMs = now
            load(silent = true)
        }
    }

    fun setOrderPage(page: Int) {
        if (page == _state.value.orderPage) return
        _state.update { it.copy(orderPage = page) }
        load(silent = false)
    }

    fun setOrderPageSize(size: Int) {
        if (size == _state.value.orderPageSize) return
        _state.update { it.copy(orderPageSize = size, orderPage = 0) }
        load(silent = false)
    }
}

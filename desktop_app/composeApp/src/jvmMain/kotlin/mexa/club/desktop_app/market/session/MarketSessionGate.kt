package mexa.club.desktop_app.market.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Gateway sessiyasi (token + bootstrap) tayyor bo‘lgach `true`.
 * Dashboard va SSE shu paytdan keyin ishga tushadi.
 */
object MarketSessionGate {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun setReady(value: Boolean) {
        _ready.value = value
    }

    suspend fun awaitReady() {
        if (_ready.value) return
        ready.filter { it }.first()
    }
}

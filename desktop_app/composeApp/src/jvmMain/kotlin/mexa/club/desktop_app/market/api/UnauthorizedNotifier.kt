package mexa.club.desktop_app.market.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UnauthorizedNotifier {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    @Volatile
    private var lastEmitMs: Long = 0L
    private const val DEBOUNCE_MS = 5_000L

    fun notifyUnauthorized() {
        val now = System.currentTimeMillis()
        if (now - lastEmitMs < DEBOUNCE_MS) return
        lastEmitMs = now
        _events.tryEmit("Sessiya muddati tugagan. Qaytadan kiring.")
    }

    fun clearDebounce() {
        lastEmitMs = 0L
    }
}

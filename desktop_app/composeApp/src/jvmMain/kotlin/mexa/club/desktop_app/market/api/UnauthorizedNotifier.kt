package mexa.club.desktop_app.market.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UnauthorizedNotifier {

    // S-50: replay=1 — collector keyin ulansa ham oxirgi xabarni ko'radi
    private val _events = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
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

}

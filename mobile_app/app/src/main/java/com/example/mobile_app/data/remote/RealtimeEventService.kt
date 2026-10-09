package com.example.mobile_app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Backend'dan real-time SSE (Server-Sent Events) eventlarini tinglaydi.
 *
 * Xatoxlik (audit M-H1):
 * - Har bir qayta ulanishda response/body yopiladi (try/finally) — socket leak yo'q
 * - stop() joriy Call'ni bekor qiladi — bloklangan read ochiladi
 * - Backoff: 2s dan boshlanib 30s gacha ko'tariladi, muvaffaqiyatli ulanishda tiklanadi
 * - Token yo'q bo'lsa (logout) socket umuman ochilmaydi
 */
class RealtimeEventService(
    private val baseUrl: String,
    private val onEvent: (eventType: String, data: String) -> Unit
) {
    private var client: OkHttpClient? = null
    private var thread: Thread? = null
    private var activeCall: okhttp3.Call? = null

    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true

        if (client == null) {
            // Past darajadagi qurilmalar uchun kichikroq OkHttp client (bir marta yaratiladi)
            client = OkHttpClient.Builder()
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
        }

        thread = Thread {
            var backoffMs = INITIAL_BACKOFF_MS
            while (running) {
                // Har qayta ulanishda joriy token olinadi (u refresh bo'lishi mumkin).
                // Gateway /api/realtime/** ni JWT bilan himoyalaydi.
                val token = com.example.mobile_app.data.local.TokenManager.accessToken
                if (token.isNullOrBlank()) {
                    // Token yo'q (logout) — socket ochilmaydi, 5s da bir tekshiramiz
                    sleepQuietly(IDLE_POLL_MS)
                    continue
                }
                val request = Request.Builder()
                    .url("${baseUrl}api/realtime/stream")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer $token")
                    .build()

                var response: Response? = null
                try {
                    val call = client!!.newCall(request)
                    activeCall = call
                    response = call.execute()

                    if (response.code == 401 || response.code == 403) {
                        // Auth xato — sekin qayta urinish (token refresh keyingi urinishda)
                        sleepQuietly(AUTH_RETRY_MS)
                        continue
                    }
                    if (!response.isSuccessful) {
                        // 5xx/429 va hokazo — exponential backoff
                        sleepQuietly(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                        continue
                    }

                    // Muvaffaqiyatli ulanish — backoff ni tiklaymiz
                    backoffMs = INITIAL_BACKOFF_MS

                    val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
                    var eventType = ""
                    var data = ""
                    while (running) {
                        val line = reader.readLine() ?: break

                        when {
                            line.startsWith("event:") -> eventType = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> {
                                data = line.removePrefix("data:").trim()
                                if (eventType.isNotBlank() && data.isNotBlank()) {
                                    onEvent(eventType, data)
                                }
                                eventType = ""
                                data = ""
                            }
                            line.isBlank() -> {
                                eventType = ""
                                data = ""
                            }
                        }
                    }
                    // Server kanalni yopdi — zudlik bilan qayta ulanishdan oldin qisqa pauza
                    sleepQuietly(CONNECT_GAP_MS)
                } catch (e: Exception) {
                    if (running) {
                        sleepQuietly(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                    }
                } finally {
                    activeCall = null
                    try {
                        response?.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        // Bloklangan SSE read'ini ochish uchun joriy call'ni bekor qilamiz
        try {
            activeCall?.cancel()
        } catch (_: Exception) {
        }
        thread?.interrupt()
        thread = null
    }

    private fun sleepQuietly(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
            // stop() tomonidan interrupt — tsikl running=false ni ko'rib chiqadi
        }
    }

    private companion object {
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 30_000L
        const val AUTH_RETRY_MS = 10_000L
        const val IDLE_POLL_MS = 5_000L
        const val CONNECT_GAP_MS = 1_000L
    }
}

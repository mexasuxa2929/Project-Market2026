package com.example.mobile_app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Backend'dan real-time SSE (Server-Sent Events) eventlarini tinglaydi.
 *
 * Masalan: admin desktop app'dan mahsulot o'chirilganda, mobil app darhol biladi.
 * Past darajadagi qurilmalar uchun optimizatsiya qilingan.
 */
class RealtimeEventService(
    private val baseUrl: String,
    private val onEvent: (eventType: String, data: String) -> Unit
) {
    private var client: OkHttpClient? = null
    private var thread: Thread? = null
    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true

        // Past darajadagi qurilmalar uchun kichikroq OkHttp client
        client = OkHttpClient.Builder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        thread = Thread {
            val request = Request.Builder()
                .url("${baseUrl}api/realtime/stream")
                .header("Accept", "text/event-stream")
                .build()

            while (running) {
                try {
                    val response = client!!.newCall(request).execute()
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
                } catch (e: Exception) {
                    if (running) {
                        // Xatolikdan keyin 10 soniya kutish (eski: 5s)
                        // Past darajadagi qurilmalarda qayta ulanishni sekinlashtirish
                        Thread.sleep(10_000)
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
        thread?.interrupt()
        thread = null
    }
}

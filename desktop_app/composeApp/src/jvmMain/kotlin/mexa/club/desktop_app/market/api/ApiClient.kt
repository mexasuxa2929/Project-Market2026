package mexa.club.desktop_app.market.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.coroutineContext
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.settings.AppPreferences

object ApiClient {

    /** @deprecated Use [resolveBaseUrl]; kept for debug logs. */
    val BASE_URL: String
        get() = resolveBaseUrl()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * Windows + CIO: parallel localhost so'rovlarda "Address already in use" chiqadi.
     * JVM [Java] engine va ketma-ket GET buni bartaraf etadi.
     */
    private val httpGate = Mutex()

    val client: HttpClient = HttpClient(Java) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(jsonParser)
        }
    }

    private val sseClient: HttpClient = HttpClient(Java) {
        install(HttpTimeout) {
            requestTimeoutMillis = Long.MAX_VALUE
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = Long.MAX_VALUE
        }
    }

    fun resolveBaseUrl(): String {
        val raw = AppPreferences.baseUrl.trimEnd('/')
        return if (raw.contains("://localhost")) {
            raw.replace("://localhost", "://127.0.0.1")
        } else {
            raw
        }
    }

    fun fullPath(path: String): String =
        if (path.startsWith("http")) path
        else "${resolveBaseUrl()}${if (path.startsWith("/")) path else "/$path"}"

    suspend fun get(path: String): String = getWithRetry(path, attempts = 1)

    suspend fun getBytes(path: String): ByteArray = httpGate.withLock {
        val response: HttpResponse = client.get(fullPath(path)) { authHeader() }
        if (response.status.value !in 200..299) throw GatewayApiException(response.status.value, "HTTP ${response.status.value}")
        response.body<ByteArray>()
    }

    /** JSON body bilan PUT so'rov. IO dispatcher'da chaqiring. */
    suspend fun put(path: String, jsonBody: String): String = httpGate.withLock {
        val response: HttpResponse = client.put(fullPath(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }

    /** PATCH so'rov — mutex kutmasdan (qisqa holat yangilash uchun). IO dispatcher'da chaqiring. */
    suspend fun patch(path: String, jsonBody: String): String {
        val response: HttpResponse = client.patch(fullPath(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        return text
    }

    /** DELETE so'rov. IO dispatcher'da chaqiring. */
    suspend fun delete(path: String): String = httpGate.withLock {
        val response: HttpResponse = client.delete(fullPath(path)) {
            authHeader()
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }

    /** Multipart PUT so'rov (product update with images). IO dispatcher'da chaqiring. */
    suspend fun putMultipart(path: String, jsonPart: String, files: List<Pair<String, ByteArray>>): String = httpGate.withLock {
        val response: HttpResponse = client.put(fullPath(path)) {
            authHeader()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("product", jsonPart, io.ktor.http.Headers.build { append(HttpHeaders.ContentType, "application/json") })
                        files.forEach { (name, bytes) ->
                            val mime = when {
                                name.endsWith(".png",  ignoreCase = true) -> "image/png"
                                name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                                else -> "image/jpeg"
                            }
                            append("files", bytes, io.ktor.http.Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                                append(HttpHeaders.ContentType, mime)
                            })
                        }
                    },
                ),
            )
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }


    /**
     * Multipart/form-data POST so'rov.
     * [jsonPart] — "product" nomli JSON part (application/json).
     * [files]    — (fileName, ByteArray) juftlari.
     */
    suspend fun postMultipart(
        path: String,
        jsonPart: String,
        files: List<Pair<String, ByteArray>>,
    ): String = httpGate.withLock {
        val response: HttpResponse = client.post(fullPath(path)) {
            authHeader()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "product", jsonPart,
                            io.ktor.http.Headers.build {
                                append(HttpHeaders.ContentType, "application/json")
                            },
                        )
                        files.forEach { (name, bytes) ->
                            val mime = when {
                                name.endsWith(".png",  ignoreCase = true) -> "image/png"
                                name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                                else -> "image/jpeg"
                            }
                            append(
                                "files", bytes,
                                io.ktor.http.Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                                    append(HttpHeaders.ContentType, mime)
                                },
                            )
                        }
                    },
                ),
            )
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }

    /** JSON body bilan POST so'rov. IO dispatcher'da chaqiring. */
    suspend fun post(path: String, jsonBody: String): String = httpGate.withLock {
        val response: HttpResponse = client.post(fullPath(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }

    /** Bitta faylni multipart "file" parametr sifatida yuborish. IO dispatcher'da chaqiring. */
    suspend fun uploadFile(path: String, fileName: String, bytes: ByteArray): String = httpGate.withLock {
        val mime = when {
            fileName.endsWith(".png",  ignoreCase = true) -> "image/png"
            fileName.endsWith(".svg",  ignoreCase = true) -> "image/svg+xml"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        }
        val response: HttpResponse = client.post(fullPath(path)) {
            authHeader()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file", bytes,
                            io.ktor.http.Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                append(HttpHeaders.ContentType, mime)
                            },
                        )
                    },
                ),
            )
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }

    private suspend fun getWithRetry(path: String, attempts: Int): String {
        var last: Throwable? = null
        repeat(attempts) { attempt ->
            try {
                return executeGet(path)
            } catch (e: Throwable) {
                last = e
                if (attempt < attempts - 1 && isRetryable(e)) {
                    delay(300L * (attempt + 1))
                }
            }
        }
        throw last ?: GatewayApiException(0, "So‘rov bajarilmadi: $path")
    }

    private suspend fun executeGet(path: String): String = httpGate.withLock {
        val response: HttpResponse = client.get(fullPath(path)) {
            authHeader()
        }
        val text = response.bodyAsText()
        if (response.status.value == 401) {
            SessionManager.clearGatewaySession()
            UnauthorizedNotifier.notifyUnauthorized()
            throw GatewayApiException(401, "Avtorizatsiya muddati tugagan")
        }
        if (response.status.value !in 200..299) {
            val brief = text.take(200).ifBlank { "HTTP ${response.status.value}" }
            throw GatewayApiException(response.status.value, brief)
        }
        text
    }

    private fun isRetryable(e: Throwable): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("connection reset")
            || msg.contains("timeout")
            || msg.contains("broken pipe")
    }

    suspend fun consumeServerSentEvents(path: String, onData: suspend (String) -> Unit) {
        sseClient.prepareGet(fullPath(path)) {
            authHeader()
            header(HttpHeaders.Accept, "text/event-stream")
            header(HttpHeaders.CacheControl, "no-cache")
        }.execute { response ->
            if (response.status.value == 401) {
                // SSE uchun 401 sessiyani buzmasligi kerak — dashboard allaqachon
                // muvaffaqiyatli yuklangan (screenshotda ko'rinib turibdi), demak
                // token yaroqli. SSE alohida ruxsat talab qilishi mumkin, shuning
                // uchun faqat log va qayta urinish, toast emas.
                return@execute
            }
            if (response.status.value !in 200..299) return@execute
            val channel = response.bodyAsChannel()
            val dataBuffer = StringBuilder()
            while (coroutineContext.isActive) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.isBlank() -> {
                        if (dataBuffer.isNotEmpty()) {
                            val payload = dataBuffer.toString()
                            dataBuffer.clear()
                            onData(payload)
                        }
                    }
                    line.startsWith("data:") -> {
                        val rest = line.removePrefix("data:").trimStart()
                        if (dataBuffer.isNotEmpty()) dataBuffer.append('\n')
                        dataBuffer.append(rest)
                    }
                    line.startsWith(":") -> Unit
                }
            }
        }
    }

    private fun HttpRequestBuilder.authHeader() {
        val t = SessionManager.token.takeIf { it.isNotEmpty() }
            ?: AuthSession.accessToken?.takeIf { it.isNotBlank() }
        if (!t.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $t")
        }
    }

    fun parseJsonObject(text: String): JsonObject? =
        runCatching { jsonParser.parseToJsonElement(text) as? JsonObject }.getOrNull()

    fun dataObject(envelope: JsonObject): JsonObject? {
        val data = envelope["data"] ?: return null
        return data as? JsonObject
    }

    /**
     * Envelope ("data" kaliti) yoki to'g'ridan-to'g'ri sahifa body'sini qaytaradi.
     * product-service `{"success":true,"data":{...}}` shaklida javob qaytaradi,
     * shop-service `/api/products` esa `{"content":[...],"totalElements":...}` kabi
     * yalang'och PagePayload qaytaradi — ikkala shakl ham qo'llab-quvvatlanadi.
     */
    fun dataObjectOrSelf(envelope: JsonObject): JsonObject? {
        val data = envelope["data"]
        if (data != null) return data as? JsonObject
        if (envelope["content"] is JsonArray || envelope["items"] is JsonArray) return envelope
        return null
    }

    fun json(): Json = jsonParser
}

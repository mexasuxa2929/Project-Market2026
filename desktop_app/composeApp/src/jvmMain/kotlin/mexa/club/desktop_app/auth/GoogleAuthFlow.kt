package mexa.club.desktop_app.auth

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.BindException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Desktop uchun Google OAuth — **Authorization Code + PKCE (S256)** flow.
 *
 * - Implicit flow (`response_type=id_token`) yo'q qilingan: token URL fragmentda
 *   kelmasligi uchun (S-49).
 * - Kod loopback `127.0.0.1` serverga qaytadi; `/submit` kodni token endpointga
 *   PKCE `code_verifier` bilan almashadi va id_token oladi (backendga xuddi
 *   shu id_token uzatiladi — server tomoni o'zgarmaydi).
 * - Serverga kirish uchun `GOOGLE_DESKTOP_CLIENT_SECRET` (Google Cloud Console →
 *   Web client → Client secret) sozlangan bo'lishi shart.
 *
 * Google Cloud Console'da Web client uchun redirect URI sifatida
 * [REDIRECT_URI] ro'yxatdan o'tgan bo'lishi shart.
 */
object GoogleAuthFlow {

    private const val DEFAULT_CLIENT_ID =
        "758985644197-6cg7sel08sg5mjubod3pdhsvt6q07h2s.apps.googleusercontent.com"
    private const val PORT = 45123

    const val REDIRECT_URI = "http://127.0.0.1:$PORT/callback"

    private const val TIMEOUT_SECONDS = 180L

    private val clientId: String =
        System.getenv("GOOGLE_DESKTOP_CLIENT_ID")?.takeIf { it.isNotBlank() } ?: DEFAULT_CLIENT_ID

    /** S-49: PKCE kod almashinuvi uchun client secret (faqat desktop muhitda). */
    private val clientSecret: String? =
        System.getenv("GOOGLE_DESKTOP_CLIENT_SECRET")?.takeIf { it.isNotBlank() }
    sealed class Result {
        data class Success(val idToken: String) : Result()
        data class Failure(val reason: Reason) : Result()
    }

    enum class Reason { PORT_BUSY, BROWSER_FAILED, TIMEOUT, ACCESS_DENIED, STATE_MISMATCH, MALFORMED, NO_CLIENT_SECRET }

    suspend fun signIn(): Result = withContext(Dispatchers.IO) {
        // S-49: PKCE uchun client secret majburiy — yo'qsa flow boshlanmaydi.
        if (clientSecret == null) return@withContext Result.Failure(Reason.NO_CLIENT_SECRET)
        val state = randomHex(16)
        val codeVerifier = randomHex(32)
        val codeChallenge = sha256Base64Url(codeVerifier)

        val server = try {
            HttpServer.create(InetSocketAddress("127.0.0.1", PORT), 0)
        } catch (e: BindException) {
            return@withContext Result.Failure(Reason.PORT_BUSY)
        } catch (e: Exception) {
            return@withContext Result.Failure(Reason.MALFORMED)
        }

        val future = CompletableFuture<String>()

        server.createContext("/callback") { exchange ->
            val bytes = CALLBACK_HTML.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }

        server.createContext("/submit") { exchange ->
            val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val params = parseForm(body)
            // Avval brauzerga javob yozamiz, keyin futureni yakunlaymiz
            // (aks holda ilova serverni yopib, fetch javobi uzilishi mumkin).
            try {
                val ok = "OK".toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, ok.size.toLong())
                exchange.responseBody.use { it.write(ok); it.flush() }
            } catch (_: Exception) {
            }
            val error = params["error"]
            when {
                error != null -> future.completeExceptionally(GoogleFlowException(error))
                params["state"] != state -> future.completeExceptionally(StateMismatchException())
                // S-49: Authorization Code + PKCE — kodni token endpointga almashamiz
                !params["code"].isNullOrBlank() -> {
                    try {
                        val idToken = exchangeCodeForIdToken(params["code"].orEmpty(), codeVerifier)
                        if (idToken.isBlank()) {
                            future.completeExceptionally(GoogleFlowException("empty id_token"))
                        } else {
                            future.complete(idToken)
                        }
                    } catch (e: Exception) {
                        future.completeExceptionally(GoogleFlowException(e.message ?: "token exchange failed"))
                    }
                }
                else -> future.completeExceptionally(GoogleFlowException("missing code"))
            }
        }

        server.start()
        try {
            if (!openBrowser(buildAuthorizeUrl(state, codeChallenge))) {
                return@withContext Result.Failure(Reason.BROWSER_FAILED)
            }
            val idToken = try {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                return@withContext Result.Failure(Reason.TIMEOUT)
            } catch (e: java.util.concurrent.ExecutionException) {
                val cause = e.cause
                return@withContext when (cause) {
                    is StateMismatchException -> Result.Failure(Reason.STATE_MISMATCH)
                    is GoogleFlowException ->
                        if (cause.message == "access_denied") Result.Failure(Reason.ACCESS_DENIED)
                        else Result.Failure(Reason.MALFORMED)
                    else -> Result.Failure(Reason.MALFORMED)
                }
            }
            if (idToken.isBlank()) Result.Failure(Reason.MALFORMED) else Result.Success(idToken)
        } finally {
            // Brauzerga javob to'liq yetib olishi uchun biroz kutasiz, keyin serverni yopamiz.
            try {
                Thread.sleep(500)
            } catch (_: InterruptedException) {
            }
            server.stop(2)
        }
    }

    private fun buildAuthorizeUrl(state: String, codeChallenge: String): String =
        "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=${enc(clientId)}" +
            "&redirect_uri=${enc(REDIRECT_URI)}" +
            "&response_type=code" +
            "&scope=${enc("openid email profile")}" +
            "&prompt=select_account" +
            "&state=${enc(state)}" +
            "&code_challenge=${enc(codeChallenge)}" +
            "&code_challenge_method=S256"

    private fun openBrowser(url: String): Boolean {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return true
            }
        } catch (_: Throwable) {
        }
        return try {
            ProcessBuilder("xdg-open", url).start() != null
        } catch (_: Exception) {
            false
        }
    }

    private fun parseForm(body: String): Map<String, String> =
        body.split('&')
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) {
                    pair to ""
                } else {
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8") to
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                }
            }

    private fun enc(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    /** S-49: authorization kodni token endpointga PKCE bilan almashadi → id_token. */
    private fun exchangeCodeForIdToken(code: String, codeVerifier: String): String {
        val body = buildString {
            append("code=").append(enc(code))
            append("&client_id=").append(enc(clientId))
            append("&client_secret=").append(enc(clientSecret ?: ""))
            append("&redirect_uri=").append(enc(REDIRECT_URI))
            append("&grant_type=authorization_code")
            append("&code_verifier=").append(enc(codeVerifier))
        }
        val conn = java.net.URL("https://oauth2.googleapis.com/token").openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.readBytes()?.toString(Charsets.UTF_8) ?: ""
            if (status !in 200..299) throw GoogleFlowException("token endpoint HTTP $status")
            val json = kotlinx.serialization.json.Json.parseToJsonElement(text)
            val idToken = (json as? kotlinx.serialization.json.JsonObject)
                ?.get("id_token")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            return idToken.orEmpty()
        } finally {
            conn.disconnect()
        }
    }

    /** S-49: PKCE S256 — base64url(SHA-256(verifier)). */
    private fun sha256Base64Url(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun randomHex(bytes: Int): String {
        val buf = ByteArray(bytes)
        SecureRandom().nextBytes(buf)
        return buf.joinToString("") { "%02x".format(it) }
    }

    private class GoogleFlowException(message: String) : Exception(message)
    private class StateMismatchException : Exception("state mismatch")

    private val CALLBACK_HTML = """
        <!doctype html>
        <html>
        <head><meta charset="utf-8"><title>Google</title></head>
        <body style="font-family: sans-serif; text-align: center; padding-top: 80px; background: #f8fafc;">
            <p id="msg" style="font-size: 16px; color: #0f172a;">Google hisobingizni tasdiqlang…</p>
            <script>
                (function () {
                    var hash = window.location.hash ? window.location.hash.substring(1) : "";
                    var query = window.location.search ? window.location.search.substring(1) : "";
                    var payload = hash || query;
                    if (payload) {
                        window.history.replaceState(null, '', window.location.pathname);
                    }
                    if (!payload) {
                        document.getElementById('msg').textContent = 'Xato: javob parametrlari yo\'q';
                        return;
                    }
                    fetch('/submit', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: payload
                    }).then(function (r) {
                        document.getElementById('msg').textContent = 'Tayyor! Bu oynani yopishingiz mumkin.';
                    }).catch(function (e) {
                        document.getElementById('msg').textContent = 'Yuborishda xato: ' + e;
                    });
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

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
 * Desktop uchun Google OAuth (implicit flow: tizim brauzerida ochiladi,
 * id_token loopback server orqali ilovaga qaytadi).
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

    sealed class Result {
        data class Success(val idToken: String) : Result()
        data class Failure(val reason: Reason) : Result()
    }

    enum class Reason { PORT_BUSY, BROWSER_FAILED, TIMEOUT, ACCESS_DENIED, STATE_MISMATCH, MALFORMED }

    suspend fun signIn(): Result = withContext(Dispatchers.IO) {
        val state = randomHex(16)
        val nonce = randomHex(16)

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
                !params["id_token"].isNullOrBlank() -> future.complete(params["id_token"].orEmpty())
                else -> future.completeExceptionally(GoogleFlowException("missing id_token"))
            }
        }

        server.start()
        try {
            if (!openBrowser(buildAuthorizeUrl(state, nonce))) {
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

    private fun buildAuthorizeUrl(state: String, nonce: String): String =
        "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=${enc(clientId)}" +
            "&redirect_uri=${enc(REDIRECT_URI)}" +
            "&response_type=id_token" +
            "&scope=${enc("openid email profile")}" +
            "&prompt=select_account" +
            "&state=${enc(state)}" +
            "&nonce=${enc(nonce)}"

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

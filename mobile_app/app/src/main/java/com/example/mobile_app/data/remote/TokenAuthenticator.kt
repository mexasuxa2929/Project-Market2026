package com.example.mobile_app.data.remote

import com.example.mobile_app.data.local.SessionEvents
import com.example.mobile_app.data.local.TokenManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OkHttp Authenticator — server 401 yoki 403 qaytarganda avtomatik token yangilaydi.
 * Refresh muvaffaqiyatli bo'lsa, asl so'rov yangi token bilan qayta yuboriladi.
 * Refresh ham muvaffaqiyatsiz bo'lsa, sessiya tugagan deb e'lon qilinadi
 * (login ekraniga qaytish SessionEvents orqali amalga oshiriladi).
 */
class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedPath = response.request.url.encodedPath

        // Refresh endpoint o'zi 401/403 qaytarsa — cheksiz loop oldini olamiz
        if (failedPath.contains("/api/auth/refresh")) {
            TokenManager.clear()
            SessionEvents.expire()
            return null
        }

        // 1 martadan ko'p urinmaymiz
        if (response.retryCount > 1) return null

        val code = response.code
        if (code != 401 && code != 403) return null

        // Hisob bloklangan bo'lsa — refreshga urinmasdan darhol logout qilamiz.
        // Refresh muvaffaqiyatsiz bo'lsa ham kechikmaslik uchun.
        try {
            val body = response.peekBody(1024).string()
            if (body.contains("ACCOUNT_DISABLED")) {
                TokenManager.clear()
                SessionEvents.expire()
                return null
            }
        } catch (_: Exception) {}

        val refreshToken = TokenManager.refreshToken ?: run {
            TokenManager.clear()
            SessionEvents.expire()
            return null
        }

        // Thread-safe: bir vaqtda faqat bitta refresh so'rovi
        return synchronized(TokenAuthenticator::class.java) {
            // Boshqa thread allaqachon yangilab qo'ygan bo'lishi mumkin
            val latestToken = TokenManager.accessToken
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (latestToken != null && latestToken != requestToken) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val tokenPair = tryRefresh(refreshToken)
            if (tokenPair != null) {
                TokenManager.accessToken = tokenPair.first
                TokenManager.refreshToken = tokenPair.second
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenPair.first}")
                    .build()
            } else {
                TokenManager.clear()
                SessionEvents.expire()
                null
            }
        }
    }

    /**
     * OkHttp synchronous call orqali /api/auth/refresh endpointiga murojaat qiladi.
     * Retrofit ishlatilmaydi (suspend funksiya kerak emas).
     */
    private fun tryRefresh(refreshToken: String): Pair<String, String>? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val json = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString()

            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${RetrofitClient.BASE_URL}api/auth/refresh")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return null
                val jsonObj = JSONObject(responseBody)
                if (jsonObj.optBoolean("success", false)) {
                    val data = jsonObj.optJSONObject("data") ?: return null
                    val newAccess = data.optString("token").takeIf { it.isNotBlank() }
                    val newRefresh = data.optString("refreshToken").takeIf { it.isNotBlank() }
                    if (newAccess != null && newRefresh != null) {
                        Pair(newAccess, newRefresh)
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/** So'rov necha marta retry qilinganini hisoblash */
val Response.retryCount: Int
    get() {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
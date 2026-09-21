package com.example.mobile_app.data.remote

import com.example.mobile_app.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Har bir so'rovga Authorization: Bearer <token> qo'shadi.
 * Token muddati tugagan bo'lsa — 401 kelishini kutmasdan,
 * TokenAuthenticator refresh qiladi (reactive).
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.accessToken
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

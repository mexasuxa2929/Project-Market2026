package com.example.mobile_app.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.mobile_app.MexaMarketApp

object TokenManager {
    private const val PREF_NAME = "mexa_secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"  // Unix ms

    private val prefs
        get() = MexaMarketApp.instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_ACCESS_TOKEN, value) }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_REFRESH_TOKEN, value) }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_EMAIL, value) }

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(value) = prefs.edit { putString(KEY_FULL_NAME, value) }

    /** Token muddati tugash vaqti (Unix millisecond) */
    var tokenExpiresAt: Long
        get() = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_TOKEN_EXPIRES_AT, value) }

    val isLoggedIn: Boolean
        get() = accessToken != null

    /** Token muddati tugaganmi (30 soniya buffer bilan) */
    val isTokenExpired: Boolean
        get() = tokenExpiresAt > 0 && System.currentTimeMillis() > (tokenExpiresAt - 30_000)

    /**
     * JWT payload'idagi userId claim'ini o'qiydi.
     * Real-time eventlarda (ORDER_CREATED, NOTIFICATION_CREATED) o'z
     * useriga tegishli eventlarni filtrlash uchun ishlatiladi.
     */
    val userId: String?
        get() = accessToken?.let { token ->
            try {
                val parts = token.split(".")
                if (parts.size < 2) return@let null
                val payload = parts[1]
                val normalized = payload.replace('-', '+').replace('_', '/')
                val padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=')
                val json = String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT), Charsets.UTF_8)
                org.json.JSONObject(json).optString("userId").ifBlank { null }
            } catch (e: Exception) {
                null
            }
        }

    fun clear() = prefs.edit { clear() }
}

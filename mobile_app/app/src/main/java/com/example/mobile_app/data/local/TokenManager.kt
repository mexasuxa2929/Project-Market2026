package com.example.mobile_app.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.mobile_app.MexaMarketApp

object TokenManager {
    private const val PREF_NAME = "mexa_secure_prefs_enc"
    private const val LEGACY_PREF_NAME = "mexa_secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"  // Unix ms
    private const val KEY_DEVICE_ID = "device_id"

    /** Logout ham o'chirmasligi kerak: qurilma identifikatori o'rnatilishga tegishli. */
    private val devicePrefs
        get() = MexaMarketApp.instance.getSharedPreferences("mexa_device_prefs", Context.MODE_PRIVATE)

    /**
     * M-55: tokenlar shifrlangan (AES256_GCM) SharedPreferences'da saqlanadi.
     * Eski oddiy (plaintext) prefs'dan bir marta ko'chiriladi; fallback — oddiy prefs.
     */
    private val prefs: android.content.SharedPreferences by lazy {
        val app = MexaMarketApp.instance
        runCatching {
            val masterKey = androidx.security.crypto.MasterKey.Builder(app)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encrypted = androidx.security.crypto.EncryptedSharedPreferences.create(
                app,
                PREF_NAME,
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            // Migratsiya: eski plaintext fayldagi qiymatlarni ko'chirish
            if (encrypted.getString(KEY_ACCESS_TOKEN, null) == null) {
                val legacy = app.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
                if (legacy.all.isNotEmpty()) {
                    encrypted.edit {
                        legacy.all.forEach { (k, v) ->
                            when (v) {
                                is String -> putString(k, v)
                                is Long -> putLong(k, v)
                                is Int -> putInt(k, v)
                                is Boolean -> putBoolean(k, v)
                                is Float -> putFloat(k, v)
                                is Set<*> -> putStringSet(k, v.filterIsInstance<String>().toSet())
                            }
                        }
                    }
                    legacy.edit { clear() }
                }
            }
            encrypted
        }.getOrElse {
            app.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
        }
    }

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

    /**
     * Ushbu qurilma uchun barqaror identifikator (bir marta yaratiladi, saqlanadi).
     * Login/refresh so'rovlariga yuboriladi: backend shu deviceId bilan faol sessiya topsa,
     * token qayta yaratilganda (recreate) yangi qurilma sifatida hisoblamaydi.
     */
    val deviceId: String
        get() = devicePrefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString().also { devicePrefs.edit { putString(KEY_DEVICE_ID, it) } }

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

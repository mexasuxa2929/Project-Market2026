package mexa.club.desktop_app.auth

import java.util.prefs.Preferences

object TokenStore {
    private val prefs: Preferences =
        Preferences.userNodeForPackage(TokenStore::class.java)

    private const val KEY_ACCESS = "auth_access_token"
    private const val KEY_REFRESH = "auth_refresh_token"
    private const val KEY_EXPIRES = "auth_expires_at_ms"

    /** Tugashidan shuncha oldin access "eskirgan" hisoblanadi va yangilanadi. */
    private const val ACCESS_REFRESH_SKEW_MS = 5 * 60_000L

    fun save(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        prefs.put(KEY_ACCESS, accessToken)
        prefs.put(KEY_REFRESH, refreshToken)
        prefs.putLong(KEY_EXPIRES, System.currentTimeMillis() + expiresInSeconds * 1000L)
        prefs.flush()
    }

    fun loadAccess(): String? = prefs.get(KEY_ACCESS, null)
    fun loadRefresh(): String? = prefs.get(KEY_REFRESH, null)
    fun expiresAtMs(): Long = prefs.getLong(KEY_EXPIRES, 0L)

    /**
     * `true` — access token hali ishlatish mumkin (tugashidan oldin zaxira oynasi).
     * `false` bo‘lsa [TokenRefreshManager] va [AuthApiClient.ensureFreshAccessToken] refresh chaqiradi.
     */
    fun isAccessValid(): Boolean {
        val access = loadAccess() ?: return false
        if (access.isBlank()) return false
        val exp = expiresAtMs()
        if (exp <= 0L) return false
        return System.currentTimeMillis() < exp - ACCESS_REFRESH_SKEW_MS
    }

    fun clear() {
        prefs.remove(KEY_ACCESS)
        prefs.remove(KEY_REFRESH)
        prefs.remove(KEY_EXPIRES)
        prefs.flush()
    }
}

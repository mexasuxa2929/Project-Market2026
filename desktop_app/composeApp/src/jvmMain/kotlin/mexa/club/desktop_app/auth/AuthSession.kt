package mexa.club.desktop_app.auth

import java.util.UUID

object AuthSession {

    @Volatile var accessToken: String? = null
        private set

    @Volatile var refreshToken: String? = null
        private set

    @Volatile var sessionId: UUID? = null
        private set

    @Volatile var roles: List<String> = emptyList()
        private set

    @Volatile var username: String? = null
        private set

    fun restoreFromDisk() {
        val access = TokenStore.loadAccess() ?: return
        val refresh = TokenStore.loadRefresh() ?: return
        accessToken = access
        refreshToken = refresh
        parseTokenPayload(access)
    }

    fun setFromLogin(
        token: String,
        refresh: String?,
        session: String?,
        expiresInSeconds: Long = 900L,
        rememberMe: Boolean = true,
    ) {
        accessToken = token
        refreshToken = refresh
        sessionId = session?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        parseTokenPayload(token)
        if (rememberMe) {
            TokenStore.save(token, refresh ?: "", expiresInSeconds)
        } else {
            TokenStore.clear()
        }
    }

    fun updateTokens(newAccess: String, newRefresh: String, expiresInSeconds: Long) {
        accessToken = newAccess
        refreshToken = newRefresh
        parseTokenPayload(newAccess)
        TokenStore.save(newAccess, newRefresh, expiresInSeconds)
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        sessionId = null
        roles = emptyList()
        username = null
        TokenStore.clear()
    }

    val isLoggedIn: Boolean
        get() = accessToken != null

    val hasAdminAccess: Boolean
        get() = roles.any { it == "ROLE_SUPER_ADMIN" || it == "ROLE_ADMIN" }

    val isSuperAdmin: Boolean
        get() = roles.contains("ROLE_SUPER_ADMIN")

    private fun parseTokenPayload(token: String) {
        try {
            val parts = token.split(".")
            if (parts.size < 2) return
            val payload = parts[1]
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val json = String(java.util.Base64.getUrlDecoder().decode(padded))
            val rolesMatch = Regex(""""roles"\s*:\s*\[([^\]]*)]""").find(json)
            roles = rolesMatch?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim().trim('"') }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            val usernameMatch = Regex(""""(?:sub|username)"\s*:\s*"([^"]+)"""").find(json)
            username = usernameMatch?.groupValues?.get(1)
        } catch (_: Exception) {
        }
    }
}

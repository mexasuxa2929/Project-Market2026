package mexa.club.desktop_app.market.session

import mexa.club.desktop_app.market.api.ApiClient

object SessionManager {

    @Volatile
    var token: String = ""
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    @Volatile
    var currentUser: UserDto? = null
        private set

    fun setSession(accessToken: String, refresh: String?, user: UserDto?) {
        token = accessToken
        refreshToken = refresh
        currentUser = user
    }

    fun clearGatewaySession() {
        token = ""
        refreshToken = null
        currentUser = null
    }
}

suspend fun bootstrapSessionFromGateway(accessToken: String, refresh: String?) {
    SessionManager.setSession(accessToken, refresh, null)
    runCatching {
        val text = ApiClient.get("/api/admin/users/me")
        val env = ApiClient.parseJsonObject(text) ?: return@runCatching
        val data = ApiClient.dataObject(env) ?: return@runCatching
        val user = ApiClient.json().decodeFromJsonElement(UserDto.serializer(), data)
        SessionManager.setSession(accessToken, refresh, user)
    }
}

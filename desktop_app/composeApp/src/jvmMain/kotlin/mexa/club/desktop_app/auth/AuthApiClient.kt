package mexa.club.desktop_app.auth

import mexa.club.desktop_app.localization.AuthNetworkStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

sealed class AuthApiResult<out T> {
    data class Ok<T>(val value: T) : AuthApiResult<T>()
    data class Err(val message: String, val code: String? = null, val httpStatus: Int? = null) : AuthApiResult<Nothing>()
}

data class LoginTokens(
    val accessToken: String,
    val refreshToken: String?,
    val sessionId: String?,
    val expiresInSeconds: Long,
)

data class AdminUserListItem(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String? = null,
    val enabled: Boolean,
    val verified: Boolean,
    val provider: String,
    val roles: List<String>,
    val createdAt: String? = null,
    val lastActiveAt: String? = null,
    val online: Boolean = false,
    val blockReason: String? = null,
    val blockedAt: String? = null,
)

data class AdminRoleItem(
    val id: String,
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val isSystem: Boolean = false,
    /** Backenddan kelgan ruxsatlar ro'yxati (masalan: ["PRODUCT_READ", "ORDER_VIEW", ...]). */
    val permissions: List<String> = emptyList(),
)

/** Bitta permission elementi (backend `/api/admin/roles/permissions` dan). */
data class PermissionItem(
    val name: String,          // enum nomi: USER_VIEW, PRODUCT_CREATE …
    val displayName: String,   // odam o'qiydigan nom: "user view"
)

/** Kategoriya bo'yicha guruhlangan permissionlar. */
data class PermissionGroup(
    val category: String,               // masalan: "USER", "PRODUCT"
    val permissions: List<PermissionItem>,
)

data class AdminUsersPageResult(
    val items: List<AdminUserListItem>,
    val page: Int,
    val totalElements: Long,
    val totalPages: Int,
    /** Tizim bo'yicha hozir faol (muddati o'tmagan) sessiyalar soni. */
    val activeSessions: Int = 0,
    /** Filtrlarga mos keladigan admin/super adminlar soni (sahifaga bog'liq emas). */
    val adminCount: Long = 0,
    /** Filtrlarga mos keladigan warehouse rolidagilar soni. */
    val warehouseCount: Long = 0,
)

/**
 * Auth service REST API:
 * `/api/auth/login`, `/register`, `/verify-email`, `/resend-code`,
 * `/forgot-password`, `/verify-reset-code`, `/reset-password`.
 */
class AuthApiClient(
    private val baseUrl: String = AuthConfig.baseUrl,
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build(),
    private val net: AuthNetworkStrings,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val refreshMutex = Mutex()

    /**
     * Access token [TokenStore.isAccessValid] bo‘lmasa, `refreshToken` orqali yangilaydi.
     * `isAccessValid` tugashidan ~5 daqiqa oldin `false` bo‘ladi. Refresh muvaffaqiyatsiz va
     * access ishlatib bo‘lmasa `null`.
     */
    suspend fun ensureFreshAccessToken(): String? = withContext(Dispatchers.IO) {
        if (TokenStore.isAccessValid()) {
            return@withContext AuthSession.accessToken
        }
        val refresh = AuthSession.refreshToken ?: return@withContext null
        refreshMutex.withLock {
            if (TokenStore.isAccessValid()) {
                return@withLock AuthSession.accessToken
            }
            performRefresh(refresh)
        }
    }

    private suspend fun tryRefreshAccessToken(): String? = refreshMutex.withLock {
        if (TokenStore.isAccessValid()) {
            return@withLock AuthSession.accessToken
        }
        val refresh = AuthSession.refreshToken ?: return@withLock null
        performRefresh(refresh)
    }

    private suspend fun performRefresh(refresh: String): String? =
        when (val r = refreshTokens(refresh)) {
            is AuthApiResult.Ok -> {
                AuthSession.updateTokens(
                    newAccess = r.value.accessToken,
                    newRefresh = r.value.refreshToken ?: refresh,
                    expiresInSeconds = r.value.expiresInSeconds,
                )
                r.value.accessToken
            }
            is AuthApiResult.Err -> null
        }

    private fun shouldRetryAfterRefresh(result: AuthApiResult<ApiEnvelope>): Boolean {
        if (result !is AuthApiResult.Err) return false
        if (result.httpStatus != 401) return false
        return result.code == "INVALID_TOKEN" || result.code == "TOKEN_EXPIRED"
    }

    suspend fun login(username: String, password: String): AuthApiResult<LoginTokens> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(LoginRequestBody(username.trim(), password))
            when (val r = postJson("/api/auth/login", body)) {
                is AuthApiResult.Err -> r
                is AuthApiResult.Ok -> {
                    val data = r.value.data.obj() ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                    val token = data.stringField("token") ?: return@withContext AuthApiResult.Err(net.tokenMissing)
                    AuthApiResult.Ok(
                        LoginTokens(
                            accessToken = token,
                            refreshToken = data.stringField("refreshToken"),
                            sessionId = data.stringField("sessionId"),
                            expiresInSeconds = data["expiresIn"]
                                ?.let { (it as? JsonPrimitive)?.content?.toLongOrNull() }
                                ?: 900L,
                        ),
                    )
                }
            }
        }

    suspend fun listAdminUsers(
        accessToken: String,
        page: Int = 0,
        size: Int = 200,
    ): AuthApiResult<List<AdminUserListItem>> =
        when (val r = listAdminUsersPage(accessToken, page, size)) {
            is AuthApiResult.Err -> r
            is AuthApiResult.Ok -> AuthApiResult.Ok(r.value.items)
        }

    suspend fun listAdminUsersPage(
        accessToken: String,
        page: Int = 0,
        size: Int = 20,
        keyword: String? = null,
        role: String? = null,
        enabled: Boolean? = null,
    ): AuthApiResult<AdminUsersPageResult> =
        withContext(Dispatchers.IO) {
            val token = ensureFreshAccessToken() ?: accessToken
            val query = buildString {
                append("page=").append(page)
                append("&size=").append(size)
                if (!keyword.isNullOrBlank()) {
                    append("&keyword=").append(urlEncode(keyword.trim()))
                }
                if (!role.isNullOrBlank()) {
                    append("&role=").append(urlEncode(role))
                }
                if (enabled != null) {
                    append("&enabled=").append(enabled)
                }
            }
            when (val r = getJsonWithBearer("/api/admin/users?$query", token)) {
                is AuthApiResult.Err -> r
                is AuthApiResult.Ok -> {
                    val data = r.value.data.obj() ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                    val items = parseAdminUserItems(data["items"] as? JsonArray)
                    val pageNum = (data["page"] as? JsonPrimitive)?.content?.toIntOrNull() ?: page
                    val totalEl = (data["totalElements"] as? JsonPrimitive)?.content?.toLongOrNull() ?: items.size.toLong()
                    val totalPages = (data["totalPages"] as? JsonPrimitive)?.content?.toIntOrNull()
                        ?: if (size > 0) ((totalEl + size - 1) / size).toInt().coerceAtLeast(1) else 1
                    AuthApiResult.Ok(
                        AdminUsersPageResult(
                            items = items,
                            page = pageNum,
                            totalElements = totalEl,
                            totalPages = totalPages,
                            activeSessions = (data["activeSessions"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                            adminCount = (data["adminCount"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L,
                            warehouseCount = (data["warehouseCount"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L,
                        ),
                    )
                }
            }
        }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8)

    suspend fun createAdminUser(
        accessToken: String,
        username: String,
        email: String,
        password: String,
        roleIds: List<String>,
        enabled: Boolean,
    ): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(
                AdminCreateUserRequestBody(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    roleIds = roleIds,
                    enabled = enabled,
                ),
            )
            val token = ensureFreshAccessToken() ?: accessToken
            postJsonWithBearer("/api/admin/users", body, token).toUnit()
        }

    suspend fun deleteAdminUser(accessToken: String, userId: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val token = ensureFreshAccessToken() ?: accessToken
            deleteJsonWithBearer("/api/admin/users/$userId", token).toUnit()
        }

    private fun parseAdminUserItems(items: JsonArray?): List<AdminUserListItem> =
        (items ?: JsonArray(emptyList())).mapNotNull { node ->
            val o = node as? JsonObject ?: return@mapNotNull null
            val roles = ((o["roles"] as? JsonArray) ?: JsonArray(emptyList()))
                .mapNotNull { (it as? JsonPrimitive)?.content }
            AdminUserListItem(
                id = (o["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                username = (o["username"] as? JsonPrimitive)?.content ?: "",
                email = (o["email"] as? JsonPrimitive)?.content ?: "",
                fullName = (o["fullName"] as? JsonPrimitive)?.content?.ifBlank { null },
                enabled = (o["enabled"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                verified = (o["verified"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
                provider = (o["provider"] as? JsonPrimitive)?.content ?: "LOCAL",
                roles = roles,
                createdAt = (o["createdAt"] as? JsonPrimitive)?.content,
                lastActiveAt = (o["lastActiveAt"] as? JsonPrimitive)?.content,
                online = (o["online"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
                blockReason = (o["blockReason"] as? JsonPrimitive)?.content,
                blockedAt = (o["blockedAt"] as? JsonPrimitive)?.content,
            )
        }

    suspend fun blockUser(accessToken: String, userId: String, reason: String?): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = if (reason.isNullOrBlank()) "{}"
            else {
                val encoded = reason.trim().replace("\\", "\\\\").replace("\"", "\\\"")
                """{"reason":"$encoded"}"""
            }
            val token = ensureFreshAccessToken() ?: accessToken
            postJsonWithBearer("/api/admin/users/$userId/block", body, token).toUnit()
        }

    suspend fun unblockUser(accessToken: String, userId: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val token = ensureFreshAccessToken() ?: accessToken
            postJsonWithBearer("/api/admin/users/$userId/unblock", "{}", token).toUnit()
        }

    suspend fun listRoles(accessToken: String): AuthApiResult<List<AdminRoleItem>> =
        withContext(Dispatchers.IO) {
            val token = ensureFreshAccessToken() ?: accessToken
            when (val r = getJsonWithBearer("/api/admin/roles", token)) {
                is AuthApiResult.Err -> r
                is AuthApiResult.Ok -> {
                    // Backend returns `"data": [...]` — a JsonArray, not a JsonObject.
                    val rawData = r.value.data
                        ?: return@withContext AuthApiResult.Err(net.emptyResponse)

                    // Support both shapes:
                    //   shape A: data = [role1, role2, ...]          (GET /api/admin/roles)
                    //   shape B: data = { "items": [...], ... }      (paged endpoints)
                    val items: JsonArray = when (rawData) {
                        is JsonArray  -> rawData
                        is JsonObject -> rawData["items"] as? JsonArray ?: JsonArray(emptyList())
                        else          -> JsonArray(emptyList())
                    }

                    val mapped = items.mapNotNull { node ->
                        val o = node as? JsonObject ?: return@mapNotNull null
                        val perms = (o["permissions"] as? JsonArray)
                            ?.mapNotNull { (it as? JsonPrimitive)?.content }
                            ?: emptyList()
                        AdminRoleItem(
                            id = (o["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                            name = (o["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                            displayName = (o["displayName"] as? JsonPrimitive)?.content,
                            description = (o["description"] as? JsonPrimitive)?.content,
                            isSystem = (o["isSystem"] as? JsonPrimitive)?.content
                                ?.toBooleanStrictOrNull() ?: false,
                            permissions = perms,
                        )
                    }
                    AuthApiResult.Ok(mapped)
                }
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Role CRUD
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun createRole(
        accessToken: String,
        name: String,
        displayName: String?,
        description: String?,
        permissionNames: Set<String>,
    ): AuthApiResult<AdminRoleItem> = withContext(Dispatchers.IO) {
        val token = ensureFreshAccessToken() ?: accessToken
        val body = json.encodeToString(
            CreateRoleRequestBody(
                name = name,
                displayName = displayName?.takeIf { it.isNotBlank() },
                description = description?.takeIf { it.isNotBlank() },
                permissionNames = permissionNames,
            ),
        )
        when (val r = postJsonWithBearer("/api/admin/roles", body, token)) {
            is AuthApiResult.Err -> r
            is AuthApiResult.Ok -> {
                val data = r.value.data.obj()
                    ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                val id = data.stringField("id")
                    ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                val perms = (data["permissions"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
                AuthApiResult.Ok(
                    AdminRoleItem(
                        id          = id,
                        name        = data.stringField("name") ?: name,
                        displayName = data.stringField("displayName"),
                        description = data.stringField("description"),
                        isSystem    = (data["isSystem"] as? JsonPrimitive)
                            ?.content?.toBooleanStrictOrNull() ?: false,
                        permissions = perms,
                    ),
                )
            }
        }
    }

    /** `PUT /api/admin/roles/{id}` — mavjud rolni yangilash (displayName, description, permissions). */
    suspend fun updateRole(
        accessToken: String,
        roleId: String,
        roleCode: String,           // backend "name" = ROLE_... kodi (@NotBlank)
        displayName: String?,
        description: String?,
        permissionNames: Set<String>,
    ): AuthApiResult<AdminRoleItem> = withContext(Dispatchers.IO) {
        val token = ensureFreshAccessToken() ?: accessToken
        val body = json.encodeToString(
            UpdateRoleRequestBody(
                name            = roleCode,
                displayName    = displayName?.takeIf { it.isNotBlank() },
                description    = description?.takeIf { it.isNotBlank() },
                permissionNames = permissionNames,
            ),
        )
        when (val r = putJsonWithBearer("/api/admin/roles/$roleId", body, token)) {
            is AuthApiResult.Err -> r
            is AuthApiResult.Ok  -> {
                val data = r.value.data.obj()
                    ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                val id = data.stringField("id")
                    ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                val perms = (data["permissions"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
                AuthApiResult.Ok(
                    AdminRoleItem(
                        id          = id,
                        name        = data.stringField("name") ?: roleId,
                        displayName = data.stringField("displayName"),
                        description = data.stringField("description"),
                        isSystem    = (data["isSystem"] as? JsonPrimitive)
                            ?.content?.toBooleanStrictOrNull() ?: false,
                        permissions = perms,
                    ),
                )
            }
        }
    }

    /** `DELETE /api/admin/roles/{id}` — rolni o'chirish (faqat tizim roli bo'lmaganlar). */
    suspend fun deleteRole(accessToken: String, roleId: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val token = ensureFreshAccessToken() ?: accessToken
            deleteJsonWithBearer("/api/admin/roles/$roleId", token).toUnit()
        }

    /** `GET /api/admin/roles/permissions` — barcha permission enum'lar kategoriya bo'yicha. */
    suspend fun listPermissions(accessToken: String): AuthApiResult<List<PermissionGroup>> =
        withContext(Dispatchers.IO) {
            val token = ensureFreshAccessToken() ?: accessToken
            when (val r = getJsonWithBearer("/api/admin/roles/permissions", token)) {
                is AuthApiResult.Err -> r
                is AuthApiResult.Ok -> {
                    val rawData = r.value.data
                        ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                    val arr: JsonArray = when (rawData) {
                        is JsonArray  -> rawData
                        is JsonObject -> rawData["items"] as? JsonArray ?: JsonArray(emptyList())
                        else          -> JsonArray(emptyList())
                    }
                    val groups = arr.mapNotNull { node ->
                        val o = node as? JsonObject ?: return@mapNotNull null
                        val category = o.stringField("category") ?: return@mapNotNull null
                        val permsArr = o["permissions"] as? JsonArray ?: return@mapNotNull null
                        val items = permsArr.mapNotNull { p ->
                            val po = p as? JsonObject ?: return@mapNotNull null
                            val n = po.stringField("name") ?: return@mapNotNull null
                            val d = po.stringField("displayName") ?: n
                            PermissionItem(name = n, displayName = d)
                        }
                        PermissionGroup(category = category, permissions = items)
                    }
                    AuthApiResult.Ok(groups)
                }
            }
        }

    suspend fun setUserRoles(
        accessToken: String,
        userId: String,
        roleIds: List<String>,
    ): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val parsedRoleIds = roleIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            if (parsedRoleIds.size != roleIds.size || parsedRoleIds.isEmpty()) {
                return@withContext AuthApiResult.Err("Role ID formati noto'g'ri (UUID bo'lishi kerak).")
            }
            val body = Json.encodeToString(
                SetUserRolesRequestBody(roleIds = parsedRoleIds.map { it.toString() }),
            )
            val token = ensureFreshAccessToken() ?: accessToken
            putJsonWithBearer("/api/admin/users/$userId/roles", body, token).toUnit()
        }

    suspend fun refreshTokens(refreshToken: String): AuthApiResult<LoginTokens> =
        withContext(Dispatchers.IO) {
            val body = Json.encodeToString(
                RefreshTokenRequestBody(refreshToken = refreshToken),
            )
            when (val r = postJson("/api/auth/refresh", body)) {
                is AuthApiResult.Err -> r
                is AuthApiResult.Ok -> {
                    val data = r.value.data.obj()
                        ?: return@withContext AuthApiResult.Err(net.emptyResponse)
                    val token = data.stringField("token")
                        ?: return@withContext AuthApiResult.Err(net.tokenMissing)
                    AuthApiResult.Ok(
                        LoginTokens(
                            accessToken = token,
                            refreshToken = data.stringField("refreshToken"),
                            sessionId = data.stringField("sessionId"),
                            expiresInSeconds = data["expiresIn"]
                                ?.let { (it as? JsonPrimitive)?.content?.toLongOrNull() }
                                ?: 900L,
                        ),
                    )
                }
            }
        }

    /**
     * Dastur yana ochilganda yoki dashboardga kirilganda: refresh token bo‘lsa,
     * serverdan yangi access/refresh olishga harakat qiladi.
     * Refresh xato bersa, lekin access hali [TokenStore.isAccessValid] bo‘lsa, sessiya saqlanadi.
     */
    suspend fun refreshAccessOnAppResume(): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val rt = AuthSession.refreshToken
            if (rt.isNullOrBlank()) {
                return@withLock !AuthSession.accessToken.isNullOrBlank()
            }
            if (performRefresh(rt) != null) {
                true
            } else {
                TokenStore.isAccessValid() && !AuthSession.accessToken.isNullOrBlank()
            }
        }
    }

    suspend fun logout(accessToken: String, refreshToken: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = Json.encodeToString(
                LogoutRequestBody(refreshToken = refreshToken),
            )
            postJsonWithBearer("/api/auth/logout", body, accessToken).toUnit()
        }

    suspend fun register(username: String, email: String, password: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(RegisterRequestBody(username.trim(), email.trim(), password))
            postJson("/api/auth/register", body).toUnit()
        }

    suspend fun verifyEmail(email: String, otpCode: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(VerifyEmailRequestBody(email.trim(), otpCode.trim()))
            postJson("/api/auth/verify-email", body).toUnit()
        }

    suspend fun resendCode(email: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(ResendCodeRequestBody(email.trim()))
            postJson("/api/auth/resend-code", body).toUnit()
        }

    suspend fun forgotPassword(email: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(ForgotPasswordRequestBody(email.trim()))
            postJson("/api/auth/forgot-password", body).toUnit()
        }

    suspend fun verifyResetCode(email: String, code: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(VerifyResetCodeRequestBody(email.trim(), code.trim()))
            postJson("/api/auth/verify-reset-code", body).toUnit()
        }

    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthApiResult<Unit> =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(
                ResetPasswordRequestBody(
                    email = email.trim(),
                    code = code.trim(),
                    newPassword = newPassword,
                ),
            )
            postJson("/api/auth/reset-password", body).toUnit()
        }

    private fun postJson(path: String, jsonBody: String): AuthApiResult<ApiEnvelope> {
        val uri = URI.create(baseUrl + path)
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()
        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            val text = response.body() ?: ""
            if (status == 429) {
                return AuthApiResult.Err(net.rateLimited, httpStatus = status)
            }
            val envelope = runCatching { json.decodeFromString<ApiEnvelope>(text) }.getOrNull()
            if (envelope != null) {
                val httpOk = status in 200..299
                if (envelope.success && httpOk) {
                    AuthApiResult.Ok(envelope)
                } else {
                    val msg = envelope.error?.message ?: if (!httpOk) "HTTP $status" else net.requestFailed
                    AuthApiResult.Err(msg, envelope.error?.code, status)
                }
            } else if (status in 200..299) {
                AuthApiResult.Ok(ApiEnvelope(success = true, data = JsonObject(emptyMap())))
            } else {
                AuthApiResult.Err(parseRoughError(status, text), httpStatus = status)
            }
        } catch (e: Exception) {
            AuthApiResult.Err(e.message ?: net.networkError)
        }
    }

    private suspend fun getJsonWithBearer(path: String, bearerToken: String): AuthApiResult<ApiEnvelope> {
        val first = doGetJsonWithBearer(path, bearerToken)
        if (shouldRetryAfterRefresh(first)) {
            val newToken = tryRefreshAccessToken()
            if (newToken != null) return doGetJsonWithBearer(path, newToken)
        }
        return first
    }

    private fun doGetJsonWithBearer(path: String, bearerToken: String): AuthApiResult<ApiEnvelope> {
        val uri = URI.create(baseUrl + path)
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer $bearerToken")
            .GET()
            .build()
        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            val text = response.body() ?: ""
            if (status == 429) return AuthApiResult.Err(net.rateLimited, httpStatus = status)
            val envelope = runCatching { json.decodeFromString<ApiEnvelope>(text) }.getOrNull()
            if (envelope != null) {
                val httpOk = status in 200..299
                if (envelope.success && httpOk) AuthApiResult.Ok(envelope)
                else {
                    val msg = envelope.error?.message ?: if (!httpOk) "HTTP $status" else net.requestFailed
                    AuthApiResult.Err(msg, envelope.error?.code, status)
                }
            } else if (status in 200..299) {
                AuthApiResult.Ok(ApiEnvelope(success = true, data = JsonObject(emptyMap())))
            } else {
                AuthApiResult.Err(parseRoughError(status, text), httpStatus = status)
            }
        } catch (e: Exception) {
            AuthApiResult.Err(e.message ?: net.networkError)
        }
    }

    private suspend fun postJsonWithBearer(
        path: String,
        jsonBody: String,
        bearerToken: String,
    ): AuthApiResult<ApiEnvelope> =
        jsonWithBearer(HttpMethod.POST, path, jsonBody, bearerToken)

    private suspend fun putJsonWithBearer(
        path: String,
        jsonBody: String,
        bearerToken: String,
    ): AuthApiResult<ApiEnvelope> =
        jsonWithBearer(HttpMethod.PUT, path, jsonBody, bearerToken)

    private suspend fun deleteJsonWithBearer(path: String, bearerToken: String): AuthApiResult<ApiEnvelope> =
        jsonWithBearer(HttpMethod.DELETE, path, "", bearerToken)

    private enum class HttpMethod { POST, PUT, DELETE }

    private suspend fun jsonWithBearer(
        method: HttpMethod,
        path: String,
        jsonBody: String,
        bearerToken: String,
    ): AuthApiResult<ApiEnvelope> {
        val first = doJsonWithBearer(method, path, jsonBody, bearerToken)
        if (shouldRetryAfterRefresh(first)) {
            val newToken = tryRefreshAccessToken()
            if (newToken != null) return doJsonWithBearer(method, path, jsonBody, newToken)
        }
        return first
    }

    private fun doJsonWithBearer(
        method: HttpMethod,
        path: String,
        jsonBody: String,
        bearerToken: String,
    ): AuthApiResult<ApiEnvelope> {
        val uri = URI.create(baseUrl + path)
        val builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $bearerToken")
        val request = when (method) {
            HttpMethod.POST -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build()
            HttpMethod.PUT -> builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody)).build()
            HttpMethod.DELETE -> builder.DELETE().build()
        }
        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            val text = response.body() ?: ""
            if (status == 429) return AuthApiResult.Err(net.rateLimited, httpStatus = status)
            val envelope = runCatching { json.decodeFromString<ApiEnvelope>(text) }.getOrNull()
            if (envelope != null) {
                val httpOk = status in 200..299
                if (envelope.success && httpOk) AuthApiResult.Ok(envelope)
                else {
                    val msg = envelope.error?.message ?: if (!httpOk) "HTTP $status" else net.requestFailed
                    AuthApiResult.Err(msg, envelope.error?.code, status)
                }
            } else if (status in 200..299) {
                AuthApiResult.Ok(ApiEnvelope(success = true, data = JsonObject(emptyMap())))
            } else {
                AuthApiResult.Err(parseRoughError(status, text), httpStatus = status)
            }
        } catch (e: Exception) {
            AuthApiResult.Err(e.message ?: net.networkError)
        }
    }

    private fun parseRoughError(status: Int, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "HTTP $status"
        if (trimmed.length > 200) return "${net.serverErrorPrefix} (${trimmed.take(80)}…)"
        return trimmed.ifBlank { net.unknownError }
    }
}

private fun AuthApiResult<ApiEnvelope>.toUnit(): AuthApiResult<Unit> =
    when (this) {
        is AuthApiResult.Ok -> AuthApiResult.Ok(Unit)
        is AuthApiResult.Err -> this
    }

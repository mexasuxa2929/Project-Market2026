package mexa.club.desktop_app.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class LoginRequestBody(
    @SerialName("usernameOrEmail") val username: String,
    val password: String,
)

@Serializable
internal data class RegisterRequestBody(
    val username: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class VerifyEmailRequestBody(
    val email: String,
    @SerialName("otpCode") val otpCode: String,
)

@Serializable
internal data class ResendCodeRequestBody(
    val email: String,
)

@Serializable
internal data class ForgotPasswordRequestBody(
    val email: String,
)

@Serializable
internal data class VerifyResetCodeRequestBody(
    val email: String,
    val code: String,
)

@Serializable
internal data class ResetPasswordRequestBody(
    val email: String,
    val code: String,
    val newPassword: String,
)

@Serializable
internal data class RefreshTokenRequestBody(
    val refreshToken: String,
)

@Serializable
internal data class LogoutRequestBody(
    val refreshToken: String,
)

@Serializable
internal data class SetUserRolesRequestBody(
    val roleIds: List<String>,
)

@Serializable
internal data class AdminCreateUserRequestBody(
    val username: String,
    val email: String,
    val password: String,
    val roleIds: List<String> = emptyList(),
    val enabled: Boolean = true,
)

@Serializable
internal data class CreateRoleRequestBody(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val permissionNames: Set<String> = emptySet(),
)

@Serializable
internal data class UpdateRoleRequestBody(
    val name: String,                       // @NotBlank — backend talab qiladi (rol kodi)
    val displayName: String? = null,
    val description: String? = null,
    val permissionNames: Set<String> = emptySet(),
)

@Serializable
internal data class ApiEnvelope(
    val success: Boolean = false,
    /**
     * Backend ba'zi endpointlarda `data` ni [JsonObject], ba'zilarida [JsonArray] sifatida qaytaradi.
     * Masalan: GET /api/admin/roles → `"data": [...]`
     * Shuning uchun [JsonElement] ishlatiladi — har ikki holat ham qo'llab-quvvatlanadi.
     */
    val data: JsonElement? = null,
    val error: ApiErrorPayload? = null,
)

/** [JsonElement]ni [JsonObject] sifatida xavfsiz kesish (array yoki null bo'lsa `null` qaytaradi). */
internal fun JsonElement?.obj(): JsonObject? = this as? JsonObject

@Serializable
internal data class ApiErrorPayload(
    val code: String? = null,
    val message: String? = null,
)

internal fun JsonObject.stringField(key: String): String? =
    (this[key] as? JsonPrimitive)?.content

package com.example.mobile_app.data.model.auth

data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class VerifyEmailRequest(
    val email: String,
    val otpCode: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class GoogleAuthRequest(val idToken: String)

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyResetCodeRequest(
    val email: String,
    val code: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

data class AuthTokenData(
    val token: String,
    val refreshToken: String,
    val sessionId: String?,
    val expiresIn: Long?,
    val tokenType: String?
)

data class AuthResponse(
    val success: Boolean,
    val data: AuthTokenData?,
    val error: ErrorPayload?
)

data class MessageData(val message: String)

data class RegisterResponse(
    val success: Boolean,
    val data: MessageData?,
    val error: ErrorPayload?
)

data class ErrorPayload(
    val code: String?,
    val message: String?
)

data class UserMeData(
    val id: String?,
    val username: String?,
    val email: String?,
    val fullName: String?
)

data class UserMeResponse(
    val success: Boolean,
    val data: UserMeData?,
    val error: ErrorPayload?
)

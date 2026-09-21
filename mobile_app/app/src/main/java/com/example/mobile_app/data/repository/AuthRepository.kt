package com.example.mobile_app.data.repository

import com.example.mobile_app.data.local.TokenManager
import com.example.mobile_app.data.model.auth.AuthResponse
import com.example.mobile_app.data.model.auth.AuthTokenData
import com.example.mobile_app.data.model.auth.ForgotPasswordRequest
import com.example.mobile_app.data.model.auth.GoogleAuthRequest
import com.example.mobile_app.data.model.auth.LoginRequest
import com.example.mobile_app.data.model.auth.RefreshTokenRequest
import com.example.mobile_app.data.model.auth.RegisterRequest
import com.example.mobile_app.data.model.auth.RegisterResponse
import com.example.mobile_app.data.model.auth.ResetPasswordRequest
import com.example.mobile_app.data.model.auth.UserMeData
import com.example.mobile_app.data.model.auth.VerifyEmailRequest
import com.example.mobile_app.data.remote.AuthApiService

class AuthRepository(private val authApiService: AuthApiService) {

    suspend fun login(username: String, password: String): Result<AuthResponse> = try {
        val response = authApiService.login(LoginRequest(usernameOrEmail = username, password = password))
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                saveTokens(body.data)
                TokenManager.username = username
                // fullName ni serverdan olib saqlash
                try {
                    val me = authApiService.getCurrentUser()
                    if (me.isSuccessful) {
                        TokenManager.fullName = me.body()?.data?.fullName
                        me.body()?.data?.email?.let { TokenManager.email = it }
                    }
                } catch (_: Exception) {}
                Result.success(body)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Login failed"))
            }
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(username: String, email: String, password: String): Result<RegisterResponse> = try {
        val response = authApiService.register(RegisterRequest(username, email, password))
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) {
                TokenManager.email = email
                Result.success(body)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Registration failed"))
            }
        } else {
            val msg = parseErrorBody(response) ?: "HTTP ${response.code()}: ${response.message()}"
            Result.failure(Exception(msg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parseErrorBody(response: retrofit2.Response<*>): String? {
        return try {
            val raw = response.errorBody()?.string() ?: return null
            // Backend: {"success":false,"error":{"code":"EMAIL_IN_USE","message":"Email is already in use"}}
            when {
                raw.contains("EMAIL_IN_USE") -> "EMAIL_IN_USE"
                raw.contains("USERNAME_TAKEN") -> "USERNAME_TAKEN"
                raw.contains("\"message\"") -> {
                    Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    suspend fun verifyEmail(email: String, code: String): Result<AuthResponse> = try {
        val response = authApiService.verifyEmail(VerifyEmailRequest(email, otpCode = code))
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data?.token != null) {
                saveTokens(body.data)
                TokenManager.email = email
                // fullName ni serverdan olib saqlash (bo'lmasa Profil da "Foydalanuvchi" chiqib qoladi)
                try {
                    val me = authApiService.getCurrentUser()
                    if (me.isSuccessful) {
                        me.body()?.data?.fullName?.trim()?.takeIf { it.isNotBlank() }?.let {
                            TokenManager.fullName = it
                        }
                        me.body()?.data?.username?.trim()?.takeIf { it.isNotBlank() }?.let {
                            TokenManager.username = it
                        }
                        me.body()?.data?.email?.trim()?.takeIf { it.isNotBlank() }?.let {
                            TokenManager.email = it
                        }
                    }
                } catch (_: Exception) {}
                Result.success(body)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Verification failed"))
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun refreshToken(): Result<AuthResponse> = try {
        val refreshTok = TokenManager.refreshToken
            ?: return Result.failure(Exception("No refresh token"))
        val response = authApiService.refreshToken(RefreshTokenRequest(refreshTok))
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                saveTokens(body.data)
                Result.success(body)
            } else {
                TokenManager.clear()
                Result.failure(Exception("Session expired"))
            }
        } else {
            TokenManager.clear()
            Result.failure(Exception("Session expired"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> = try {
        val response = authApiService.loginWithGoogle(GoogleAuthRequest(idToken))
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                saveTokens(body.data)
                try {
                    val me = authApiService.getCurrentUser()
                    if (me.isSuccessful) {
                        TokenManager.fullName = me.body()?.data?.fullName
                        me.body()?.data?.email?.let { TokenManager.email = it }
                    }
                } catch (_: Exception) {}
                Result.success(body)
            } else {
                Result.failure(Exception(body?.error?.message ?: "Google login failed"))
            }
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resendCode(email: String): Result<Unit> = try {
        val response = authApiService.resendCode(mapOf("email" to email))
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun forgotPassword(email: String): Result<Unit> = try {
        val response = authApiService.forgotPassword(ForgotPasswordRequest(email))
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            val raw = try { response.errorBody()?.string() } catch (_: Exception) { null }
            val msg = when {
                raw != null && raw.contains("EMAIL_NOT_FOUND") -> "EMAIL_NOT_FOUND"
                raw != null && raw.contains("\"message\"") -> Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1) ?: "Email topilmadi"
                else -> "Email topilmadi"
            }
            Result.failure(Exception(msg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun verifyResetCode(email: String, code: String): Result<Unit> = try {
        val response = authApiService.verifyResetCode(com.example.mobile_app.data.model.auth.VerifyResetCodeRequest(email, code))
        if (response.isSuccessful) Result.success(Unit)
        else {
            val msg = parseErrorBody(response) ?: "Kod noto'g'ri"
            Result.failure(Exception(msg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> = try {
        val response = authApiService.resetPassword(ResetPasswordRequest(email, code, newPassword))
        if (response.isSuccessful) Result.success(Unit)
        else {
            val msg = parseErrorBody(response) ?: "Parolni yangilashda xatolik"
            Result.failure(Exception(msg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCurrentUser(): Result<UserMeData> = try {
        val response = authApiService.getCurrentUser()
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                // Keshni yangilab qo'yamiz — Profil ekrani shundan fullname ko'rsatadi
                body.data.fullName?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let {
                    TokenManager.fullName = it
                }
                body.data.username?.trim()?.takeIf { it.isNotBlank() }?.let {
                    TokenManager.username = it
                }
                body.data.email?.trim()?.takeIf { it.isNotBlank() }?.let {
                    TokenManager.email = it
                }
                Result.success(body.data)
            }
            else Result.failure(Exception(body?.error?.message ?: "Failed to load profile"))
        } else Result.failure(Exception("HTTP ${response.code()}"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateFullName(fullName: String): Result<UserMeData> = try {
        val response = authApiService.updateCurrentUser(mapOf("fullName" to fullName.trim()))
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                body.data.fullName?.trim()?.takeIf { it.isNotBlank() }?.let {
                    TokenManager.fullName = it
                } ?: run { TokenManager.fullName = fullName.trim() }
                Result.success(body.data)
            }
            else Result.failure(Exception(body?.error?.message ?: "Failed to update name"))
        } else Result.failure(Exception("HTTP ${response.code()}"))
    } catch (e: Exception) { Result.failure(e) }

    fun logout() {
        TokenManager.clear()
    }

    /** Tokenlarni va muddatini TokenManager ga saqlaydi */
    private fun saveTokens(data: AuthTokenData) {
        TokenManager.accessToken = data.token
        TokenManager.refreshToken = data.refreshToken
        // expiresIn soniya bo'lsa, ms ga aylantirib saqlaymiz
        if (data.expiresIn != null && data.expiresIn > 0) {
            TokenManager.tokenExpiresAt = System.currentTimeMillis() + (data.expiresIn * 1000)
        }
    }
}

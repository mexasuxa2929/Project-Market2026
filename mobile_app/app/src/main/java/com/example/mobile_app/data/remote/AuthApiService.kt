package com.example.mobile_app.data.remote

import com.example.mobile_app.data.model.auth.AuthResponse
import com.example.mobile_app.data.model.auth.ForgotPasswordRequest
import com.example.mobile_app.data.model.auth.GoogleAuthRequest
import com.example.mobile_app.data.model.auth.LoginRequest
import com.example.mobile_app.data.model.auth.RefreshTokenRequest
import com.example.mobile_app.data.model.auth.RegisterRequest
import com.example.mobile_app.data.model.auth.RegisterResponse
import com.example.mobile_app.data.model.auth.ResetPasswordRequest
import com.example.mobile_app.data.model.auth.VerifyEmailRequest
import com.example.mobile_app.data.model.auth.UserMeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<AuthResponse>

    @POST("api/auth/resend-code")
    suspend fun resendCode(@Body body: Map<String, String>): Response<RegisterResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<RegisterResponse>

    @POST("api/auth/verify-reset-code")
    suspend fun verifyResetCode(@Body request: com.example.mobile_app.data.model.auth.VerifyResetCodeRequest): Response<RegisterResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<RegisterResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Response<RegisterResponse>

    @GET("api/admin/users/me")
    suspend fun getCurrentUser(): Response<UserMeResponse>

    @PUT("api/admin/users/me")
    suspend fun updateCurrentUser(@Body body: Map<String, String>): Response<UserMeResponse>
}

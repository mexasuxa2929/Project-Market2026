package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_app.data.local.TokenManager
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val needsEmailVerification: Boolean = false,
    val pendingEmail: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val needsFullName: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository(RetrofitClient.authApiService)

    private val _uiState = MutableStateFlow(
        AuthUiState(isLoggedIn = TokenManager.isLoggedIn)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (TokenManager.isLoggedIn) checkFullName()
    }

    fun login(usernameOrEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.login(usernameOrEmail.trim(), password).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    checkFullName()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.register(username.trim(), email.trim().lowercase(), password).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsEmailVerification = true,
                        pendingEmail = email.trim().lowercase(),
                        successMessage = response.data?.message
                    )
                },
                onFailure = { e ->
                    val raw = e.message ?: "Registration failed"
                    val friendly = when {
                        raw.contains("EMAIL_IN_USE") || raw.contains("Email is already in use") ->
                            "Bu email allaqachon ro'yxatdan o'tgan. Iltimos, kiring yoki boshqa email ishlating."
                        raw.contains("USERNAME_TAKEN") || raw.contains("Username is already taken") ->
                            "Bu foydalanuvchi nomi band. Boshqa nom tanlang."
                        raw.contains("409") || raw.contains("Conflict") ->
                            "Bu email yoki ism allaqachon mavjud."
                        else -> raw
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = friendly
                    )
                }
            )
        }
    }

    fun verifyEmail(code: String) {
        val email = _uiState.value.pendingEmail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.verifyEmail(email, code.trim()).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        needsEmailVerification = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Verification failed"
                    )
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.loginWithGoogle(idToken).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    checkFullName()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Google orqali kirish muvaffaqiyatsiz"
                    )
                }
            )
        }
    }

    private fun checkFullName() {
        viewModelScope.launch {
            repository.getCurrentUser().fold(
                onSuccess = { user ->
                    val fn = user.fullName?.trim()
                    // getCurrentUser allaqachon TokenManager ga saqlaydi, shu yerda ham kafolatlaymiz
                    if (!fn.isNullOrBlank() && !fn.equals("null", ignoreCase = true)) {
                        TokenManager.fullName = fn
                    }
                    if (fn.isNullOrBlank() || fn.equals("null", ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(needsFullName = true)
                    }
                },
                onFailure = { /* e'tiborsiz, keyin profil da to'ldiradi */ }
            )
        }
    }

    fun submitFullName(fullName: String) {
        val trimmed = fullName.trim()
        if (trimmed.length < 2) {
            _uiState.value = _uiState.value.copy(error = "Ism kamida 2 ta belgi bo'lsin")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.updateFullName(trimmed).fold(
                onSuccess = { user ->
                    // Darhol keshga yozamiz — Profil ekrani kutmasdan fullname ko'rsatadi
                    TokenManager.fullName = user.fullName?.trim()?.takeIf { it.isNotBlank() } ?: trimmed
                    _uiState.value = _uiState.value.copy(isLoading = false, needsFullName = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Saqlashda xatolik")
                }
            )
        }
    }

    fun dismissFullNamePrompt() {
        _uiState.value = _uiState.value.copy(needsFullName = false)
    }

    fun resendCode() {
        val email = _uiState.value.pendingEmail ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.resendCode(email).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            repository.forgotPassword(email.trim()).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "OK"
                    )
                },
                onFailure = { e ->
                    val raw = e.message ?: ""
                    val friendly = when {
                        raw.contains("EMAIL_NOT_FOUND") -> "Bu email tizimda topilmadi. Iltimos, emailni tekshiring yoki ro'yxatdan o'ting."
                        raw.contains("not found", ignoreCase = true) -> "Bu email tizimda topilmadi."
                        else -> raw.ifBlank { "Xatolik yuz berdi" }
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = friendly)
                }
            )
        }
    }

    fun verifyResetCode(email: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.verifyResetCode(email.trim(), code.trim()).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Kod noto'g'ri")
                }
            )
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.resetPassword(email.trim(), code.trim(), newPassword).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "OK")
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Xatolik")
                }
            )
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearVerificationState() {
        _uiState.value = _uiState.value.copy(
            needsEmailVerification = false,
            pendingEmail = null,
            error = null
        )
    }
}

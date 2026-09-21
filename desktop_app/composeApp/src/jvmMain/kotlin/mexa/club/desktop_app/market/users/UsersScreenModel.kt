package mexa.club.desktop_app.market.users

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AdminRoleItem
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.market.data.UsersRepository
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.model.CreateUserForm
import mexa.club.desktop_app.market.model.UserRole
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.model.userRoleToBackendName

data class UsersUiState(
    val users: List<AdminUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val pageSize: Int = 5,
    val searchQuery: String = "",
    val roleFilter: String? = null,   // Backend rol nomi ("ROLE_ADMIN") yoki null
    val statusFilter: String? = null, // "ACTIVE" / "BLOCKED" / null
    val assignableRoles: List<AdminRoleItem> = emptyList(),
    val adminCount: Int = 0,
    val warehouseCount: Int = 0,
    val activeSessions: Int = 0,
) {
    companion object {
        val Initial = UsersUiState()
    }
}

/** MVVM: Voyager ScreenModel + StateFlow + Repository. */
class UsersScreenModel(
    private val repository: UsersRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(UsersUiState.Initial)
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    fun loadRoles() {
        screenModelScope.launch {
            val token = AuthSession.accessToken ?: return@launch
            when (val r = repository.listRoles(token)) {
                is AuthApiResult.Ok -> {
                    _uiState.update { it.copy(assignableRoles = r.value) }
                }
                is AuthApiResult.Err -> {
                }
            }
        }
    }

    fun loadUsers() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val token = AuthSession.accessToken
            if (token.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Sessiya topilmadi") }
                return@launch
            }
            val s = _uiState.value
            when (
                val r = repository.listUsersPage(
                    token = token,
                    page = s.currentPage - 1,
                    size = s.pageSize,
                    keyword = s.searchQuery.takeIf { it.isNotBlank() },
                    role = s.roleFilter,
                    enabled = when (s.statusFilter) {
                        UserStatus.ACTIVE.name -> true
                        UserStatus.BLOCKED.name -> false
                        else -> null
                    },
                )
            ) {
                is AuthApiResult.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = r.value.items.map { item -> item.toAdminUser() },
                            totalCount = r.value.totalElements.toInt(),
                            totalPages = r.value.totalPages.coerceAtLeast(1),
                            adminCount = r.value.adminCount.toInt(),
                            warehouseCount = r.value.warehouseCount.toInt(),
                            activeSessions = r.value.activeSessions,
                            error = null,
                        )
                    }
                }
                is AuthApiResult.Err -> _uiState.update { it.copy(isLoading = false, error = r.message) }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadUsers()
    }

    /** Backenddan kelgan rol kodi ("ROLE_ADMIN" va hokazo) yoki null — to'g'ridan-to'g'ri serverga uzatiladi. */
    fun updateRoleFilter(code: String?) {
        val rf = code?.takeIf { it.isNotBlank() } ?: null
        _uiState.update { it.copy(roleFilter = rf, currentPage = 1) }
        loadUsers()
    }

    fun updateStatusFilter(value: String?) {
        val sf = when (value) {
            null, "Barcha holat" -> null
            else -> value
        }
        _uiState.update { it.copy(statusFilter = sf, currentPage = 1) }
        loadUsers()
    }

    fun setPageSize(size: Int) {
        _uiState.update { it.copy(pageSize = size, currentPage = 1) }
        loadUsers()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(searchQuery = "", roleFilter = null, statusFilter = null, currentPage = 1)
        }
        loadUsers()
    }

    fun goToPage(page: Int) {
        val s = _uiState.value
        if (page < 1 || page > s.totalPages) return
        _uiState.update { it.copy(currentPage = page) }
        loadUsers()
    }

    fun createUser(form: CreateUserForm, onSuccess: () -> Unit) {
        screenModelScope.launch {
            val token = AuthSession.accessToken ?: return@launch
            val roleIds = resolveRoleIds(form.roles)
            when (
                val r = repository.createUser(
                    token,
                    form.username,
                    form.email,
                    form.password,
                    roleIds,
                    form.isActive,
                )
            ) {
                is AuthApiResult.Ok -> {
                    loadUsers()
                    onSuccess()
                }
                is AuthApiResult.Err -> _uiState.update { it.copy(error = r.message) }
            }
        }
    }

    fun deleteUser(id: String, onSuccess: () -> Unit) {
        screenModelScope.launch {
            val token = AuthSession.accessToken ?: return@launch
            when (val r = repository.deleteUser(token, id)) {
                is AuthApiResult.Ok -> {
                    loadUsers()
                    onSuccess()
                }
                is AuthApiResult.Err -> _uiState.update { it.copy(error = r.message) }
            }
        }
    }

    /**
     * Rol UUID (id) ni to'g'ridan-to'g'ri qabul qilib belgilaydi.
     * Barcha turdagi rollar uchun ishlaydi (standart va custom).
     */
    fun setUserRoleById(
        userId: String,
        roleId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        screenModelScope.launch {
            val token = AuthSession.accessToken ?: run { onError("Sessiya topilmadi"); return@launch }
            when (val r = repository.setUserRoles(token, userId, listOf(roleId))) {
                is AuthApiResult.Ok  -> { loadUsers(); onSuccess() }
                is AuthApiResult.Err -> onError(r.message)
            }
        }
    }

    fun blockUser(
        userId: String,
        reason: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        screenModelScope.launch {
            val token = AuthSession.accessToken ?: run { onError("Sessiya topilmadi"); return@launch }
            when (val r = repository.blockUser(token, userId, reason)) {
                is AuthApiResult.Ok -> {
                    loadUsers()
                    onSuccess()
                }
                is AuthApiResult.Err -> onError(r.message ?: "Bloklashda xatolik yuz berdi")
            }
        }
    }

    fun unblockUser(
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        screenModelScope.launch {
            val token = AuthSession.accessToken ?: run { onError("Sessiya topilmadi"); return@launch }
            when (val r = repository.unblockUser(token, userId)) {
                is AuthApiResult.Ok -> {
                    loadUsers()
                    onSuccess()
                }
                is AuthApiResult.Err -> onError(r.message ?: "Blokdan chiqarishda xatolik yuz berdi")
            }
        }
    }

    private fun resolveRoleIds(selected: Set<UserRole>): List<String> {
        if (selected.isEmpty()) return emptyList()
        val byName = _uiState.value.assignableRoles.associateBy { it.name }
        return selected.mapNotNull { role ->
            byName[userRoleToBackendName(role)]?.id
        }
    }
}
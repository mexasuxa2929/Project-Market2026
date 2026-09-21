package mexa.club.desktop_app.market.roles

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.auth.PermissionGroup
import mexa.club.desktop_app.market.data.RolesRepository
import mexa.club.desktop_app.market.model.CreateRoleForm
import mexa.club.desktop_app.market.model.UpdateRoleForm
import mexa.club.desktop_app.market.model.RoleDefinition
import mexa.club.desktop_app.market.model.RoleScreenStats
import mexa.club.desktop_app.market.model.RoleUserRow
import mexa.club.desktop_app.market.model.UserStatus
import mexa.club.desktop_app.market.model.userRoleToBackendName
import mexa.club.desktop_app.market.users.toAdminUser

data class RolesUiState(
    val roles: List<RoleDefinition> = emptyList(),
    val users: List<RoleUserRow> = emptyList(),
    val filteredUsers: List<RoleUserRow> = emptyList(),
    val stats: RoleScreenStats = RoleScreenStats(0, 0, 0, 0),
    val isLoading: Boolean = false,
    val error: String? = null,
    val tabFilter: String? = null,   // null = Hammasi (ALL), "ROLE_ADMIN" = aniq fil'tr
    // ── Create rol ──────────────────────────────────────
    val availablePermissions: List<PermissionGroup> = emptyList(),
    val isCreating: Boolean = false,
    val createError: String? = null,
    // ── Edit rol ────────────────────────────────────────
    val editingRole: RoleDefinition? = null,
    val isUpdating: Boolean = false,
    val updateError: String? = null,
    // ── Delete rol ──────────────────────────────────────
    val deletingRole: RoleDefinition? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
) {
    companion object {
        val Initial = RolesUiState()
    }
}

/** MVVM: Voyager ScreenModel + StateFlow + Repository. */
class RolesScreenModel(
    private val repository: RolesRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(RolesUiState.Initial)
    val uiState: StateFlow<RolesUiState> = _uiState.asStateFlow()

    @Volatile private var lastRefreshMs: Long = 0L
    private var pollingJob: Job? = null

    private companion object {
        const val DEBOUNCE_MS        = 3_000L
        const val POLLING_INTERVAL_MS = 30_000L   // 30s fallback polling
    }

    init {
        startPolling()
    }

    /** Polling: har 30s da fon yangilanish (SSE ishlamasa ham ishlaydi). */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = screenModelScope.launch {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                load()
            }
        }
    }

    /**
     * SSE eventi kelganda chaqiriladi.
     * Debounce: 3s ichida bir marta yangilanadi.
     */
    fun onRealtimeRefreshTick() {
        screenModelScope.launch {
            val now = System.currentTimeMillis()
            if (now - lastRefreshMs < DEBOUNCE_MS) return@launch
            lastRefreshMs = now
            // Agar ruxsatlar hali yuklanmagan bo'lsa, ularni ham yuklash
            if (_uiState.value.availablePermissions.isEmpty()) loadPermissions()
            load()
        }
    }

    fun load() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val token = AuthSession.accessToken
            if (token.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Sessiya topilmadi") }
                return@launch
            }

            // ── 1. Backend rollar (PRIMARY manba) ──────────────────────────────
            var err: String? = null
            val localByCode = RoleCatalog.baseDefinitions().associateBy { it.code }

            var defs: List<RoleDefinition> = when (val rolesRes = repository.listRoles(token)) {
                is AuthApiResult.Ok -> {
                    rolesRes.value.map { apiRole ->
                        // Local catalog faqat fallback: icon, displayName, permissions (agar backend yo'q bo'lsa)
                        val local = localByCode[apiRole.name]
                        RoleDefinition(
                            id          = apiRole.id,
                            name        = apiRole.displayName?.takeIf { it.isNotBlank() }
                                              ?: local?.name ?: apiRole.name,
                            code        = apiRole.name,
                            description = apiRole.description?.takeIf { it.isNotBlank() }
                                              ?: local?.description ?: "",
                            permissions = apiRole.permissions.ifEmpty {
                                local?.permissions ?: emptyList()
                            },
                            userCount   = 0,
                            isSystemRole = apiRole.isSystem,
                            fullWidth   = local?.fullWidth ?: false,
                        )
                    }
                }
                is AuthApiResult.Err -> {
                    err = rolesRes.message
                    RoleCatalog.baseDefinitions()   // fallback
                }
            }

            // ── 2. Foydalanuvchilar (user count va stats uchun) ────────────────
            when (val usersRes = repository.listUsersPage(token, page = 0, size = 200)) {
                is AuthApiResult.Ok -> {
                    val allUsers = usersRes.value.items.map { item ->
                        val u = item.toAdminUser()
                        // rawRoleNames — backenddan kelgan asl kodlar (ROLE_MANAGER kabi custom uchun ham)
                        val roleCode = u.rawRoleNames.firstOrNull()
                            ?: u.roles.firstOrNull()?.let { userRoleToBackendName(it) }
                            ?: "ROLE_USER"
                        RoleUserRow(
                            id       = u.id,
                            fullName = u.fullName,
                            email    = u.email,
                            role     = roleCode,
                            provider = u.authProvider,
                            status   = u.status,
                        )
                    }
                    // Har bir rol kodi bo'yicha user soni (custom rollar ham hisoblanadi)
                    val counts = allUsers.groupingBy { it.role }.eachCount()
                    val rolesWithCounts = defs.map { def ->
                        def.copy(userCount = counts[def.code] ?: 0)
                    }
                    val stats = RoleScreenStats(
                        totalRoles   = rolesWithCounts.size,
                        totalUsers   = allUsers.size,
                        activeUsers  = allUsers.count { it.status == UserStatus.ACTIVE },
                        blockedUsers = allUsers.count { it.status == UserStatus.BLOCKED },
                    )
                    _uiState.update {
                        it.copy(
                            roles     = rolesWithCounts,
                            users     = allUsers,
                            stats     = stats,
                            isLoading = false,
                            error     = err,
                        )
                    }
                    applyTabFilter()
                }
                is AuthApiResult.Err -> {
                    if (err == null) err = usersRes.message
                    _uiState.update {
                        it.copy(roles = defs, users = emptyList(), isLoading = false, error = err)
                    }
                    applyTabFilter()
                }
            }
        }
    }

    /** null = Hammasi (ALL), "ROLE_ADMIN" = aniq rol kodi fil'tri */
    fun setTab(filter: String?) {
        _uiState.update { it.copy(tabFilter = filter) }
        applyTabFilter()
    }

    private fun applyTabFilter() {
        val s = _uiState.value
        var list = s.users
        // null = hammasi, aks holda rol kodi bo'yicha to'g'ridan-to'g'ri filtr
        s.tabFilter?.let { code -> list = list.filter { it.role == code } }
        list = list.sortedByDescending { it.id }
        _uiState.update { it.copy(filteredUsers = list) }
    }

    /** null = Hammasi; string = aniq rol kodi ("ROLE_ADMIN", "ROLE_MANAGER", ...) */
    fun tabCount(filter: String?): Int {
        val u = _uiState.value.users
        return if (filter == null) u.size else u.count { it.role == filter }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create role
    // ─────────────────────────────────────────────────────────────────────────

    /** Backend `/api/admin/roles/permissions` dan barcha permissionlarni yuklaydi. */
    fun loadPermissions() {
        screenModelScope.launch {
            val token = AuthSession.accessToken
            if (token.isNullOrBlank()) {
                return@launch
            }
            when (val r = repository.listPermissions(token)) {
                is AuthApiResult.Ok -> {
                    val total = r.value.sumOf { it.permissions.size }
                    _uiState.update { it.copy(availablePermissions = r.value) }
                }
                is AuthApiResult.Err -> {
                }
            }
        }
    }

    /**
     * Yangi rol yaratish.
     * @param form  Dialog'dan kelgan forma ma'lumotlari.
     * @param onSuccess Dialog yopilsin.
     * @param onError   Dialog ochiq qolsin, xato ko'rsatilsin.
     */
    fun createRole(
        form: CreateRoleForm,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        screenModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            val token = AuthSession.accessToken ?: run {
                _uiState.update { it.copy(isCreating = false) }
                onError("Sessiya topilmadi")
                return@launch
            }
            when (val r = repository.createRole(
                token          = token,
                name           = form.code.trim(),          // backend "name" = ROLE_... kodi
                displayName    = form.name.trim(),          // ko'rsatiladigan nom
                description    = form.description.trim().takeIf { it.isNotBlank() },
                permissionNames = form.permissions,
            )) {
                is AuthApiResult.Ok -> {
                    _uiState.update { it.copy(isCreating = false, createError = null) }
                    load()        // ro'yxatni yangilaymiz
                    onSuccess()
                }
                is AuthApiResult.Err -> {
                    _uiState.update { it.copy(isCreating = false, createError = r.message) }
                    onError(r.message)
                }
            }
        }
    }

    fun clearCreateError() {
        _uiState.update { it.copy(createError = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit role
    // ─────────────────────────────────────────────────────────────────────────

    fun startEditRole(role: RoleDefinition) {
        _uiState.update { it.copy(editingRole = role, updateError = null) }
    }

    fun clearEditRole() {
        _uiState.update { it.copy(editingRole = null, updateError = null) }
    }

    fun updateRole(
        roleId: String,
        roleCode: String,
        form: UpdateRoleForm,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        screenModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateError = null) }
            val token = AuthSession.accessToken ?: run {
                _uiState.update { it.copy(isUpdating = false) }
                onError("Sessiya topilmadi")
                return@launch
            }
            when (val r = repository.updateRole(
                token           = token,
                roleId          = roleId,
                roleCode        = roleCode,
                displayName     = form.name.trim().takeIf { it.isNotBlank() },
                description     = form.description.trim().takeIf { it.isNotBlank() },
                permissionNames = form.permissions,
            )) {
                is AuthApiResult.Ok  -> {
                    _uiState.update { it.copy(isUpdating = false, updateError = null, editingRole = null) }
                    load()
                    onSuccess()
                }
                is AuthApiResult.Err -> {
                    _uiState.update { it.copy(isUpdating = false, updateError = r.message) }
                    onError(r.message)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete role
    // ─────────────────────────────────────────────────────────────────────────

    fun startDeleteRole(role: RoleDefinition) {
        _uiState.update { it.copy(deletingRole = role, deleteError = null) }
    }

    fun clearDeleteRole() {
        _uiState.update { it.copy(deletingRole = null, deleteError = null) }
    }

    fun deleteRole(
        roleId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        screenModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            val token = AuthSession.accessToken ?: run {
                _uiState.update { it.copy(isDeleting = false) }
                onError("Sessiya topilmadi")
                return@launch
            }
            when (val r = repository.deleteRole(token, roleId)) {
                is AuthApiResult.Ok  -> {
                    _uiState.update { it.copy(isDeleting = false, deleteError = null, deletingRole = null) }
                    load()
                    onSuccess()
                }
                is AuthApiResult.Err -> {
                    _uiState.update { it.copy(isDeleting = false, deleteError = r.message) }
                    onError(r.message)
                }
            }
        }
    }
}

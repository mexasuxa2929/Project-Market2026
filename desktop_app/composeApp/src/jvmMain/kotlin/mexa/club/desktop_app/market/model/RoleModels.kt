package mexa.club.desktop_app.market.model

data class RoleDefinition(
    val id: String,
    val name: String,
    val code: String,
    val description: String,
    val permissions: List<String>,
    val userCount: Int,
    val isSystemRole: Boolean,
    val fullWidth: Boolean = false,
)

data class RoleUserRow(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String,        // raw backend role code: "ROLE_ADMIN", "ROLE_MANAGER", etc.
    val provider: AuthProvider,
    val status: UserStatus,
)

data class RoleScreenStats(
    val totalRoles: Int,
    val totalUsers: Int,
    val activeUsers: Int,
    val blockedUsers: Int,
)

// RoleTabFilter endi String? sifatida ishlatiladi:
// null     = "Hammasi" (ALL)
// "ROLE_ADMIN", "ROLE_MANAGER", ...  = aniq rol kodi (DB dan dinamik)

data class CreateRoleForm(
    val name: String,
    val code: String,
    val permissions: Set<String>,
    val description: String,
)

data class UpdateRoleForm(
    val name: String,           // displayName ko'rsatish uchun
    val description: String,
    val permissions: Set<String>,
)

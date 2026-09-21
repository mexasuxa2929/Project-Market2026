package mexa.club.desktop_app.market.model

data class AdminUser(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val roles: List<UserRole>,
    /** Backenddan kelgan asl rol kodlari (masalan: ["ROLE_MANAGER", "ROLE_ADMIN"]).
     *  Custom rollar uchun `UserRole` enum etarli emas, shu yerda saqlanadi. */
    val rawRoleNames: List<String> = emptyList(),
    val status: UserStatus,
    val isVerified: Boolean,
    val authProvider: AuthProvider,
    val verifiedLabel: String,
    val lastActivity: String,
    /** Ro'yxatdan o'tgan vaqti (ISO, backend `createdAt`). */
    val createdAt: String? = null,
    /** Eng so'nggi faol sessiya vaqti (ISO). */
    val lastActiveAt: String? = null,
    /** Hozir onlaynmi (faol sessiyasi so'nggi 15 daqiqada ishlatilgan). */
    val online: Boolean = false,
    val blockReason: String? = null,
    val blockedAt: String? = null,
)

enum class UserRole { SUPER_ADMIN, ADMIN, WAREHOUSE, COURIER, USER }

enum class UserStatus { ACTIVE, BLOCKED }

enum class AuthProvider { LOCAL, GOOGLE }

data class CreateUserForm(
    val username: String,
    val email: String,
    val password: String,
    val roles: Set<UserRole>,
    val isActive: Boolean,
)

fun backendRoleToUserRole(name: String): UserRole? = when (name) {
    "ROLE_SUPER_ADMIN" -> UserRole.SUPER_ADMIN
    "ROLE_ADMIN" -> UserRole.ADMIN
    "ROLE_WAREHOUSE" -> UserRole.WAREHOUSE
    "ROLE_COURIER" -> UserRole.COURIER
    "ROLE_USER" -> UserRole.USER
    else -> null
}

fun userRoleToBackendName(role: UserRole): String = when (role) {
    UserRole.SUPER_ADMIN -> "ROLE_SUPER_ADMIN"
    UserRole.ADMIN -> "ROLE_ADMIN"
    UserRole.WAREHOUSE -> "ROLE_WAREHOUSE"
    UserRole.COURIER -> "ROLE_COURIER"
    UserRole.USER -> "ROLE_USER"
}

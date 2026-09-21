package mexa.club.desktop_app.market.data

import mexa.club.desktop_app.auth.AdminRoleItem
import mexa.club.desktop_app.auth.AdminUsersPageResult
import mexa.club.desktop_app.auth.AuthApiResult

interface UsersRepository {
    suspend fun listRoles(token: String): AuthApiResult<List<AdminRoleItem>>
    suspend fun listUsersPage(
        token: String,
        page: Int,
        size: Int,
        keyword: String? = null,
        role: String? = null,
        enabled: Boolean? = null,
    ): AuthApiResult<AdminUsersPageResult>
    suspend fun createUser(
        token: String,
        username: String,
        email: String,
        password: String,
        roleIds: List<String>,
        enabled: Boolean,
    ): AuthApiResult<Unit>

    suspend fun deleteUser(token: String, id: String): AuthApiResult<Unit>

    suspend fun setUserRoles(token: String, userId: String, roleIds: List<String>): AuthApiResult<Unit>

    suspend fun blockUser(token: String, userId: String, reason: String?): AuthApiResult<Unit>

    suspend fun unblockUser(token: String, userId: String): AuthApiResult<Unit>
}

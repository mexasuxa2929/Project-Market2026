package mexa.club.desktop_app.market.data

import mexa.club.desktop_app.auth.AdminRoleItem
import mexa.club.desktop_app.auth.AdminUsersPageResult
import mexa.club.desktop_app.auth.AuthApiResult
import mexa.club.desktop_app.auth.PermissionGroup

interface RolesRepository {
    suspend fun listRoles(token: String): AuthApiResult<List<AdminRoleItem>>
    suspend fun listUsersPage(token: String, page: Int, size: Int): AuthApiResult<AdminUsersPageResult>
    suspend fun createRole(
        token: String,
        name: String,
        displayName: String?,
        description: String?,
        permissionNames: Set<String>,
    ): AuthApiResult<AdminRoleItem>
    suspend fun updateRole(
        token: String,
        roleId: String,
        roleCode: String,
        displayName: String?,
        description: String?,
        permissionNames: Set<String>,
    ): AuthApiResult<AdminRoleItem>
    suspend fun deleteRole(token: String, roleId: String): AuthApiResult<Unit>
    suspend fun listPermissions(token: String): AuthApiResult<List<PermissionGroup>>
}

package mexa.club.desktop_app.market.data

import mexa.club.desktop_app.auth.AuthApiClient

class RolesRepositoryImpl(
    private val api: AuthApiClient,
) : RolesRepository {

    override suspend fun listRoles(token: String) = api.listRoles(token)

    override suspend fun listUsersPage(token: String, page: Int, size: Int) =
        api.listAdminUsersPage(token, page, size)

    override suspend fun createRole(
        token: String,
        name: String,
        displayName: String?,
        description: String?,
        permissionNames: Set<String>,
    ) = api.createRole(token, name, displayName, description, permissionNames)

    override suspend fun updateRole(
        token: String,
        roleId: String,
        roleCode: String,
        displayName: String?,
        description: String?,
        permissionNames: Set<String>,
    ) = api.updateRole(token, roleId, roleCode, displayName, description, permissionNames)

    override suspend fun deleteRole(token: String, roleId: String) =
        api.deleteRole(token, roleId)

    override suspend fun listPermissions(token: String) = api.listPermissions(token)
}

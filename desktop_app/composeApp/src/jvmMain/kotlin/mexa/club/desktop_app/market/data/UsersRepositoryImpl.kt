package mexa.club.desktop_app.market.data

import mexa.club.desktop_app.auth.AuthApiClient

class UsersRepositoryImpl(
    private val api: AuthApiClient,
) : UsersRepository {

    override suspend fun listRoles(token: String) = api.listRoles(token)

    override suspend fun listUsersPage(
        token: String,
        page: Int,
        size: Int,
        keyword: String?,
        role: String?,
        enabled: Boolean?,
    ) = api.listAdminUsersPage(token, page, size, keyword, role, enabled)

    override suspend fun createUser(
        token: String,
        username: String,
        email: String,
        password: String,
        roleIds: List<String>,
        enabled: Boolean,
    ) = api.createAdminUser(token, username, email, password, roleIds, enabled)

    override suspend fun deleteUser(token: String, id: String) =
        api.deleteAdminUser(token, id)

    override suspend fun setUserRoles(token: String, userId: String, roleIds: List<String>) =
        api.setUserRoles(token, userId, roleIds)

    override suspend fun blockUser(token: String, userId: String, reason: String?) =
        api.blockUser(token, userId, reason)

    override suspend fun unblockUser(token: String, userId: String) =
        api.unblockUser(token, userId)
}

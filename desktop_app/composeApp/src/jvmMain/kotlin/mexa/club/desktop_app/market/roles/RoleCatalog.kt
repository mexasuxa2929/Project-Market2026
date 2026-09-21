package mexa.club.desktop_app.market.roles

import mexa.club.desktop_app.auth.AdminRoleItem
import mexa.club.desktop_app.market.model.RoleDefinition
import mexa.club.desktop_app.market.model.UserRole

object RoleCatalog {

    fun baseDefinitions(): List<RoleDefinition> = listOf(
        RoleDefinition(
            id = "system-super-admin",
            name = "Super Admin",
            code = "ROLE_SUPER_ADMIN",
            description = "Barcha huquqlarga ega tizim boshqaruvchisi. O'chirib bo'lmaydi.",
            permissions = listOf(
                "Barcha sahifalar",
                "Foydalanuvchi boshqaruvi",
                "Moliyaviy hisobotlar",
                "Tizim sozlamalari",
                "Audit log ko'rish",
                "Rollar belgilash",
            ),
            userCount = 0,
            isSystemRole = true,
        ),
        RoleDefinition(
            id = "system-admin",
            name = "Admin",
            code = "ROLE_ADMIN",
            description = "Kontent va buyurtmalarni boshqaruvchi administrator.",
            permissions = listOf(
                "Mahsulotlar boshqaruvi",
                "Buyurtmalar ko'rish",
                "Bildirishnomalar",
                "Hisobotlar",
            ),
            userCount = 0,
            isSystemRole = true,
        ),
        RoleDefinition(
            id = "system-warehouse",
            name = "Ombor Boshqaruvchi",
            code = "ROLE_WAREHOUSE",
            description = "Ombor va zaxiralarni boshqaruvchi xodim.",
            permissions = listOf(
                "Omborlar boshqaruvi",
                "Stok ko'rish",
                "Yetkazib olish",
                "Audit log",
            ),
            userCount = 0,
            isSystemRole = true,
        ),
        RoleDefinition(
            id = "system-courier",
            name = "Kuryer",
            code = "ROLE_COURIER",
            description = "Yetkazib berish xizmati xodimi.",
            permissions = listOf(
                "O'z yetkazishlarini ko'rish",
                "Holat yangilash",
                "Tracking kod",
            ),
            userCount = 0,
            isSystemRole = true,
        ),
        RoleDefinition(
            id = "system-user",
            name = "Foydalanuvchi (Xaridor)",
            code = "ROLE_USER",
            description = "Ro'yxatdan o'tganda avtomatik beriladigan standart rol. Sotib oluvchi.",
            permissions = emptyList(),
            userCount = 0,
            isSystemRole = true,
            fullWidth = false,
        ),
    )

    /**
     * Server javobidan [AdminRoleItem] ro'yxatini olib, lokal [definitions] ga birlashtiradi:
     * - `id`         → serverdan (UUID kerak bo'lganda to'g'ri bo'lsin)
     * - `name`       → serverda `displayName` bo'lsa undan, aks holda lokal nom
     * - `description` → serverda bo'lsa undan, aks holda lokal tavsif
     * - `isSystemRole` → server `isSystem` yoki lokal qiymat
     */
    fun mergeWithApiData(
        definitions: List<RoleDefinition>,
        apiRoles: List<AdminRoleItem>,
    ): List<RoleDefinition> {
        val byCode = apiRoles.associateBy { it.name }
        return definitions.map { def ->
            val api = byCode[def.code]
            def.copy(
                id          = api?.id ?: def.id,
                name        = api?.displayName?.takeIf { it.isNotBlank() } ?: def.name,
                description = api?.description?.takeIf { it.isNotBlank() } ?: def.description,
                isSystemRole = api?.isSystem ?: def.isSystemRole,
                // Backend permissions bo'lsa ularni ishlatamiz, aks holda local catalog
                permissions = api?.permissions?.takeIf { it.isNotEmpty() } ?: def.permissions,
            )
        }
    }

    /** Orqaga moslik uchun saqlanadi — faqat id birlashtiradi. */
    @Deprecated("mergeWithApiData ishlatish tavsiya etiladi", ReplaceWith("mergeWithApiData(definitions, apiRoles.map { AdminRoleItem(it.first, it.second) })"))
    fun mergeApiRoleIds(
        definitions: List<RoleDefinition>,
        apiRoles: List<Pair<String, String>>,
    ): List<RoleDefinition> {
        val byCode = apiRoles.associate { (id, name) -> name to id }
        return definitions.map { def ->
            val apiId = byCode[def.code] ?: def.id
            def.copy(id = apiId)
        }
    }

    fun userRoleFromCode(code: String): UserRole? = when (code) {
        "ROLE_SUPER_ADMIN" -> UserRole.SUPER_ADMIN
        "ROLE_ADMIN" -> UserRole.ADMIN
        "ROLE_WAREHOUSE", "ROLE_WAREHOUSE_MANAGER" -> UserRole.WAREHOUSE
        "ROLE_COURIER" -> UserRole.COURIER
        "ROLE_USER" -> UserRole.USER
        else -> null
    }

    val permissionGroups: List<Pair<String, List<String>>> = listOf(
        "Ombor bo'limi" to listOf("Omborlar boshqaruvi", "Stok ko'rish", "Yetkazib olish", "Audit log"),
        "Mahsulotlar bo'limi" to listOf("Mahsulotlar boshqaruvi", "Buyurtmalar ko'rish"),
        "Buyurtmalar bo'limi" to listOf("Buyurtmalar ko'rish", "Bildirishnomalar"),
    )
}

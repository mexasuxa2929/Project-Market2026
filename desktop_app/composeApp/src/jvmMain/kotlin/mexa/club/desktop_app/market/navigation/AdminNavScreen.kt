package mexa.club.desktop_app.market.navigation

/**
 * Sidebar bo‘yicha ekranlar (HTML mockup bilan mos).
 * [superAdminOnly] = true bo‘lgan ekranlar faqat SUPER_ADMIN ko‘ra oladi;
 * ROLE_ADMIN (warehouse admin) faqat o‘z omborini boshqaradigan sahifalarni ko‘radi.
 */
sealed class AdminNavScreen {
    abstract val superAdminOnly: Boolean

    data object Dashboard : AdminNavScreen() { override val superAdminOnly = false }
    data object Users : AdminNavScreen() { override val superAdminOnly = true }
    data object Roles : AdminNavScreen() { override val superAdminOnly = true }
    data object Warehouses : AdminNavScreen() { override val superAdminOnly = false }
    data object Products : AdminNavScreen() { override val superAdminOnly = false }
    data object Orders : AdminNavScreen() { override val superAdminOnly = true }
    data object Delivery : AdminNavScreen() { override val superAdminOnly = true }
    data object Couriers : AdminNavScreen() { override val superAdminOnly = true }
    data object Notifications : AdminNavScreen() { override val superAdminOnly = false }
    data object Templates : AdminNavScreen() { override val superAdminOnly = true }
    data object Reports : AdminNavScreen() { override val superAdminOnly = true }
    data object AuditLog : AdminNavScreen() { override val superAdminOnly = true }
    data object Reviews : AdminNavScreen() { override val superAdminOnly = true }
    data object Map : AdminNavScreen() { override val superAdminOnly = true }
    data object Settings : AdminNavScreen() { override val superAdminOnly = false }
}

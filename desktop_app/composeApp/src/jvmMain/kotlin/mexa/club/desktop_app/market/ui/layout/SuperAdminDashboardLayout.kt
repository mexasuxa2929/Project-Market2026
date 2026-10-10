package mexa.club.desktop_app.market.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.navigation.AdminNavScreen
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.market.ui.components.AdminSidebar
import mexa.club.desktop_app.market.ui.components.AdminSidebarItem
import mexa.club.desktop_app.market.ui.components.SuperAdminTopBar
import mexa.club.desktop_app.market.ui.roles.RolesScreen
import mexa.club.desktop_app.market.ui.products.ProductsScreen
import mexa.club.desktop_app.market.ui.orders.OrdersScreen
import mexa.club.desktop_app.market.ui.analytics.AnalyticsScreen
import mexa.club.desktop_app.market.ui.promotions.PromotionsScreen
import mexa.club.desktop_app.market.ui.groups.GroupsScreen
import mexa.club.desktop_app.market.ui.search.SearchOpsScreen
import mexa.club.desktop_app.market.ui.delivery.DeliveryPricingScreen
import mexa.club.desktop_app.market.ui.delivery.DeliveryScreen
import mexa.club.desktop_app.market.ui.couriers.CouriersScreen
import mexa.club.desktop_app.market.ui.notifications.NotificationsScreen
import mexa.club.desktop_app.market.ui.templates.TemplatesScreen
import mexa.club.desktop_app.market.ui.reports.ReportsScreen
import mexa.club.desktop_app.market.ui.auditlog.AuditLogScreen
import mexa.club.desktop_app.market.ui.settings.SettingsScreen
import mexa.club.desktop_app.market.ui.reviews.ReviewsScreen
import mexa.club.desktop_app.market.ui.screens.SuperAdminDashboardScreen
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.users.UsersScreen
import mexa.club.desktop_app.market.ui.warehouses.WarehousesScreen
import mexa.club.desktop_app.market.ui.maps.MarketMapScreen

private fun userDisplayName(raw: String?): String {
    val u = raw?.trim().orEmpty()
    if (u.isEmpty()) return "Super Admin"
    return if (u.length > 24) u.take(24) + "…" else u
}

private fun avatarInitials(raw: String?): String {
    val u = raw?.trim().orEmpty()
    if (u.isEmpty()) return "SA"
    val parts = u.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 ->
            "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"

        u.length >= 2 -> u.take(2).uppercase(Locale.getDefault())
        else -> "${u.first().uppercaseChar()}A"
    }
}

@Composable
fun SuperAdminDashboardLayout(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    var current by remember { mutableStateOf<AdminNavScreen>(AdminNavScreen.Dashboard) }
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    // T-H1: global qidiruv → (bo'lim kaliti, so'rov); tanlangan ekranga initialQuery sifatida uzatiladi
    var pendingSearch by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isSuperAdmin = AuthSession.isSuperAdmin

    // Global qidiruv bo'limlari (superadmin bo'lmaganlar uchun faqat ruxsat etilganlari)
    val searchScreenByKey = remember(isSuperAdmin) {
        mapOf(
            "orders" to AdminNavScreen.Orders,
            "products" to AdminNavScreen.Products,
            "users" to AdminNavScreen.Users,
            "warehouses" to AdminNavScreen.Warehouses,
            "notifications" to AdminNavScreen.Notifications,
            "auditlog" to AdminNavScreen.AuditLog,
        ).filterValues { isSuperAdmin || !it.superAdminOnly }
    }
    val allSearchTargets = AppStrings.globalSearchTargets(appLanguage())
    val searchTargets = remember(allSearchTargets, searchScreenByKey) {
        allSearchTargets.filterKeys { it in searchScreenByKey }
    }
    fun searchQueryFor(key: String): String =
        if (pendingSearch?.first == key) pendingSearch?.second.orEmpty() else ""

    val displayName = userDisplayName(
        SessionManager.currentUser?.username ?: AuthSession.username,
    )
    val initials = avatarInitials(
        SessionManager.currentUser?.username ?: AuthSession.username,
    )

    // O-45: items barqiya har recompositionda yangi List — remember bilan barqaror.
    // S-48: sidebar matnlari AppStrings orqali (hardcoded emas).
    val sidebarLang = appLanguage()
    val items = remember(isSuperAdmin, sidebarLang) {
        listOf(
            AdminSidebarItem(AppStrings.t(sidebarLang, "Dashboard"), AdminNavScreen.Dashboard, Icons.Filled.Dashboard),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Users"), AdminNavScreen.Users, Icons.Filled.SupervisorAccount),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Roles"), AdminNavScreen.Roles, Icons.Filled.Security),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Warehouses"), AdminNavScreen.Warehouses, Icons.Filled.Warehouse),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Products"), AdminNavScreen.Products, Icons.Filled.Inventory2),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Orders"), AdminNavScreen.Orders, Icons.Filled.ShoppingCart),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Deliveries"), AdminNavScreen.Deliveries, Icons.Filled.LocalShipping),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Couriers"), AdminNavScreen.Couriers, Icons.Filled.DeliveryDining),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Delivery Pricing"), AdminNavScreen.Delivery, Icons.Filled.PriceChange),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Notifications"), AdminNavScreen.Notifications, Icons.Filled.Notifications),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Templates"), AdminNavScreen.Templates, Icons.Filled.Description),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Reports"), AdminNavScreen.Reports, Icons.Filled.Assessment),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Analytics"), AdminNavScreen.Analytics, Icons.Filled.BarChart),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Promotions"), AdminNavScreen.Promotions, Icons.Filled.LocalOffer),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Groups"), AdminNavScreen.Groups, Icons.Filled.GroupWork),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Search"), AdminNavScreen.SearchOps, Icons.Filled.Search),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Audit Log"), AdminNavScreen.AuditLog, Icons.Filled.History),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Sharhlar"), AdminNavScreen.Reviews, Icons.Filled.RateReview),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Xarita"), AdminNavScreen.Map, Icons.Filled.Map),
            AdminSidebarItem(AppStrings.t(sidebarLang, "Settings"), AdminNavScreen.Settings, Icons.Filled.Settings),
        ).filter { isSuperAdmin || !it.screen.superAdminOnly }
    }

    LaunchedEffect(items) {
        if (current.superAdminOnly && !isSuperAdmin) {
            current = AdminNavScreen.Dashboard
        }
    }

    Row(modifier.fillMaxSize()) {
            AdminSidebar(
                modifier = Modifier.width(240.dp),
                items = items,
                selected = current,
                onSelect = { current = it; pendingSearch = null },
                title = "MEXA WAREHOUSE",
                subtitle = AppStrings.t(
                    appLanguage(),
                    if (isSuperAdmin) "SUPER ADMIN PANEL" else "WAREHOUSE ADMIN PANEL",
                ),
                onLogout = { showLogoutDialog = true },
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SuperAdminTopBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    searchTargets = searchTargets,
                    onSearchNavigate = { key, query ->
                        searchScreenByKey[key]?.let { current = it }
                        pendingSearch = key to query.trim()
                    },
                    displayName = displayName,
                    subtitle = "Admin Panel",
                    avatarInitials = initials,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MexaWarehouseColors.backgroundPage)
                        // Xarita brauzeri chegaralarga yopishishi uchun — padding'siz to'liq egallaydi.
                        .then(if (current == AdminNavScreen.Map) Modifier else Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
                ) {
                    // O-45: sahifa holati saqlanadi — Orders 5-sahifadan chiqib
                    // qaytsa state yo'qolmaydi (SaveableStateHolder + kalit).
                    val saveableStateHolder = rememberSaveableStateHolder()
                    saveableStateHolder.SaveableStateProvider(key = current.toString()) {
                    when (current) {
                        AdminNavScreen.Dashboard     -> SuperAdminDashboardScreen()
                        AdminNavScreen.Users         -> UsersScreen(initialQuery = searchQueryFor("users"))
                        AdminNavScreen.Roles         -> RolesScreen()
                        AdminNavScreen.Warehouses    -> WarehousesScreen(initialQuery = searchQueryFor("warehouses"))
                        AdminNavScreen.Products      -> ProductsScreen(initialQuery = searchQueryFor("products"))
                        AdminNavScreen.Orders        -> OrdersScreen(initialQuery = searchQueryFor("orders"))
                        AdminNavScreen.Deliveries    -> DeliveryScreen()
                        AdminNavScreen.Couriers      -> CouriersScreen()
                        AdminNavScreen.Delivery      -> DeliveryPricingScreen()
                        AdminNavScreen.Notifications -> NotificationsScreen(initialQuery = searchQueryFor("notifications"))
                        AdminNavScreen.Templates     -> TemplatesScreen()
                        AdminNavScreen.Reports       -> ReportsScreen()
                        AdminNavScreen.Analytics     -> AnalyticsScreen()
                        AdminNavScreen.Promotions    -> PromotionsScreen()
                        AdminNavScreen.Groups        -> GroupsScreen()
                        AdminNavScreen.SearchOps     -> SearchOpsScreen()
                        AdminNavScreen.AuditLog      -> AuditLogScreen(initialQuery = searchQueryFor("auditlog"))
                        AdminNavScreen.Reviews       -> ReviewsScreen()
                        AdminNavScreen.Map           -> MarketMapScreen()
                        AdminNavScreen.Settings      -> SettingsScreen()
                    }
                    }
                }
            }
        }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MexaWarehouseColors.danger)
            },
            title = { Text(AppStrings.logoutDialogTitle(appLanguage())) },
            text = {
                Text(
                    AppStrings.logoutDialogMessage(appLanguage()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MexaWarehouseColors.textMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text(AppStrings.logout(appLanguage()), color = MexaWarehouseColors.danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(AppStrings.cancel(appLanguage()), color = MexaWarehouseColors.textMuted)
                }
            },
        )
    }
}

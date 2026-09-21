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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.market.navigation.AdminNavScreen
import mexa.club.desktop_app.market.session.SessionManager
import mexa.club.desktop_app.market.ui.components.AdminSidebar
import mexa.club.desktop_app.market.ui.components.AdminSidebarItem
import mexa.club.desktop_app.market.ui.components.SuperAdminTopBar
import mexa.club.desktop_app.market.ui.roles.RolesScreen
import mexa.club.desktop_app.market.ui.products.ProductsScreen
import mexa.club.desktop_app.market.ui.orders.OrdersScreen
import mexa.club.desktop_app.market.ui.couriers.CouriersScreen
import mexa.club.desktop_app.market.ui.delivery.DeliveryScreen
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

    val isSuperAdmin = AuthSession.isSuperAdmin

    val displayName = userDisplayName(
        SessionManager.currentUser?.username ?: AuthSession.username,
    )
    val initials = avatarInitials(
        SessionManager.currentUser?.username ?: AuthSession.username,
    )

    val allItems = listOf(
        AdminSidebarItem("Dashboard", AdminNavScreen.Dashboard, Icons.Filled.Dashboard),
        AdminSidebarItem("Users", AdminNavScreen.Users, Icons.Filled.SupervisorAccount),
        AdminSidebarItem("Roles", AdminNavScreen.Roles, Icons.Filled.Security),
        AdminSidebarItem("Warehouses", AdminNavScreen.Warehouses, Icons.Filled.Warehouse),
        AdminSidebarItem("Products", AdminNavScreen.Products, Icons.Filled.Inventory2),
        AdminSidebarItem("Orders", AdminNavScreen.Orders, Icons.Filled.ShoppingCart),
        AdminSidebarItem("Delivery", AdminNavScreen.Delivery, Icons.Filled.LocalShipping),
        AdminSidebarItem("Couriers", AdminNavScreen.Couriers, Icons.Filled.DeliveryDining),
        AdminSidebarItem("Notifications", AdminNavScreen.Notifications, Icons.Filled.Notifications),
        AdminSidebarItem("Templates", AdminNavScreen.Templates, Icons.Filled.Description),
        AdminSidebarItem("Reports", AdminNavScreen.Reports, Icons.Filled.Assessment),
        AdminSidebarItem("Audit Log", AdminNavScreen.AuditLog, Icons.Filled.History),
        AdminSidebarItem("Sharhlar", AdminNavScreen.Reviews, Icons.Filled.RateReview),
        AdminSidebarItem("Xarita", AdminNavScreen.Map, Icons.Filled.Map),
        AdminSidebarItem("Settings", AdminNavScreen.Settings, Icons.Filled.Settings),
    )

    val items = allItems.filter { isSuperAdmin || !it.screen.superAdminOnly }

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
                onSelect = { current = it },
                title = "MEXA WAREHOUSE",
                subtitle = if (isSuperAdmin) "SUPER ADMIN PANEL" else "WAREHOUSE ADMIN PANEL",
                onLogout = { showLogoutDialog = true },
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SuperAdminTopBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
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
                    when (current) {
                        AdminNavScreen.Dashboard     -> SuperAdminDashboardScreen()
                        AdminNavScreen.Users         -> UsersScreen()
                        AdminNavScreen.Roles         -> RolesScreen()
                        AdminNavScreen.Warehouses    -> WarehousesScreen()
                        AdminNavScreen.Products      -> ProductsScreen()
                        AdminNavScreen.Orders        -> OrdersScreen()
                        AdminNavScreen.Delivery      -> DeliveryScreen()
                        AdminNavScreen.Couriers      -> CouriersScreen()
                        AdminNavScreen.Notifications -> NotificationsScreen()
                        AdminNavScreen.Templates     -> TemplatesScreen()
                        AdminNavScreen.Reports       -> ReportsScreen()
                        AdminNavScreen.AuditLog      -> AuditLogScreen()
                        AdminNavScreen.Reviews       -> ReviewsScreen()
                        AdminNavScreen.Map           -> MarketMapScreen()
                        AdminNavScreen.Settings      -> SettingsScreen()
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
            title = { Text("Chiqishni tasdiqlash") },
            text = {
                Text(
                    "Tizimdan chiqmoqchimisiz?",
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
                    Text("Chiqish", color = MexaWarehouseColors.danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Bekor qilish", color = MexaWarehouseColors.textMuted)
                }
            },
        )
    }
}

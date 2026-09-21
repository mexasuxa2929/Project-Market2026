package com.example.mobile_app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mobile_app.navigation.Screen
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.util.tr

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun navItems() = listOf(
    BottomNavItem(tr("nav_home"), Screen.Home.route, Icons.Default.Home, Icons.Default.Home),
    BottomNavItem(tr("nav_categories"), Screen.Catalog.route, Icons.Filled.Apps, Icons.Filled.Apps),
    BottomNavItem(tr("nav_cart"), Screen.Cart.route, Icons.Default.ShoppingCart, Icons.Default.ShoppingCart),
    BottomNavItem(tr("nav_orders"), Screen.Orders.route, Icons.Filled.List, Icons.Filled.List),
    BottomNavItem(tr("nav_profile"), Screen.Profile.route, Icons.Default.Person, Icons.Default.Person)
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        navItems().forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = Primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

package com.example.mobile_app.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object VerifyEmail : Screen("verify_email")
    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object CategoryProducts : Screen("category_products/{categoryId}") {
        fun route(categoryId: String) = "category_products/$categoryId"
    }
    data object Cart : Screen("cart")
    data object Orders : Screen("orders")
    data object Profile : Screen("profile")
    data object ProductDetail : Screen("product/{productId}") {
        fun route(productId: String) = "product/$productId"
    }
    data object OrderDetail : Screen("order_detail/{orderId}") {
        fun route(orderId: String) = "order_detail/$orderId"
    }
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")
    data object Checkout : Screen("checkout")
    data object Favorites : Screen("favorites")
    data object Addresses : Screen("addresses")
    data object MapPicker : Screen("map_picker?lat={lat}&lng={lng}") {
        fun route(lat: Double? = null, lng: Double? = null): String {
            return if (lat != null && lng != null) "map_picker?lat=$lat&lng=$lng" else "map_picker"
        }
    }
    data object PaymentMethods : Screen("payment_methods")
    data object Settings : Screen("settings")
    data object Help : Screen("help")
    data object ForgotPassword : Screen("forgot_password")
}

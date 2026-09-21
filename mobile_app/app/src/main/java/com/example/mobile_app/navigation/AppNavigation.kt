package com.example.mobile_app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mobile_app.ui.components.BottomNavBar
import com.example.mobile_app.data.local.SessionEvents
import androidx.navigation.navArgument
import com.example.mobile_app.ui.screens.auth.LoginScreen
import com.example.mobile_app.ui.screens.auth.VerifyEmailScreen
import com.example.mobile_app.ui.screens.cart.CartScreen
import com.example.mobile_app.ui.screens.catalog.CatalogScreen
import com.example.mobile_app.ui.screens.category.CategoryProductsScreen
import com.example.mobile_app.ui.screens.home.HomeScreen
import com.example.mobile_app.ui.screens.orders.OrderDetailScreen
import com.example.mobile_app.ui.screens.orders.OrdersScreen
import com.example.mobile_app.ui.screens.product.ProductDetailScreen
import com.example.mobile_app.ui.screens.profile.AddressesScreen
import com.example.mobile_app.ui.screens.map.MapPickerScreen
import com.example.mobile_app.ui.screens.profile.HelpScreen
import com.example.mobile_app.ui.screens.profile.PaymentMethodsScreen
import com.example.mobile_app.ui.screens.profile.ProfileScreen
import com.example.mobile_app.ui.screens.profile.SettingsScreen
import com.example.mobile_app.ui.screens.checkout.CheckoutScreen
import com.example.mobile_app.ui.screens.search.SearchScreen
import com.example.mobile_app.ui.screens.splash.SplashScreen
import com.example.mobile_app.ui.viewmodel.AuthViewModel
import com.example.mobile_app.ui.viewmodel.CartViewModel
import com.example.mobile_app.ui.viewmodel.HomeViewModel
import com.example.mobile_app.ui.viewmodel.OrderViewModel
import com.example.mobile_app.ui.viewmodel.ProductDetailViewModel
import com.example.mobile_app.util.LanguageManager
import com.example.mobile_app.util.localizedName
import com.example.mobile_app.util.tr

// Tezkor animatsiyalar — og'irlik kamaytirish uchun
private const val TRANSITION_DURATION = 150
private val quickFadeIn: EnterTransition = fadeIn(animationSpec = tween(TRANSITION_DURATION))
private val quickFadeOut: ExitTransition = fadeOut(animationSpec = tween(TRANSITION_DURATION))

// Asosiy ekranlarda (bottom nav ko'rinadigan joylar)
private val mainRoutes = setOf(
    Screen.Home.route,
    Screen.Catalog.route,
    Screen.Cart.route,
    Screen.Orders.route,
    Screen.Profile.route
)

@Composable
fun AppNavigation(navController: NavHostController, languageManager: LanguageManager? = null) {
    // Faqat AuthViewModel'ni ildizda yaratamiz — qolganlarni kerakli ekranda
    val authViewModel: AuthViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in mainRoutes

    // Sessiya tugagach (401/403 refresh muvaffaqiyatsiz) avtomatik Login ekraniga qaytish.
    val sessionExpired by SessionEvents.sessionExpired.collectAsState()
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            SessionEvents.reset()
            authViewModel.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = com.example.mobile_app.ui.theme.Background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: Screen.Home.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Splash / Auth / ProductDetail — to'liq ekran (padding berilmaydi),
        // shunda binafsha fon status/nav bar ostigacha tushadi.
        val isSplash = currentRoute == Screen.Splash.route
        val isAuthFullBleed = currentRoute == Screen.Login.route || currentRoute == Screen.VerifyEmail.route || currentRoute == Screen.ForgotPassword.route
        // ProductDetail to'liq edge-to-edge (yuqori va pastdan): oq rasm/panel
        // status-bar va nav-bar ostigacha tushadi, chetlarda boshqa rang qolmaydi.
        val isDetailFullBleed = currentRoute == Screen.ProductDetail.route
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when {
                        isSplash -> Modifier
                        isAuthFullBleed -> Modifier
                        isDetailFullBleed -> Modifier
                        else -> Modifier.padding(innerPadding)
                    }
                )
        ) {
            // Auth — ForgotPassword must be defined before Login to allow navigation
            composable(
                Screen.ForgotPassword.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                com.example.mobile_app.ui.screens.auth.ForgotPasswordScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }
            composable(
                Screen.Splash.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                SplashScreen(
                    onNavigateToLogin = {
                        val destination = if (com.example.mobile_app.data.local.TokenManager.isLoggedIn)
                            Screen.Home.route
                        else
                            Screen.Login.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                Screen.Login.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                LoginScreen(
                    authViewModel = authViewModel,
                    languageManager = languageManager,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNeedsVerification = {
                        navController.navigate(Screen.VerifyEmail.route)
                    },
                    onForgotClick = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }

            composable(
                Screen.VerifyEmail.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val email = authViewModel.uiState.value.pendingEmail ?: ""
                VerifyEmailScreen(
                    authViewModel = authViewModel,
                    email = email,
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Home — faqat shu ekranda HomeViewModel yaratiladi
            composable(
                Screen.Home.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                val activity = LocalContext.current as ComponentActivity
                val wishlistViewModel: com.example.mobile_app.ui.viewmodel.WishlistViewModel = viewModel(viewModelStoreOwner = activity, factory = com.example.mobile_app.ui.viewmodel.WishlistViewModel.Factory)
                val cartViewModel: CartViewModel = viewModel(viewModelStoreOwner = activity, factory = CartViewModel.Factory)
                HomeScreen(
                    homeViewModel = homeViewModel,
                    wishlistViewModel = wishlistViewModel,
                    cartViewModel = cartViewModel,
                    onNavigateToProduct = { productId ->
                        navController.navigate(Screen.ProductDetail.route(productId))
                    }
                )
            }

            // Catalog — endi faqat kategoriyalar grid'i (mahsulot ro'yxati yo'q)
            composable(
                Screen.Catalog.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                val notificationViewModel: com.example.mobile_app.ui.viewmodel.NotificationViewModel = viewModel(factory = com.example.mobile_app.ui.viewmodel.NotificationViewModel.Factory)
                CatalogScreen(
                    homeViewModel    = homeViewModel,
                    notificationViewModel = notificationViewModel,
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Screen.CategoryProducts.route(categoryId))
                    }
                )
            }

            // CategoryProducts — tanlangan kategoriya mahsulotlari (alohida sahifa)
            composable(
                route = Screen.CategoryProducts.route,
                arguments = listOf(navArgument("categoryId") {}),
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
                val vm: com.example.mobile_app.ui.viewmodel.CategoryProductsViewModel =
                    viewModel(factory = com.example.mobile_app.ui.viewmodel.CategoryProductsViewModel.Factory)
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                val activity = LocalContext.current as ComponentActivity
                val cartViewModel: CartViewModel = viewModel(viewModelStoreOwner = activity, factory = CartViewModel.Factory)
                val wishlistViewModel: com.example.mobile_app.ui.viewmodel.WishlistViewModel = viewModel(viewModelStoreOwner = activity, factory = com.example.mobile_app.ui.viewmodel.WishlistViewModel.Factory)
                val wishlistState by wishlistViewModel.wishlist.collectAsState()
                val categories by homeViewModel.categories.collectAsState()
                CategoryProductsScreen(
                    categoryId = categoryId,
                    categoryName = categories.firstOrNull { it.id == categoryId }
                        ?.localizedName(com.example.mobile_app.util.AppLanguage.current)
                        ?: "",
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.route(productId))
                    },
                    favoritedIds = (wishlistState as? com.example.mobile_app.ui.state.UiState.Success)
                        ?.data?.map { it.productId }?.toSet() ?: emptySet(),
                    onFavoriteToggle = { productId, currentlyFavorited ->
                        wishlistViewModel.toggleFavorite(productId, currentlyFavorited)
                    },
                    onAddToCart = { product -> cartViewModel.updateItem(product.id, 1) }
                )
            }

            composable(
                Screen.Notifications.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val notificationViewModel: com.example.mobile_app.ui.viewmodel.NotificationViewModel = viewModel(factory = com.example.mobile_app.ui.viewmodel.NotificationViewModel.Factory)
                com.example.mobile_app.ui.screens.notifications.NotificationsScreen(
                    viewModel = notificationViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Cart — CartViewModel activity-scoped: Checkout bilan bir instance
            // (clearCart darhol aks etadi, stale data yo'q)
            composable(
                Screen.Cart.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val activity = LocalContext.current as ComponentActivity
                val cartViewModel: CartViewModel = viewModel(viewModelStoreOwner = activity, factory = CartViewModel.Factory)
                CartScreen(
                    cartViewModel = cartViewModel,
                    onCheckout = { navController.navigate(Screen.Checkout.route) },
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.route(productId))
                    }
                )
            }

            // Orders — OrderViewModel activity-scoped: Detail/Checkout bilan bir instance
            // (cancel/order ro'yxatda darhol aks etadi)
            composable(
                Screen.Orders.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val activity = LocalContext.current as ComponentActivity
                val orderViewModel: OrderViewModel = viewModel(viewModelStoreOwner = activity, factory = OrderViewModel.Factory)
                OrdersScreen(
                    orderViewModel = orderViewModel,
                    onOrderClick = { order ->
                        navController.navigate(Screen.OrderDetail.route(order.id))
                    }
                )
            }

            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(navArgument("orderId") {}),
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                val activity = LocalContext.current as ComponentActivity
                val orderViewModel: OrderViewModel = viewModel(viewModelStoreOwner = activity, factory = OrderViewModel.Factory)
                OrderDetailScreen(
                    orderId = orderId,
                    orderViewModel = orderViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Profile.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    languageManager = languageManager,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToAddresses = { navController.navigate(Screen.Addresses.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToPaymentMethods = { navController.navigate(Screen.PaymentMethods.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToHelp = { navController.navigate(Screen.Help.route) }
                )
            }

            composable(
                Screen.Search.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val activity = LocalContext.current as ComponentActivity
                val wishlistViewModel: com.example.mobile_app.ui.viewmodel.WishlistViewModel = viewModel(viewModelStoreOwner = activity, factory = com.example.mobile_app.ui.viewmodel.WishlistViewModel.Factory)
                val cartViewModel: CartViewModel = viewModel(viewModelStoreOwner = activity, factory = CartViewModel.Factory)
                val wishlistState by wishlistViewModel.wishlist.collectAsState()
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.route(productId))
                    },
                    favoritedIds = (wishlistState as? com.example.mobile_app.ui.state.UiState.Success)
                        ?.data?.map { it.productId }?.toSet() ?: emptySet(),
                    onFavoriteToggle = { productId, currentlyFavorited ->
                        wishlistViewModel.toggleFavorite(productId, currentlyFavorited)
                    },
                    onAddToCart = { product -> cartViewModel.updateItem(product.id, 1) }
                )
            }

            // Checkout — faqat shu ekranda kerakli VM yaratiladi
            composable(
                Screen.Checkout.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val activity = LocalContext.current as ComponentActivity
                val cartViewModel: CartViewModel = viewModel(viewModelStoreOwner = activity, factory = CartViewModel.Factory)
                val orderViewModel: OrderViewModel = viewModel(viewModelStoreOwner = activity, factory = OrderViewModel.Factory)
                CheckoutScreen(
                    cartViewModel = cartViewModel,
                    orderViewModel = orderViewModel,
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = {
                        navController.navigate(Screen.Orders.route) {
                            popUpTo(Screen.Cart.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") {}),
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                val vm: ProductDetailViewModel = viewModel(factory = ProductDetailViewModel.Factory)
                val activity = LocalContext.current as ComponentActivity
                val cartViewModel: CartViewModel = viewModel(viewModelStoreOwner = activity, factory = CartViewModel.Factory)
                val wishlistViewModel: com.example.mobile_app.ui.viewmodel.WishlistViewModel = viewModel(viewModelStoreOwner = activity, factory = com.example.mobile_app.ui.viewmodel.WishlistViewModel.Factory)
                ProductDetailScreen(
                    productId = productId,
                    viewModel = vm,
                    cartViewModel = cartViewModel,
                    wishlistViewModel = wishlistViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { newProductId ->
                        navController.navigate(Screen.ProductDetail.route(newProductId))
                    }
                )
            }

            // Favorites — faqat shu ekranda WishlistViewModel yaratiladi
            composable(
                Screen.Favorites.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                val activity = LocalContext.current as ComponentActivity
                val wishlistViewModel: com.example.mobile_app.ui.viewmodel.WishlistViewModel = viewModel(viewModelStoreOwner = activity, factory = com.example.mobile_app.ui.viewmodel.WishlistViewModel.Factory)
                com.example.mobile_app.ui.screens.wishlist.WishlistScreen(
                    viewModel = wishlistViewModel,
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.route(productId))
                    }
                )
            }

            composable(
                Screen.Addresses.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) { backStackEntry ->
                val handle = backStackEntry.savedStateHandle
                val mapPicked: com.example.mobile_app.data.model.address.GeoPlace? =
                    if (handle.contains("picked_lat") && handle.contains("picked_lng")) {
                        com.example.mobile_app.data.model.address.GeoPlace(
                            handle.get<String>("picked_name") ?: "",
                            handle.get<Double>("picked_lat") ?: 0.0,
                            handle.get<Double>("picked_lng") ?: 0.0
                        )
                    } else null
                // Bitta ViewModel — dialog ham, map ham bir xil holatni ko'radi
                val addrVmForNav: com.example.mobile_app.ui.viewmodel.AddressViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = com.example.mobile_app.ui.viewmodel.AddressViewModel.Factory
                    )
                val pickedForMap = addrVmForNav.formPicked.collectAsState().value
                AddressesScreen(
                    addressViewModel = addrVmForNav,
                    onBack = { navController.popBackStack() },
                    onOpenMap = {
                        val p = pickedForMap
                        navController.navigate(Screen.MapPicker.route(p?.lat, p?.lng))
                    },
                    mapPicked = mapPicked,
                    onMapPickedConsumed = {
                        handle.remove<Double>("picked_lat")
                        handle.remove<Double>("picked_lng")
                        handle.remove<String>("picked_name")
                    }
                )
            }

            composable(
                Screen.MapPicker.route,
                arguments = listOf(
                    androidx.navigation.navArgument("lat") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument("lng") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) { backStackEntry ->
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val initLat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                val initLng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
                val geoRepo = androidx.compose.runtime.remember {
                    com.example.mobile_app.data.repository.AddressRepository(
                        com.example.mobile_app.data.remote.RetrofitClient.addressApiService
                    )
                }
                MapPickerScreen(
                    initialLat = initLat,
                    initialLng = initLng,
                    onBack = { navController.popBackStack() },
                    onLoadCoverage = { geoRepo.coverageZones().getOrDefault(emptyList()) },
                    onConfirm = { lat, lng ->
                        scope.launch {
                            // Nuqta nomi teskari geokodlashdan olinadi
                            val place = geoRepo.reversePlace(lat, lng).getOrNull()
                            navController.previousBackStackEntry?.savedStateHandle?.apply {
                                set("picked_lat", lat)
                                set("picked_lng", lng)
                                set("picked_name", place?.name ?: "$lat, $lng")
                            }
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(
                Screen.PaymentMethods.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                PaymentMethodsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Settings.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                SettingsScreen(
                    languageManager = languageManager,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Help.route,
                enterTransition = { quickFadeIn },
                exitTransition = { quickFadeOut }
            ) {
                HelpScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Global fullName gate — agar user fullName kiritmasdan tizimdan uzilib qolgan bo'lsa,
        // keyingi kirishda qayta tekshiriladi va fullName siz davom etib bo'lmaydi
        val authUiState by authViewModel.uiState.collectAsState()
        val currentRoute by navController.currentBackStackEntryAsState()
        val route = currentRoute?.destination?.route
        val isAuthFlow = route == Screen.Login.route || route == Screen.Splash.route || route == Screen.VerifyEmail.route || route == Screen.ForgotPassword.route
        if (authUiState.needsFullName && authUiState.isLoggedIn && !isAuthFlow) {
            var pendingName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            val fullNameFieldColors = com.example.mobile_app.ui.theme.appTextFieldColors()
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                titleContentColor = Color(0xFF0F172A),
                textContentColor = Color(0xFF0F172A),
                title = {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(com.example.mobile_app.ui.theme.Primary.copy(alpha = 0.12f)),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.Person,
                                contentDescription = null,
                                tint = com.example.mobile_app.ui.theme.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            "Ism Familiyangiz *",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                text = {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "Murojaat uchun ismingizni kiriting. Busiz davom etib bo'lmaydi.",
                            fontSize = 13.sp,
                            color = com.example.mobile_app.ui.theme.TextSecondary,
                            lineHeight = 18.sp
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = pendingName,
                            onValueChange = { pendingName = it },
                            placeholder = { androidx.compose.material3.Text("Masalan: Ali Valiyev", color = com.example.mobile_app.ui.theme.TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = fullNameFieldColors,
                            leadingIcon = {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Default.Person,
                                    contentDescription = null,
                                    tint = com.example.mobile_app.ui.theme.Primary
                                )
                            }
                        )
                        authUiState.error?.let {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.Text(it, color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = { authViewModel.submitFullName(pendingName.trim()) },
                        enabled = pendingName.trim().length >= 2 && !authUiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = com.example.mobile_app.ui.theme.Primary,
                            disabledContainerColor = com.example.mobile_app.ui.theme.Primary.copy(alpha = 0.45f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        if (authUiState.isLoading) androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        else androidx.compose.material3.Text("Saqlash", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp)
                    }
                },
                dismissButton = null
            )
        }
    }
}

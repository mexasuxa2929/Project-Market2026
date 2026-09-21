package com.example.mobile_app.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import android.widget.Toast
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.Brand
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.model.RecommendedProduct
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.StarColor
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.CartViewModel
import com.example.mobile_app.ui.viewmodel.HomeViewModel
import com.example.mobile_app.ui.viewmodel.WishlistViewModel
import com.example.mobile_app.util.AppLanguage
import com.example.mobile_app.util.localizedDisplayBrand
import com.example.mobile_app.util.localizedName
import com.example.mobile_app.util.tr
import com.example.mobile_app.util.trNow
import com.example.mobile_app.util.formatPrice

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    wishlistViewModel: WishlistViewModel,
    cartViewModel: CartViewModel? = null,
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {}
) {
    val trendingProducts by homeViewModel.trendingProducts.collectAsState()
    val recommended      by homeViewModel.recommended.collectAsState()
    val isLoadingMore    by homeViewModel.isLoadingMore.collectAsState()
    val hasMorePages     by homeViewModel.hasMorePages.collectAsState()
    // Eski ro'yxat ekranda qoladi — faqat pull-indikator aylanadi (1.5)
    val isRefreshing     by homeViewModel.isRefreshing.collectAsState()
    val pullState    = rememberPullToRefreshState()
    val scope        = rememberCoroutineScope()
    val listState    = rememberLazyListState()

    val wishlistState by wishlistViewModel.wishlist.collectAsState()
    val favoritedIds = remember(wishlistState) {
        (wishlistState as? UiState.Success)?.data?.map { it.productId }?.toSet() ?: emptySet()
    }

    val context = LocalContext.current
    val onFavToggle: (Product, Boolean) -> Unit = remember(wishlistViewModel) {
        { product, isFav -> wishlistViewModel.toggleFavorite(product.id, isFav) }
    }

    // Savatga qo'shish: optimistic line + toast
    val onAddToCart: (Product) -> Unit = remember(cartViewModel) {
        { product ->
            cartViewModel?.addProduct(product)
            Toast.makeText(context, trNow("detail_added_to_cart"), Toast.LENGTH_SHORT).show()
        }
    }

    val trendingRows = remember(trendingProducts) {
        (trendingProducts as? UiState.Success)?.data?.chunked(2) ?: emptyList()
    }
    val flashProducts = remember(trendingProducts) {
        (trendingProducts as? UiState.Success)?.data
            ?.filter { it.discountPercent > 0 }
            ?.take(8) ?: emptyList()
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                when (intent.action) {
                    com.example.mobile_app.MexaMarketApp.ACTION_PRODUCT_DELETED,
                    com.example.mobile_app.MexaMarketApp.ACTION_PRODUCT_CREATED -> {
                        homeViewModel.refresh()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(com.example.mobile_app.MexaMarketApp.ACTION_PRODUCT_DELETED)
            addAction(com.example.mobile_app.MexaMarketApp.ACTION_PRODUCT_CREATED)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems  = layoutInfo.totalItemsCount
            // 3 -> 5: keyingi sahifa erta yuklanadi, scroll oxirida kutish bo'lmaydi
            lastVisible >= totalItems - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore && hasMorePages) {
            homeViewModel.loadNextPage()
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F3F7))
    ) {
        PullToRefreshBox(
            isRefreshing      = isRefreshing,
            onRefresh         = { scope.launch { homeViewModel.refresh() } },
            state             = pullState,
            modifier          = Modifier.fillMaxSize(),
            contentAlignment  = Alignment.TopCenter,
            indicator = {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state    = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.padding(top = statusBarHeight + 130.dp)
                )
            }
        ) {
            LazyColumn(
                state   = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "header_space", contentType = "spacer") {
                    Spacer(Modifier.height(130.dp + statusBarHeight))
                }

                if (flashProducts.isNotEmpty()) {
                    item(key = "flash", contentType = "flash_row") {
                        FlashSaleSection(
                            products = flashProducts,
                            favoritedIds = favoritedIds,
                            onProductClick = onNavigateToProduct,
                            onFavoriteToggle = onFavToggle,
                            onAddToCart = onAddToCart
                        )
                    }
                }

                item(key = "trending_header", contentType = "section_header") {
                    SectionHeader(
                        title = tr("home_trending"),
                        actionLabel = tr("home_see_all"),
                        onAction = onNavigateToCatalog
                    )
                    Spacer(Modifier.height(12.dp))
                }

                when (trendingProducts) {
                    is UiState.Loading -> item(key = "trending_loading", contentType = "loading") {
                        // Spinner o'rniga skeleton — birinchi ochilishda ham "tez" seziladi
                        TrendingLoadingSkeleton()
                    }
                    is UiState.Success -> itemsIndexed(
                        trendingRows,
                        key = { _, row -> "trending_" + (row.firstOrNull()?.id ?: "empty") },
                        contentType = { _, _ -> "product_row" }
                    ) { _, row ->
                        TrendingProductRow(
                            row = row,
                            favoritedIds = favoritedIds,
                            onProductClick = onNavigateToProduct,
                            onFavoriteToggle = onFavToggle,
                            onAddToCart = onAddToCart
                        )
                    }
                    is UiState.Error -> item(key = "trending_error", contentType = "error") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text((trendingProducts as UiState.Error).message, color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                }

                if (recommended.isNotEmpty()) {
                    item(key = "rec_header", contentType = "section_header") {
                        Spacer(Modifier.height(12.dp))
                        SectionHeader(title = tr("home_recommended"), actionLabel = tr("home_all"), onAction = onNavigateToCatalog)
                        Spacer(Modifier.height(12.dp))
                    }
                    item(key = "rec_list", contentType = "recommended_row") {
                        RecommendedSection(
                            items = recommended,
                            onProductClick = onNavigateToProduct
                        )
                    }
                }

                if (isLoadingMore) {
                    item(key = "load_more", contentType = "loading") {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = statusBarHeight)
                .align(Alignment.TopCenter)
        ) {
            HomeAppBar()
            DeliveryAddress()
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── Flash Sale Section (ayrilgan) ──────────────────────────────────────────

@Composable
private fun FlashSaleSection(
    products: List<Product>,
    favoritedIds: Set<String>,
    onProductClick: (String) -> Unit,
    onFavoriteToggle: (Product, Boolean) -> Unit,
    onAddToCart: ((Product) -> Unit)? = null
) {
    Spacer(Modifier.height(12.dp))
    SectionHeader(title = tr("home_flash_products"))
    Spacer(Modifier.height(12.dp))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }, contentType = { "flash_card" }) { product ->
            val isFav = product.id in favoritedIds
            val onClick = remember(product.id) { { onProductClick(product.id) } }
            val onToggle = remember(product.id, isFav) { { onFavoriteToggle(product, isFav) } }
            val onAdd: (() -> Unit)? = remember(product.id, onAddToCart) {
                onAddToCart?.let { cb -> { cb(product) } }
            }
            FlashCard(
                product = product,
                isFavorited = isFav,
                onClick = onClick,
                onFavoriteToggle = onToggle,
                onAddToCart = onAdd
            )
        }
    }
    Spacer(Modifier.height(24.dp))
}

// ─── Trending skeleton (birinchi yuklanishda spinner o'rniga) ───────────────

@Composable
private fun TrendingLoadingSkeleton() {
    repeat(3) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(Modifier.height(9.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 11.dp)
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 11.dp)
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE5E7EB))
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ─── Trending Product Row (ayrilgan) ────────────────────────────────────────

@Composable
private fun TrendingProductRow(
    row: List<Product>,
    favoritedIds: Set<String>,
    onProductClick: (String) -> Unit,
    onFavoriteToggle: (Product, Boolean) -> Unit,
    onAddToCart: ((Product) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (product in row) {
            val isFav = product.id in favoritedIds
            val onClick = remember(product.id) { { onProductClick(product.id) } }
            val onToggle = remember(product.id, isFav) {
                { onFavoriteToggle(product, isFav) }
            }
            val onAdd: (() -> Unit)? = remember(product.id, onAddToCart) {
                onAddToCart?.let { cb -> { cb(product) } }
            }
            HomeProductCard(
                product = product,
                modifier = Modifier.weight(1f),
                onClick = onClick,
                isFavorited = isFav,
                onFavoriteToggle = onToggle,
                onAddToCart = onAdd
            )
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
}

// ─── Recommended Section (ayrilgan) ─────────────────────────────────────────

@Composable
private fun RecommendedSection(
    items: List<RecommendedProduct>,
    onProductClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.productId }, contentType = { "recommended_card" }) { item ->
            RecommendedCard(item = item)
        }
    }
}

// ─── App Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF4F46E5)),
            contentAlignment = Alignment.Center
        ) {
            Text(tr("home_logo_m"), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "Mexa Market",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
    }
}

// ─── Delivery Address ─────────────────────────────────────────────────────────

@Composable
private fun DeliveryAddress() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F3F7))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(tr("home_delivering_to"), fontSize = 11.sp, color = Color(0xFF888888))
        Text(
            tr("home_delivery_address"),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF888888), modifier = Modifier.size(16.dp))
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        if (actionLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onAction)
            ) {
                Text(actionLabel, fontSize = 13.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Home Product Card ────────────────────────────────────────────────────────

@Composable
private fun HomeProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isFavorited: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onAddToCart: (() -> Unit)? = null
) {
    val cartBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1)))
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (product.thumbnail != null) {
                    ProductImage(
                        url = product.thumbnail,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp),
                        cornerRadius = 18.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) { Text("\uD83D\uDCE6", fontSize = 44.sp) }
                }
                if (onFavoriteToggle != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(9.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                            .clickable { onFavoriteToggle.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorited) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                // Matn formatlash har recomposition da qayta hisoblanmasligi uchun keshlanadi
                val lang = AppLanguage.current
                val brandRaw = remember(product.id, lang) { product.localizedDisplayBrand(lang) }
                val brandText = brandRaw.ifBlank { tr("home_default_brand") }
                val ratingText = remember(product.avgRating) { "%.1f".format(product.avgRating ?: 0.0) }
                val reviewCount = product.reviewCount ?: 0L
                val priceText = remember(product.basePrice) {
                    if (product.basePrice != null) formatPrice(product.basePrice) else "—"
                }
                Text(brandText, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Box(Modifier.height(36.dp)) {
                    Text(
                        product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = StarColor, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(ratingText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    Spacer(Modifier.width(3.dp))
                    Text("($reviewCount)", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = priceText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cartBrush)
                            .then(
                                if (onAddToCart != null) Modifier.clickable { onAddToCart.invoke() }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

// ─── Flash Card ──────────────────────────────────────────────────────────────

@Composable
private fun FlashCard(product: Product, isFavorited: Boolean = false, onClick: () -> Unit = {}, onFavoriteToggle: (() -> Unit)? = null, onAddToCart: (() -> Unit)? = null) {
    val discount = product.discountPercent
    // Og'ir string formatlash bir marta hisoblanib keshlanadi
    val ratingText = remember(product.avgRating) { "%.1f".format(product.avgRating ?: 0.0) }
    val oldPrice = remember(product.basePrice, discount) {
        product.basePrice?.let { it / ((100 - discount) / 100.0) }
    }
    val oldPriceText = remember(oldPrice) { oldPrice?.let { formatPrice(it) } }
    val priceText = remember(product.basePrice) {
        product.basePrice?.let { formatPrice(it) }
    }

    val cardBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF231A55), Color(0xFF3730A3), Color(0xFF4F46E5))
        )
    }
    val discountBrush = remember {
        Brush.linearGradient(listOf(Color(0xFFFF5A3C), Color(0xFFEF4444)))
    }

    Card(
        modifier = Modifier.width(224.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        // 4dp -> 1dp: katta soya lazy listda kadr tushishining asosiy sabablaridan biri
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(cardBrush)
                .padding(10.dp)
        ) {
            Box(
                Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .align(Alignment.TopEnd)
                    .offset(x = 36.dp, y = (-38).dp)
            )
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .align(Alignment.BottomStart)
                    .offset(x = (-28).dp, y = 26.dp)
            )

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        if (product.thumbnail != null) {
                            ProductImage(
                                url = product.thumbnail,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Color(0xFFF3F4F6)), Alignment.Center) {
                                Text("\uD83D\uDCE6", fontSize = 36.sp)
                            }
                        }
                    }
                    if (discount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(discountBrush)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .zIndex(2f)
                        ) {
                            Text("-$discount%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                    if (onFavoriteToggle != null) {
                        val onFavClick = remember(product.id) { { onFavoriteToggle.invoke() } }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                                .clickable(onClick = onFavClick)
                                .zIndex(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorited) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC1E1B4B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .zIndex(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\uD83D\uDD25", fontSize = 10.sp)
                            Text("Ommabop", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(Modifier.height(34.dp)) {
                    Text(
                        product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }
                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(ratingText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("(${product.reviewCount ?: 0} ta baho)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (oldPriceText != null && priceText != null && discount > 0) {
                            Text(
                                "$oldPriceText so'm",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.55f),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                            Text(
                                "$priceText so'm",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFBBF24)
                            )
                        } else {
                            Text("—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .then(
                                if (onAddToCart != null) Modifier.clickable { onAddToCart.invoke() }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─── Recommended Card ─────────────────────────────────────────────────────────

@Composable
private fun RecommendedCard(item: RecommendedProduct) {
    Card(
        modifier  = Modifier.width(155.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (item.thumbnail != null) {
                    ProductImage(
                        url = item.thumbnail,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp),
                        cornerRadius = 16.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) { Text("\uD83D\uDCE6", fontSize = 38.sp) }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = StarColor, modifier = Modifier.size(11.dp))
                        Text(" %.1f".format(item.avgRating), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("${item.reviewCount}${tr("home_review_suffix")}", fontSize = 10.sp, color = TextSecondary)
                if (item.basePrice != null) {
                    Spacer(Modifier.height(3.dp))
                    Text("${formatPrice(item.basePrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
                } else {
                    Spacer(Modifier.height(3.dp))
                    Text("\u2014", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        }
    }
}

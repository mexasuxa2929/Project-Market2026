package com.example.mobile_app.ui.screens.product

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.model.ProductColorSibling
import com.example.mobile_app.data.model.PublicReview
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.viewmodel.CartViewModel
import com.example.mobile_app.ui.viewmodel.ProductDetailViewModel
import com.example.mobile_app.ui.viewmodel.WishlistViewModel
import com.example.mobile_app.util.tr
import com.example.mobile_app.util.formatPrice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Primary    = Color(0xFF4F46E5)
private val BgColor    = Color(0xFFF2F3F7)
private val StarYellow = Color(0xFFFBBF24)
private val ErrorRed   = Color(0xFFEF4444)
private val TextDark   = Color(0xFF1A1A2E)
private val TextGray   = Color(0xFF888888)
private val BorderGray = Color(0xFFE5E7EB)

@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: ProductDetailViewModel,
    cartViewModel: CartViewModel? = null,
    wishlistViewModel: WishlistViewModel? = null,
    onBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit = {}
) {
    LaunchedEffect(productId) {
        viewModel.load(productId)
        viewModel.loadRecommended()
    }

    val productState by viewModel.product.collectAsState()
    val reviews      by viewModel.reviews.collectAsState()
    val ratingStats  by viewModel.ratingStats.collectAsState()
    val quantity     by viewModel.quantity.collectAsState()
    val selectedVariantId by viewModel.selectedVariantId.collectAsState()
    val recommendedState by viewModel.recommendedProducts.collectAsState()

    val globalFavoritedIds by (wishlistViewModel?.favoritedIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })
    // Ko'rsatilayotgan (tanlangan) variant id — nav argument emas, state dagi mahsulot.
    val displayedId = (productState as? UiState.Success)?.data?.id ?: productId
    val isFavorited = globalFavoritedIds.contains(displayedId)

    Box(Modifier.fillMaxSize().background(BgColor)) {
        when (val s = productState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(s.message, color = ErrorRed)
            }
            is UiState.Success -> {
                val product = s.data
                val price   = product.basePrice

                // ── 4. Boshqa rangdagi nusxalar ─────────────────────────
                // Backend joriy productni ham siblingColors'da qaytaradi (isCurrent=true).
                // HAMMA rang (joriy + boshqalar) bitta barqaror saralangan ro'yxatda —
                // qaysi variant ochilmasin ketma-ketlik BIR XIL bo'ladi.
                // Select paytida HECH NARSA o'zgarmaydi (qayta yuklash yo'q).
                val allSwatches = remember(product.id) {
                    val current = ProductColorSibling(
                        id = product.id,
                        color = product.color,
                        colorCode = product.colorCode,
                        barcode = product.barcode.orEmpty(),
                        active = true
                    )
                    (product.siblingColors.orEmpty()
                        .filter { it.active && it.id != product.id } + current)
                        .sortedWith(compareBy({ it.color.orEmpty() }, { it.colorCode.orEmpty() }, { it.id }))
                }
                // Tanlangan variant — default joriy mahsulotning o'zi.
                val selectedId = selectedVariantId ?: product.id

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    // ── 1. Rasm ────────────────────────────────────────────
                    item(key = "pager") {
                        ImagePager(
                            product = product,
                            onBack = onBack,
                            isFavorited = isFavorited,
                            onToggleFavorite = {
                                if (wishlistViewModel != null) {
                                    wishlistViewModel.toggleFavorite(product.id, isFavorited)
                                } else {
                                    viewModel.toggleFavorite(product.id)
                                }
                            }
                        )
                    }

                    // ── 2. Asosiy ma'lumotlar ──────────────────────────────
                    item(key = "info") {
                        InfoCard {
                            // Brand
                            Text(product.brandName ?: product.categoryName ?: product.manufacturerName ?: "Unknown", fontSize = 12.sp, color = TextGray)
                            Spacer(Modifier.height(4.dp))

                            // Nom
                            Text(
                                text = product.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                lineHeight = 26.sp
                            )
                            Spacer(Modifier.height(8.dp))

                            // Reyting
                            val rating = ratingStats?.avgRating ?: 0.0
                            val count  = ratingStats?.reviewCount
                            RatingRow(rating, count)
                            Spacer(Modifier.height(12.dp))

                            // Narx
                            PriceRow(product)
                        }
                    }

                    // ── 3. Ranglar ─────────────────────────────────────────
                    val colors = product.colors.orEmpty()
                    if (colors.isNotEmpty()) {
                        item(key = "colors") {
                            InfoCard {
                                ColorSelector(colors)
                            }
                        }
                    }

                    // ── 4. Boshqa rangdagi nusxalar (yuqorida hisoblangan, tartibi barqaror)
                    if (allSwatches.size > 1) {
                        item(key = "siblings") {
                            InfoCard {
                                SiblingColorSelector(
                                    siblings = allSwatches,
                                    selectedId = selectedId,
                                    onColorSelected = { viewModel.selectSiblingColor(it) }
                                )
                            }
                        }
                    }

                    // ── 5. Tavsif ──────────────────────────────────────────
                    item(key = "description") {
                        InfoCard {
                            DescriptionSection(product.description, product.shortDescription, product.deliveryDaysMin, product.deliveryDaysMax)
                        }
                    }

                    // ── 6. Xarakteristikalar ───────────────────────────────
                    item(key = "characteristics") {
                        InfoCard {
                            CharacteristicsSection(product)
                        }
                    }

                    // ── 7. Sharhlar ────────────────────────────────────────
                    if (reviews.isNotEmpty()) {
                        item(key = "reviews_header") {
                            InfoCard {
                                ReviewsHeader(reviews.size)
                            }
                        }
                        items(reviews.take(3), key = { it.id }) { review ->
                            ReviewCard(review)
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    // ── 8. Tavsiya etiladi ──────────────────────────────────
                    val rec = recommendedState
                    if (rec is UiState.Success && rec.data.isNotEmpty()) {
                        item(key = "recommended") {
                            val products = rec.data
                            Column(Modifier.padding(start = 12.dp, bottom = 16.dp)) {
                                Text(
                                    tr("detail_recommended"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(end = 12.dp)
                                ) {
                                    items(products, key = { it.id }, contentType = { "recommended_card" }) { product ->
                                        RecommendedProductCard(
                                            product = product,
                                            onClick = { onNavigateToProduct(product.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Pastki panel ───────────────────────────────────────────
                BottomBar(
                    quantity    = quantity,
                    price       = price,
                    onDecrement = viewModel::decrement,
                    onIncrement = viewModel::increment,
                    productId   = product.id,
                    totalStock  = product.totalStock,
                    onAddToCart = {
                        // Savatga aynan SELECT qilingan rang-varianti tushadi.
                        cartViewModel?.updateItem(viewModel.effectiveProductId(), quantity)
                    },
                    modifier    = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ── Tavsiya etiladigan mahsulot kartasi ────────────────────────────────────────

@Composable
private fun RecommendedProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                ProductImage(
                    url = product.thumbnail,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    modifier = Modifier.height(30.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "%.1f".format(product.avgRating ?: 0.0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "(${product.reviewCount ?: 0L})",
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatPrice(product.basePrice ?: 0.0)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary
                )
            }
        }
    }
}

// ── Yordamchi wrapper ─────────────────────────────────────────────────────────

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) { content() }
}

// ── Rasm karuseli ─────────────────────────────────────────────────────────────

@Composable
private fun ImagePager(
    product: Product,
    onBack: () -> Unit,
    isFavorited: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    val images = product.originalImageUrls.orEmpty().ifEmpty { listOf(null) }
    val pagerState = rememberPagerState { images.size }
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            HorizontalPager(state = pagerState) { page ->
                val url = images[page]
                if (url != null) {
                    ProductImage(
                        url = url,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        size = Pair(800, 800)
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)), Alignment.Center) {
                        Text("📦", fontSize = 80.sp)
                    }
                }
            }

            // Magic indikator — past markazda, gradient pill ko'rinishida
            if (images.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(Color(0x66000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    images.indices.forEach { i ->
                        val selected = i == pagerState.currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 22.dp else 6.dp,
                            animationSpec = tween(300),
                            label = "magic_dot_$i"
                        )
                        Box(
                            Modifier
                                .width(dotWidth)
                                .height(6.dp)
                                .then(
                                    if (selected) Modifier.shadow(
                                        6.dp, RoundedCornerShape(3.dp),
                                        ambientColor = Color(0xFF4F46E5),
                                        spotColor = Color(0xFF4F46E5)
                                    ) else Modifier
                                )
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (selected) {
                                            listOf(Color(0xFFA5B4FC), Color(0xFF4F46E5))
                                        } else {
                                            listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.5f))
                                        }
                                    )
                                )
                        )
                    }
                }
            }

            // Yuqori tugmalar
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White),
                        Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = TextDark, modifier = Modifier.size(20.dp))
                    }
                }
                Row {
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out ${product.name} on Mexa Market!")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }) {
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .shadow(2.dp, CircleShape).background(Color.White), Alignment.Center) {
                            Icon(Icons.Default.Share, null, tint = TextDark, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .shadow(2.dp, CircleShape).background(Color.White), Alignment.Center) {
                            Icon(imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorited) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Reyting ───────────────────────────────────────────────────────────────────

@Composable
private fun RatingRow(rating: Double, reviewCount: Long?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val full = rating.toInt()
        repeat(5) { i ->
            Icon(imageVector = if (i < full) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = StarYellow,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = buildString {
                append("%.1f".format(rating))
                if (reviewCount != null && reviewCount > 0) {
                    val label = if (reviewCount >= 1000) "${"%.1f".format(reviewCount / 1000.0)}k" else "$reviewCount"
                    append(" ($label${tr("detail_review_suffix")}")
                }
            },
            fontSize = 13.sp,
            color = TextGray
        )
    }
}

// ── Narx ──────────────────────────────────────────────────────────────────────

@Composable
private fun PriceRow(product: Product) {
    val price = product.basePrice

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Flash Sale badge
        if (product.discountPercent > 0) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF6B35))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tr("detail_flash_sale"), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (price != null) {
            Text(
                text = formatPrice(price),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary
            )
        } else {
            Text(tr("detail_no_price"), fontSize = 14.sp, color = TextGray)
        }
    }
}

// ── Rang tanlash ──────────────────────────────────────────────────────────────

@Composable
private fun ColorSelector(colors: List<String>) {
    var selectedColor by remember { mutableStateOf(colors.first()) }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tr("detail_color"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Text(selectedColor, fontSize = 14.sp, color = TextGray)
    }
    Spacer(Modifier.height(10.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(colors, key = { it }) { colorName ->
            val selected = colorName == selectedColor
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colorNameToColor(colorName))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) Primary else BorderGray,
                        shape = CircleShape
                    )
                    .clickable { selectedColor = colorName }
            )
        }
    }
}

// ── Boshqa rangdagi nusxalar (mustaqil Product'lar) ────────────────────────────

/**
 * Variantlar olib tashlangach, har bir rang mustaqil Product hisoblanadi.
 * Shu sababli rang tanlash endi shu ekrandagi sub-obyektni tanlash emas,
 * balki tanlangan rangning o'z productId'siga o'tish (qayta yuklash) demakdir.
 */
@Composable
private fun SiblingColorSelector(
    siblings: List<ProductColorSibling>,
    selectedId: String,
    onColorSelected: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tr("detail_other_colors"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
    Spacer(Modifier.height(10.dp))

    // Bitta barqaror ro'yxat: select'da tartib o'zgarmaydi, faqat halqa ko'chadi.
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(siblings, key = { it.id }) { sibling ->
            SwatchCircle(
                color = resolveSwatchColor(sibling.color, sibling.colorCode),
                selected = sibling.id == selectedId,
                enabled = sibling.active,
                onClick = { onColorSelected(sibling.id) }
            )
        }
    }
}

@Composable
private fun SwatchCircle(
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) Primary else BorderGray,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
    )
}

/**
 * Swatch rangi ustuvorligi: hex colorCode → inglizcha rang nomi → kulrang fallback.
 * Backend colorCode ("#EF4444") yuboradi, color esa ixtiyoriy nom bo'lishi mumkin.
 */
private fun resolveSwatchColor(colorName: String?, colorCode: String?): Color {
    parseColorCode(colorCode)?.let { return it }
    return colorNameToColor(colorName.orEmpty())
}

private fun parseColorCode(code: String?): Color? {
    if (code.isNullOrBlank()) return null
    val hex = code.trim().removePrefix("#")
    val argb = when (hex.length) {
        // #RGB → #FFRRGGBB
        3 -> "FF" + hex.map { "$it$it" }.joinToString("")
        // #RRGGBB → #FFRRGGBB
        6 -> "FF$hex"
        // #AARRGGBB
        8 -> hex
        else -> return null
    }
    return try {
        Color(argb.toLong(16))
    } catch (e: NumberFormatException) {
        null
    }
}

private fun colorNameToColor(name: String): Color = when (name.lowercase().trim()) {
    "red"                       -> Color(0xFFEF4444)
    "blue", "midnight indigo"   -> Color(0xFF4F46E5)
    "green"                     -> Color(0xFF22C55E)
    "black"                     -> Color(0xFF111111)
    "white"                     -> Color(0xFFF0F0F0)
    "yellow"                    -> Color(0xFFFBBF24)
    "gray", "grey"              -> Color(0xFF9CA3AF)
    "pink"                      -> Color(0xFFF472B6)
    "orange"                    -> Color(0xFFF97316)
    "purple"                    -> Color(0xFF9333EA)
    "brown"                     -> Color(0xFF92400E)
    "navy"                      -> Color(0xFF1E3A8A)
    else                        -> Color(0xFFCBD5E1)
}

// ── Tavsif ────────────────────────────────────────────────────────────────────

@Composable
private fun DescriptionSection(description: String?, shortDescription: String?, deliveryDaysMin: Int?, deliveryDaysMax: Int?) {
    val displayText = description?.takeIf { it.isNotBlank() }
        ?: shortDescription?.takeIf { it.isNotBlank() }

    // Header
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tr("detail_description"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }

    Spacer(Modifier.height(10.dp))
    if (displayText != null) {
        Text(
            text      = displayText,
            fontSize  = 13.sp,
            color     = Color(0xFF555555),
            lineHeight= 21.sp
        )
    } else {
        Text(
            text     = tr("detail_no_description"),
            fontSize = 13.sp,
            color    = TextGray
        )
    }

}

// ── Xarakteristikalar ─────────────────────────────────────────────────────────

private fun formatNum(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

@Composable
private fun CharacteristicsSection(product: Product) {
    val entries = mutableListOf<Triple<String, String, String>>()
    fun addIf(emoji: String, label: String, value: String?) {
        if (!value.isNullOrBlank()) entries.add(Triple(emoji, label, value))
    }
    addIf("🏷️", tr("detail_char_brand"), product.brandName ?: product.manufacturerName)
    addIf("🗂️", tr("detail_char_category"), product.categoryName)
    // Barcode va SKU userga ko'rinmaydi (ichki ma'lumot) — ataylab chiqarilmaydi.
    addIf("🎨", tr("detail_char_color"), product.color)
    addIf("🧵", tr("detail_char_material"), product.material)
    addIf("🌍", tr("detail_char_origin"), product.countryOfOrigin)
    if (product.weight > 0) {
        entries.add(Triple("⚖️", tr("detail_char_weight"), formatNum(product.weight) + tr("detail_char_weight_suffix")))
    }
    if (product.length > 0 || product.width > 0 || product.height > 0) {
        entries.add(Triple(
            "📐",
            tr("detail_char_dimensions"),
            "${formatNum(product.length)}×${formatNum(product.width)}×${formatNum(product.height)} ${tr("detail_char_dimensions_suffix")}"
        ))
    }
    addIf("📦", tr("detail_char_package"), product.packageType)
    if (product.warrantyMonths != null && product.warrantyMonths > 0) {
        entries.add(Triple("🛡️", tr("detail_char_warranty"), "${product.warrantyMonths}${tr("detail_char_warranty_suffix")}"))
    }
    addIf("📏", tr("detail_char_unit"), product.unit)
    if (product.leadTimeDays != null && product.leadTimeDays > 0) {
        entries.add(Triple("🚚", tr("detail_char_lead_time"), "${product.leadTimeDays}${tr("detail_char_lead_time_days")}"))
    }
    if (entries.isEmpty()) return

    // Sarlavha — gradient badge bilan
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFA5B4FC), Primary))),
            Alignment.Center
        ) {
            Text("⚙️", fontSize = 15.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(tr("detail_char_title"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }

    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (emoji, label, value) ->
                    CharTile(emoji = emoji, label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CharTile(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F9))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Color.White),
            Alignment.Center
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = TextGray)
            Text(
                value,
                fontSize = 12.sp,
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Sharhlar ──────────────────────────────────────────────────────────────────

@Composable
private fun ReviewsHeader(count: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tr("detail_reviews"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Text(tr("detail_view_all"), fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReviewCard(review: PublicReview) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Primary),
                Alignment.Center
            ) {
                Text(
                    text = (review.username?.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                review.username ?: tr("detail_anonymous"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            // Yulduzlar
            Row {
                repeat(5) { i ->
                    Icon(imageVector = if (i < review.rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (!review.comment.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "\"${review.comment}\"",
                fontSize  = 13.sp,
                color     = Color(0xFF555555),
                lineHeight= 19.sp,
                maxLines  = 4,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Pastki panel ──────────────────────────────────────────────────────────────

@Composable
private fun BottomBar(
    quantity: Int,
    price: Double?,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    productId: String,
                    totalStock: Int? = null,
    onAddToCart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val total = (price ?: 0.0) * quantity
    val scope = rememberCoroutineScope()
    var added by remember { mutableStateOf(false) }
    val popScale = remember { Animatable(1f) }
    val fillColor by animateColorAsState(
        targetValue = if (added) Color(0xFF16A34A) else Color.Transparent,
        animationSpec = tween(220),
        label = "add_to_cart_fill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (added) Color(0xFF16A34A) else Primary,
        animationSpec = tween(220),
        label = "add_to_cart_border"
    )

    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp)
            .background(Color.White)
            // Oq fon sistem navigatsiya sohasigacha tushadi — pastda ikki xil
            // oq/kulrang chiziq qolmaydi, butun pastki qism bir xil oq bo'ladi.
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miqdor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDecrement),
                    Alignment.Center
                ) { Text("−", fontSize = 20.sp, color = TextDark, fontWeight = FontWeight.Light) }

                Text(
                    text = quantity.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary)
                        // Stock chegarasi olib tashlandi: miqdor omborda bor-yo'qligiga
                        // bog'liq emas (yetmagan qismga avtomatik zakaz beriladi)
                        .clickable { onIncrement() },
                    Alignment.Center
                ) { Text("+", fontSize = 20.sp, color = Color.White) }
            }

            // Jami narx
            Column(horizontalAlignment = Alignment.End) {
                Text(tr("detail_total_price"), fontSize = 11.sp, color = TextGray)
                Text(
                    text = formatPrice(total),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                onAddToCart()
                if (!added) {
                    added = true
                    scope.launch {
                        popScale.animateTo(0.85f, tween(90))
                        popScale.animateTo(
                            1.12f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )
                        popScale.animateTo(
                            1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
                        )
                        delay(1200)
                        added = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = fillColor,
                contentColor   = if (added) Color.White else Primary
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
        ) {
            if (added) {
                Icon(imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(tr("detail_added_to_cart"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            } else {
                Icon(imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(tr("detail_add_to_cart"), color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

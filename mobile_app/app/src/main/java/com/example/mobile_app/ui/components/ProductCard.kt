package com.example.mobile_app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.StarColor
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.util.AppLanguage
import com.example.mobile_app.util.localizedDisplayBrand
import com.example.mobile_app.util.tr
import com.example.mobile_app.util.trNow
import com.example.mobile_app.util.formatPrice
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlin.math.roundToInt

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    isFavorited: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onAddToCart: ((Product) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val cartBrush = remember {
        Brush.linearGradient(colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1)))
    }
    val context = LocalContext.current

    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // ── Image ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                val imgHeight = if (isGrid) 145.dp else 165.dp

                if (product.thumbnail != null) {
                    ProductImage(
                        url = product.thumbnail,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imgHeight),
                        cornerRadius = 18.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imgHeight)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                            .background(Color(0xFFF1F2F6)),
                        contentAlignment = Alignment.Center
                    ) { Text("📦", fontSize = 44.sp) }
                }

                // Favorite button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onFavoriteToggle?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorited) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Badge — fragile / sale
                if (product.fragile) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(9.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFF59E0B))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(tr("product_card_fragile"), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Info ──────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                // Brand
                Text(
                    text = product.localizedDisplayBrand(AppLanguage.current).ifBlank { tr("product_card_default_brand") },
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                // Name — 2 qatorli joy zaxiralangan (kartalar balandligi teng bo'lsin)
                Box(Modifier.height(34.dp)) {
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF0F172A),
                        lineHeight = 17.sp
                    )
                }
                Spacer(Modifier.height(5.dp))

                // Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val rating = product.avgRating ?: 0.0
                    val filled = rating.roundToInt()
                    repeat(5) { i ->
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = if (i < filled) StarColor else Color(0xFFE5E7EB),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("%.1f".format(rating), fontSize = 10.sp, color = TextSecondary)
                }

                Spacer(Modifier.height(8.dp))

                // Price + Cart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (product.basePrice != null) {
                            if (product.discountPercent > 0) {
                                val original = product.basePrice * 100.0 / (100.0 - product.discountPercent)
                                Text(
                                    text = "${formatPrice(original)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${formatPrice(product.basePrice)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (product.discountPercent > 0) Color(0xFFDC2626) else Color(0xFF0F172A)
                                )
                                if (product.discountPercent > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFDC2626).copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            "−${product.discountPercent}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (product.hasSiblingColors)
                                    "${product.siblingColors.orEmpty().size + 1}${tr("product_card_colors")}"
                                else "—",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cartBrush)
                            .clickable {
                                onAddToCart?.let {
                                    it(product)
                                    Toast.makeText(context, trNow("detail_added_to_cart"), Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = tr("product_card_add_to_cart"),
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

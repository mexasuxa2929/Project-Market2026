package com.example.mobile_app.ui.screens.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.ui.components.ProductCard
import com.example.mobile_app.ui.viewmodel.CategoryProductsViewModel
import com.example.mobile_app.util.tr

private val BgColor = Color(0xFFFCF8FF)
private val Surface = Color(0xFFFFFFFF)
private val SurfaceHigh = Color(0xFFE9E7F2)
private val OnSurface = Color(0xFF1B1B23)
private val OutlineVar = Color(0xFF767586)
private val PrimaryColor = Color(0xFF2C2ABC)
private val ErrorColor = Color(0xFFBA1A1A)

/**
 * Bitta kategoriyaga tegishli mahsulotlar ro'yxati (alohida sahifa).
 * Katalogdagi kategoriya bosilganda ochiladi.
 */
@Composable
fun CategoryProductsScreen(
    categoryId: String,
    categoryName: String,
    viewModel: CategoryProductsViewModel,
    onBack: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    favoritedIds: Set<String> = emptySet(),
    onFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onAddToCart: ((Product) -> Unit)? = null
) {
    val products = remember(categoryId) { viewModel.productsFor(categoryId) }.collectAsLazyPagingItems()
    val refreshState = products.loadState.refresh
    val isFirstLoad = refreshState is LoadState.Loading && products.itemCount == 0
    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 72.dp, bottom = 24.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                isFirstLoad -> {
                    items(
                        count = 6,
                        key = { index -> "skeleton_$index" },
                        contentType = { "product_skeleton" }
                    ) {
                        CategoryProductSkeleton()
                    }
                }
                refreshState is LoadState.Error && products.itemCount == 0 -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, key = "error") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😕", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    (refreshState.error as? Throwable)?.message ?: tr("catalog_error"),
                                    color = ErrorColor, fontSize = 14.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { products.retry() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(tr("catalog_retry"), color = Color.White) }
                            }
                        }
                    }
                }
                refreshState is LoadState.NotLoading && products.itemCount == 0 -> {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, key = "empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                tr("search_no_results_title"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tr("search_no_results_subtitle"),
                                fontSize = 13.sp,
                                color = OutlineVar
                            )
                        }
                    }
                }
                else -> {
                    items(
                        count = products.itemCount,
                        key = products.itemKey { it.id },
                        contentType = { "product_card" }
                    ) { index ->
                        val product = products[index]
                        if (product != null) {
                            val isFav = product.id in favoritedIds
                            val toggle = remember(product.id, onFavoriteToggle, isFav) {
                                { onFavoriteToggle(product.id, isFav) }
                            }
                            val add: (Product) -> Unit = remember(product.id, onAddToCart) {
                                { _: Product -> onAddToCart?.invoke(product) ?: Unit }
                            }
                            val open = remember(product.id, onProductClick) {
                                { onProductClick(product.id) }
                            }
                            ProductCard(
                                product = product,
                                isGrid = true,
                                isFavorited = isFav,
                                onFavoriteToggle = toggle,
                                onAddToCart = add,
                                onClick = open
                            )
                        } else {
                            CategoryProductSkeleton()
                        }
                    }

                    when (val append = products.loadState.append) {
                        is LoadState.Loading -> item(
                            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                            key = "load_more_loading"
                        ) {
                            Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryColor, strokeWidth = 2.dp)
                            }
                        }
                        is LoadState.Error -> item(
                            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                            key = "load_more_error"
                        ) {
                            OutlinedButton(
                                onClick = { products.retry() },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            ) {
                                Text(tr("catalog_retry"), color = PrimaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        // ── Sticky Header ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .shadow(4.dp)
                .background(Surface)
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                categoryName.ifBlank { tr("catalog_title") },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

@Composable
private fun CategoryProductSkeleton() {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceHigh)
        )
        Spacer(Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHigh)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceHigh)
        )
    }
}

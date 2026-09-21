package com.example.mobile_app.ui.screens.catalog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.Category
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.ui.viewmodel.HomeViewModel
import com.example.mobile_app.util.AppLanguage
import com.example.mobile_app.util.localizedName
import com.example.mobile_app.util.tr

private val BgColor    = Color(0xFFFCF8FF)
private val Surface    = Color(0xFFFFFFFF)
private val SurfaceHigh= Color(0xFFE9E7F2)
private val OnSurface  = Color(0xFF1B1B23)
private val OutlineVar = Color(0xFF767586)
private val PrimaryColor = Color(0xFF2C2ABC)

/**
 * Katalog sahifasi — butun sahifa bo'ylab kategoriyalar grid'i.
 * Mahsulot ro'yxati bu yerda yo'q: kategoriya bosilganda alohida
 * CategoryProducts sahifasi ochiladi.
 */
@Composable
fun CatalogScreen(
    homeViewModel: HomeViewModel? = null,
    notificationViewModel: com.example.mobile_app.ui.viewmodel.NotificationViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {}
) {
    // homeViewModel.categories — bitta source of truth.
    val categories: List<Category> = homeViewModel?.categories?.collectAsState()?.value ?: emptyList()
    val unreadCount by (notificationViewModel?.unreadCount ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0) }).collectAsState()

    // Grid <-> List ko'rinish almashtirgich (burilishlarda ham saqlanadi)
    var isGridView by rememberSaveable { mutableStateOf(true) }

    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isGridView) 2 else 1),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 72.dp, bottom = 24.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "title", contentType = "header_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("catalog_title"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    // Ko'rinish almashtirgich: grid <-> list
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Surface)
                            .clickable { isGridView = !isGridView },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (categories.isEmpty()) {
                // Kategoriyalar hali yuklanmagan — skeleton cell'lar
                items(
                    count = 6,
                    key = { index -> "skeleton_$index" },
                    contentType = { "category_skeleton" }
                ) {
                    if (isGridView) CategoryCardSkeleton() else CategoryRowSkeleton()
                }
            } else {
                items(
                    categories,
                    key = { cat -> cat.id },
                    contentType = { if (isGridView) "category_card" else "category_row" }
                ) { cat ->
                    val onClick = remember(cat.id, onNavigateToCategory) {
                        { onNavigateToCategory(cat.id) }
                    }
                    val name = cat.localizedName(AppLanguage.current)
                    if (isGridView) {
                        CategoryGridCard(
                            name = name,
                            imageUrl = cat.imageUrl,
                            onClick = onClick
                        )
                    } else {
                        CategoryListRow(
                            name = name,
                            imageUrl = cat.imageUrl,
                            onClick = onClick
                        )
                    }
                }
            }
        }

        // ── Sticky Header (on top of everything) ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .shadow(4.dp)
                .background(Surface)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tr("home_logo_m"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Mexa Market", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = PrimaryColor)
                }
                // Notification
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToNotifications() }
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsNone, null,
                        tint = OnSurface, modifier = Modifier.size(24.dp))
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else "$unreadCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                // Search
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToSearch() }
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, null,
                        tint = OnSurface, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── Category Grid Card ───────────────────────────────────────────────────────

@Composable
private fun CategoryGridCard(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            if (imageUrl != null) {
                ProductImage(
                    url = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    cornerRadius = 18.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) { Text("📦", fontSize = 44.sp) }
            }
            Text(
                name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }
    }
}

// ─── Category List Row (list ko'rinishi) ───────────────────────────────────────

@Composable
private fun CategoryListRow(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                ProductImage(
                    url = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.size(64.dp),
                    cornerRadius = 14.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) { Text("📦", fontSize = 28.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OutlineVar,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Category Card Skeleton ───────────────────────────────────────────────────

@Composable
private fun CategoryCardSkeleton() {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(SurfaceHigh)
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(SurfaceHigh)
        )
    }
}

// ─── Category Row Skeleton (list ko'rinishi uchun) ────────────────────────────

@Composable
private fun CategoryRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceHigh)
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHigh)
        )
    }
}

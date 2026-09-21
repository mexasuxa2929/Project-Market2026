package com.example.mobile_app.ui.screens.wishlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.Product
import com.example.mobile_app.data.model.wishlist.WishlistEntryResponse
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.WishlistViewModel
import com.example.mobile_app.util.formatPrice
import com.example.mobile_app.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    onProductClick: (String) -> Unit = {}
) {
    val wishlistState by viewModel.wishlist.collectAsState()
    val favoritedIds by viewModel.favoritedIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = wishlistState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is UiState.Success -> {
                val items = state.data
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FavoriteBorder, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(tr("wishlist_empty_title"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr("wishlist_empty_subtitle"), fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Ixcham sarlavha — kontent tepaga yopishadi (TopAppBar dagi
                        // ikki qavat status-bar bo'shlig'i olib tashlandi).
                        item(key = "header") {
                            Text(
                                tr("wishlist_title"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                        items(items, key = { it.productId }) { entry ->
                            WishlistProductRow(
                                entry = entry,
                                onClick = { onProductClick(entry.productId) },
                                onRemove = { viewModel.removeFromWishlist(entry.productId) }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun WishlistProductRow(
    entry: WishlistEntryResponse,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mahsulotning real rasmi (bo'lmasa — kulrang fon + yurakcha).
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                if (!entry.imageUrl.isNullOrBlank()) {
                    com.example.mobile_app.ui.components.ProductImage(
                        url = entry.imageUrl,
                        contentDescription = entry.productName,
                        modifier = Modifier.size(64.dp),
                        cornerRadius = 10.dp
                    )
                } else {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // ID o'rniga mahsulot nomi (nom bo'lmasa — qisqa ID).
                Text(
                    entry.productName?.takeIf { it.isNotBlank() }
                        ?: (entry.productId.take(12) + "..."),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (entry.addedAt != null) {
                    Text(
                        tr("wishlist_added") + entry.addedAt.take(10),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

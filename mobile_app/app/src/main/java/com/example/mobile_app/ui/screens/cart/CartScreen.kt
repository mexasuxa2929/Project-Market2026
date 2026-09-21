package com.example.mobile_app.ui.screens.cart

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.example.mobile_app.data.model.cart.CartLineResponse
import com.example.mobile_app.data.model.cart.CartResponse
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.CartViewModel
import com.example.mobile_app.util.tr
import com.example.mobile_app.util.formatPrice
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onCheckout: () -> Unit,
    onProductClick: (String) -> Unit = {}
) {
    val cartState by cartViewModel.cart.collectAsState()

    LaunchedEffect(Unit) {
        // Jim sinxronlash: ro'yxat yo'qolmaydi, jamilar yangilanadi
        cartViewModel.refreshCart()
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Summary pastga mahkamlanadi — bottom navigationgacha bo'sh joy qolmaydi
            val state = cartState
            if (state is UiState.Success && state.data.lines.isNotEmpty()) {
                CartSummary(cart = state.data, onCheckout = onCheckout)
            }
        }
    ) { paddingValues ->
        when (val state = cartState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }

            is UiState.Success -> {
                val cart = state.data
                if (cart.lines.isEmpty()) {
                    EmptyCart(modifier = Modifier.padding(paddingValues))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Ixcham sarlavha — kontent tepaga yopishadi (alohida AppBar yo'q)
                        item(key = "header") {
                            Text(
                                tr("cart_title"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                        items(cart.lines, key = { it.productId }) { line ->
                            CartLineItem(
                                line = line,
                                onIncrease = { cartViewModel.updateItem(line.productId, line.quantity + 1) },
                                onDecrease = {
                                    if (line.quantity > 1) cartViewModel.updateItem(line.productId, line.quantity - 1)
                                    else cartViewModel.removeItem(line.productId)
                                },
                                onRemove = { cartViewModel.removeItem(line.productId) },
                                onOpenDetail = { onProductClick(line.productId) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cartViewModel.loadCart() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(tr("orders_retry"), color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartLineItem(
    line: CartLineResponse,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onOpenDetail: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onOpenDetail),
                contentAlignment = Alignment.Center
            ) {
                if (line.imageUrl != null) {
                    ProductImage(
                        url = line.imageUrl,
                        contentDescription = line.productName,
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 10.dp
                    )
                } else {
                    Text("📦", fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDetail)
            ) {
                Text(
                    text = line.productName ?: "${tr("cart_product_prefix")}${line.productId.take(8)}...",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2
                )
                if (line.color != null) {
                    Text(
                        text = line.color,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                if (line.unitPrice != null) {
                    Text(
                        text = "${formatPrice(line.unitPrice)}${tr("cart_unit_price")}",
                        fontSize = 13.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, null, tint = Primary, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${line.quantity}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, null, tint = Primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CartSummary(cart: CartResponse, onCheckout: () -> Unit) {
    val subtotal = cart.lines.sumOf { it.quantity * (it.unitPrice ?: 0.0) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(tr("cart_total_items"), color = TextSecondary)
                Text("${cart.totalQuantity}${tr("cart_quantity_suffix")}", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(tr("cart_type_count"), color = TextSecondary)
                Text("${cart.lineCount}${tr("cart_type_suffix")}", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("checkout_subtotal"), fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                Text(formatPrice(subtotal), fontWeight = FontWeight.Bold, color = Primary, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.ShoppingCartCheckout, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("cart_place_order"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("cart_empty_emoji"), fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(tr("cart_empty_title"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(tr("cart_empty_subtitle"), fontSize = 14.sp, color = TextSecondary)
        }
    }
}

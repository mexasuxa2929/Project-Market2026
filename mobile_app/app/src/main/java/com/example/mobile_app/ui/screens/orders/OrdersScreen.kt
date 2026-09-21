package com.example.mobile_app.ui.screens.orders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.order.OrderResponsePayload
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.ReviewRepository
import com.example.mobile_app.MexaMarketApp
import com.example.mobile_app.ui.components.ReviewBottomSheet
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.StarColor
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.OrderViewModel
import com.example.mobile_app.ui.viewmodel.ReviewViewModel
import com.example.mobile_app.util.tr
import com.example.mobile_app.util.formatPrice
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    orderViewModel: OrderViewModel,
    onOrderClick: (OrderResponsePayload) -> Unit = {}
) {
    val ordersState by orderViewModel.orders.collectAsState()
    val cancellingIds by orderViewModel.cancellingIds.collectAsState()
    val cancelError by orderViewModel.cancelError.collectAsState()
    val isRefreshing by orderViewModel.isRefreshing.collectAsState()
    val isLoadingMore by orderViewModel.isLoadingMore.collectAsState()
    val hasMore by orderViewModel.hasMore.collectAsState()

    // Bekor qilish xatosi — Snackbar (ilgari jim yutilardi)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(cancelError) {
        cancelError?.let {
            snackbarHostState.showSnackbar(it)
            orderViewModel.consumeCancelError()
        }
    }

    // Realtime: order holati o'zgarganda ro'yxatni jimgina yangilash
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                orderViewModel.refreshOrders()
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter(MexaMarketApp.ACTION_ORDER_CHANGED), android.content.Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // ReviewViewModel (lazy create)
    val reviewRepo = remember { ReviewRepository(RetrofitClient.reviewApiService) }
    val reviewVm   = remember { ReviewViewModel(reviewRepo) }
    val submitState by reviewVm.submitState.collectAsState()
    val myReview    by reviewVm.myReview.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    // Qaysi buyurtma/mahsulot uchun review yozilmoqda
    var reviewingOrder by remember { mutableStateOf<OrderResponsePayload?>(null) }

    // BottomSheet
    reviewingOrder?.let { order ->
        val firstItem = order.items?.firstOrNull()
        ReviewBottomSheet(
            productName  = firstItem?.productName ?: tr("orders_default_product"),
            existingReview = myReview,
            submitState  = submitState,
            sheetState   = sheetState,
            onDismiss    = {
                scope.launch { sheetState.hide() }
                reviewingOrder = null
                reviewVm.resetSubmitState()
            },
            onSubmit     = { rating, comment ->
                val productId = firstItem?.productId?.toString() ?: return@ReviewBottomSheet
                reviewVm.submitReview(productId, rating, comment)
            },
        )
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { orderViewModel.refreshOrders() },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        when (val state = ordersState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }

            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(tr("orders_empty_title"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr("orders_empty_subtitle"), fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val totalItems = layoutInfo.totalItemsCount
                            totalItems > 0 && lastVisible >= totalItems - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore && !isLoadingMore && hasMore) {
                            orderViewModel.loadNextPage()
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "orders_header") {
                            Text(
                                tr("orders_title"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(state.data, key = { it.id }) { order ->
                            OrderCard(
                                order    = order,
                                onClick  = { onOrderClick(order) },
                                isCancelling = order.id in cancellingIds,
                                onCancel = {
                                    orderViewModel.cancelOrder(order.id)
                                },
                                onReview = {
                                    val pid = order.items?.firstOrNull()?.productId?.toString()
                                    if (pid != null) {
                                        reviewVm.loadMyReview(pid)
                                        reviewingOrder = order
                                        scope.launch { sheetState.show() }
                                    }
                                }
                            )
                        }
                        if (isLoadingMore) {
                            item(key = "load_more") {
                                Box(
                                    Modifier.fillMaxWidth().padding(12.dp),
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
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { orderViewModel.loadOrders() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text(tr("orders_retry")) }
                    }
                }
            }
        }
        } // PullToRefreshBox
    }
}

// ─── Order karta ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderCard(
    order: OrderResponsePayload,
    onClick: () -> Unit,
    isCancelling: Boolean,
    onCancel: () -> Unit,
    onReview: () -> Unit,
) {
    val isDelivered = order.status.uppercase() == "DELIVERED"
    val statusColor = orderStatusColor(order.status)
    val statusLabel = orderStatusLabel(order.status)

    Card(
        onClick  = onClick,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Sarlavha + holat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tr("orders_number_prefix") + (order.orderNumber ?: order.id.take(8)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Jami summa
            if (order.totalAmount != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("orders_total"), color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "${formatPrice(order.totalAmount)} ${order.currency ?: ""}",
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Mahsulotlar soni
            if (!order.items.isNullOrEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("orders_products"), color = TextSecondary, fontSize = 13.sp)
                    Text("${order.items.sumOf { it.quantity }}${tr("orders_items_count_suffix")}", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Sana
            if (order.createdAt != null) {
                Text(tr("orders_date") + order.createdAt.take(10), color = TextSecondary, fontSize = 12.sp)
            }

            // Baho berish tugmasi (faqat DELIVERED holat uchun)
            if (isDelivered) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = onReview,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = StarColor),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, StarColor.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Default.Star, null, tint = StarColor, modifier = Modifier.size(16.dp))
                    Text(tr("orders_review"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Bekor qilish tugmasi (faol holatlarda)
            if (orderCancellable(order.status)) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = onCancel,
                    enabled  = !isCancelling,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFEF4444), strokeWidth = 2.dp)
                    } else {
                        Text(tr("orders_cancel"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

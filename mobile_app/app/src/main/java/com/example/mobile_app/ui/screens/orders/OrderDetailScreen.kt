package com.example.mobile_app.ui.screens.orders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.ui.components.ReviewBottomSheet
import com.example.mobile_app.data.model.order.OrderItemPayload
import com.example.mobile_app.data.model.order.OrderResponsePayload
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.ReviewRepository
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
fun OrderDetailScreen(
    orderId: String,
    orderViewModel: OrderViewModel,
    onBack: () -> Unit
) {
    val detailState by orderViewModel.orderDetail.collectAsState()
    val cancellingIds by orderViewModel.cancellingIds.collectAsState()
    val cancelError by orderViewModel.cancelError.collectAsState()

    LaunchedEffect(orderId) { orderViewModel.loadOrderDetail(orderId) }

    // Bekor qilish xatosi — Snackbar (ilgari jim yutilardi)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(cancelError) {
        cancelError?.let {
            snackbarHostState.showSnackbar(it)
            orderViewModel.consumeCancelError()
        }
    }

    // Har bir mahsulotga alohida review (ro'yxatda faqat birinchisiga qo'yilardi)
    val reviewRepo = remember { ReviewRepository(RetrofitClient.reviewApiService) }
    val reviewVm = remember { ReviewViewModel(reviewRepo) }
    val submitState by reviewVm.submitState.collectAsState()
    val myReview by reviewVm.myReview.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var reviewingItem by remember { mutableStateOf<OrderItemPayload?>(null) }

    reviewingItem?.let { item ->
        ReviewBottomSheet(
            productName = item.productName ?: tr("orders_default_product"),
            existingReview = myReview,
            submitState = submitState,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }
                reviewingItem = null
                reviewVm.resetSubmitState()
            },
            onSubmit = { rating, comment ->
                reviewVm.submitReview(item.productId, rating, comment)
            },
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("order_detail_title"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("checkout_back"),
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        when (val state = detailState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { orderViewModel.loadOrderDetail(orderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text(tr("orders_retry")) }
                    }
                }
            }

            is UiState.Success -> {
                val order = state.data
                val statusColor = orderStatusColor(order.status)
                val statusLabel = orderStatusLabel(order.status)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        tr("orders_number_prefix") + (order.orderNumber ?: order.id.take(8)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
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
                                if (order.createdAt != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(tr("orders_date") + order.createdAt.take(16).replace("T", " "), color = TextSecondary, fontSize = 12.sp)
                                }
                                if (order.deliveryAddress != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        tr("order_detail_delivery_address") + order.deliveryAddress,
                                        color = Color(0xFF374151),
                                        fontSize = 13.sp
                                    )
                                }
                                if (order.cancelReason != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        tr("order_detail_cancel_reason") + order.cancelReason,
                                        color = Color(0xFFEF4444),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    order.items?.let { items ->
                        item {
                            Text(tr("order_detail_items"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                        }
                        val isDelivered = order.status.uppercase() == "DELIVERED"
                        items(items, key = { it.id }) { item ->
                            OrderDetailItemRow(
                                item = item,
                                showReview = isDelivered,
                                onReview = {
                                    reviewVm.loadMyReview(item.productId)
                                    reviewingItem = item
                                    scope.launch { sheetState.show() }
                                }
                            )
                        }
                    }

                    item {
                        OrderTotalsCard(order)
                        if (orderCancellable(order.status)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick  = { orderViewModel.cancelOrder(order.id) },
                                enabled  = order.id !in cancellingIds,
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                            ) {
                                if (order.id in cancellingIds) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFEF4444), strokeWidth = 2.dp)
                                } else {
                                    Text(tr("orders_cancel"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailItemRow(
    item: OrderItemPayload,
    showReview: Boolean = false,
    onReview: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl != null) {
                    ProductImage(
                        url = item.imageUrl,
                        contentDescription = item.productName,
                        modifier = Modifier.size(52.dp),
                        cornerRadius = 10.dp
                    )
                } else {
                    Text("📦", fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName ?: item.productId.take(8),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    color = Color(0xFF1F2937)
                )
                if (item.unitPrice != null) {
                    Text(
                        text = "${item.quantity} × ${formatPrice(item.unitPrice)}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
            if (item.subtotal != null) {
                Text(
                    text = "${formatPrice(item.subtotal)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
            }
            }
            // Har bir mahsulotga alohida baho (faqat yetkazilgan orderda)
            if (showReview) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StarColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StarColor.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Default.Star, null, tint = StarColor, modifier = Modifier.size(16.dp))
                    Text(tr("orders_review"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun OrderTotalsCard(order: OrderResponsePayload) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            order.subtotal?.let {
                TotalRow(tr("checkout_subtotal"), "${formatPrice(it)}")
            }
            order.deliveryFee?.let {
                Spacer(modifier = Modifier.height(4.dp))
                TotalRow(
                    tr("checkout_shipping_fee"),
                    if (it <= 0.0) tr("checkout_free") else "${formatPrice(it)}"
                )
            }
            order.totalAmount?.let {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                TotalRow(tr("checkout_total"), "${formatPrice(it)}", bold = true)
            }
            order.paymentStatus?.let { payment ->
                Spacer(modifier = Modifier.height(4.dp))
                TotalRow(tr("order_detail_payment_status"), paymentLabel(payment))
            }
            order.paymentMethod?.let { method ->
                Spacer(modifier = Modifier.height(4.dp))
                TotalRow(tr("order_payment_method"), paymentMethodLabel(method))
            }
            order.estimatedDeliveryDays?.let { days ->
                Spacer(modifier = Modifier.height(4.dp))
                TotalRow(tr("order_eta_label"), "$days ${tr("order_eta_days")}")
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (bold) Color(0xFF1F2937) else TextSecondary,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = if (bold) Primary else Color(0xFF1F2937),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun paymentLabel(status: String): String = when (status.uppercase()) {
    "PAID"      -> tr("order_payment_paid")
    "REFUNDED"  -> tr("order_payment_refunded")
    else        -> tr("order_payment_unpaid")
}

@Composable
private fun paymentMethodLabel(method: String): String = when (method.uppercase()) {
    "CASH"   -> tr("pay_cash")
    "CARD"   -> tr("pay_card")
    "ONLINE" -> tr("pay_online")
    else     -> method
}
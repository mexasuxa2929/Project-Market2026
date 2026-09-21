package com.example.mobile_app.ui.screens.checkout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.mobile_app.ui.theme.appTextFieldColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import com.example.mobile_app.ui.components.ProductImage
import com.example.mobile_app.data.model.cart.CartLineResponse
import com.example.mobile_app.data.model.order.CreateOrderItemRequest
import com.example.mobile_app.data.model.order.CreateOrderRequest
import com.example.mobile_app.ui.state.UiState
import com.example.mobile_app.ui.theme.Background
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.ui.viewmodel.CartViewModel
import com.example.mobile_app.ui.viewmodel.OrderViewModel
import com.example.mobile_app.ui.viewmodel.AddressViewModel
import com.example.mobile_app.util.tr
import com.example.mobile_app.util.formatPrice
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    orderViewModel: OrderViewModel,
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit = {}
) {
    val cartState by cartViewModel.cart.collectAsState()
    val placing by orderViewModel.placing.collectAsState()
    val placeError by orderViewModel.placeError.collectAsState()

    val addressViewModel: AddressViewModel = viewModel(factory = AddressViewModel.Factory)
    val addressesState by addressViewModel.addresses.collectAsState()
    val savedAddresses = (addressesState as? UiState.Success)?.data.orEmpty()

    var address by remember { mutableStateOf("") }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var discountCode by remember { mutableStateOf("") }
    // To'lov usuli: CASH | CARD | ONLINE — backend order'ga saqlaydi
    var paymentMethod by remember { mutableStateOf("CASH") }
    val hasAddress = selectedAddressId != null || address.trim().isNotBlank()

    // Tanlangan manzil xizmat hududidami (null = tekshirilmagan/noma'lum).
    var addressServed by remember { mutableStateOf<Boolean?>(null) }
    var checkingService by remember { mutableStateOf(false) }
    val checkoutScope = androidx.compose.runtime.rememberCoroutineScope()
    fun recheckServiceability(id: String?) {
        val sel = savedAddresses.firstOrNull { it.id == id }
        val lat = sel?.latitude
        val lng = sel?.longitude
        if (id == null || lat == null || lng == null) {
            addressServed = null
            checkingService = false
            return
        }
        checkingService = true
        addressServed = null
        checkoutScope.launch {
            addressServed = try {
                addressViewModel.checkPoint(lat, lng)
            } catch (_: Exception) {
                null
            }
            checkingService = false
        }
    }

    // Asosiy manzil (switch) avtomatik tanlanadi — qo'lda tanlash shart emas
    androidx.compose.runtime.LaunchedEffect(savedAddresses) {
        if (selectedAddressId == null && address.isBlank()) {
            savedAddresses.firstOrNull { it.defaultAddress == true }?.let { def ->
                selectedAddressId = def.id
                address = def.displayName
            }
        }
        recheckServiceability(selectedAddressId)
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("checkout_title"),
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
        },
        bottomBar = {
            val state = cartState
            if (state is UiState.Success && state.data.lines.isNotEmpty()) {
                val subtotal = state.data.lines.sumOf { it.quantity * (it.unitPrice ?: 0.0) }
                Card(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Button(
                        onClick = {
                            orderViewModel.placeOrder(
                                request = CreateOrderRequest(
                                    deliveryAddressId = selectedAddressId,
                                    deliveryAddress = address.trim().ifBlank { null },
                                    note = note.trim().ifBlank { null },
                                    discountCode = discountCode.trim().ifBlank { null },
                                    paymentMethod = paymentMethod,
                                    items = state.data.lines.map {
                                        CreateOrderItemRequest(productId = it.productId, quantity = it.quantity)
                                    }
                                ),
                                onSuccess = {
                                    cartViewModel.clearCart()
                                    onOrderPlaced()
                                }
                            )
                        },
                        enabled = !placing && hasAddress && addressServed != false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (placing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "${tr("checkout_place_order")} · ${formatPrice(subtotal)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
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

            is UiState.Success -> {
                val cart = state.data
                if (cart.lines.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗑️", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(tr("checkout_empty_title"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    val subtotal = cart.lines.sumOf { it.quantity * (it.unitPrice ?: 0.0) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Shipping Address
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = tr("checkout_shipping"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // Saqlangan manzillar holati: loading/error endi ko'rinadi
                                when (addressesState) {
                                    is UiState.Loading -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Primary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tr("catalog_loading"),
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    is UiState.Error -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = (addressesState as UiState.Error).message,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = { addressViewModel.load() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text(tr("orders_retry"), color = Color.White, fontSize = 13.sp) }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    else -> Unit
                                }
                                if (savedAddresses.isNotEmpty()) {
                                    Text(
                                        text = tr("checkout_address_select"),
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    savedAddresses.forEach { saved ->
                                        val selected = saved.id == selectedAddressId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) Primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
                                                .border(
                                                    if (selected) 1.dp else 0.dp,
                                                    Primary,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    selectedAddressId = saved.id
                                                    address = saved.displayName
                                                    recheckServiceability(saved.id)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                null,
                                                tint = if (selected) Primary else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = saved.label?.ifBlank { tr("addresses_other") } ?: tr("addresses_other"),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1F2937)
                                                )
                                                Text(
                                                    text = saved.displayName.ifBlank { "—" },
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = {
                                        address = it
                                        if (selectedAddressId != null) selectedAddressId = null
                                        addressServed = null
                                        checkingService = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = appTextFieldColors(),
                                    placeholder = { Text(tr("checkout_address_hint"), fontSize = 14.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                // Xizmat hududi statusi — user yetkazib berilishini oldindan biladi.
                                Spacer(modifier = Modifier.height(8.dp))
                                when {
                                    checkingService -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Primary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tr("checkout_service_checking"),
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    addressServed == true -> {
                                        ServiceBanner(
                                            served = true,
                                            text = tr("checkout_service_ok")
                                        )
                                    }
                                    addressServed == false -> {
                                        ServiceBanner(
                                            served = false,
                                            text = tr("checkout_service_no")
                                        )
                                    }
                                }
                            }
                        }

                        // Section 1.5: Izoh
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = tr("checkout_note_title"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = appTextFieldColors(),
                                    placeholder = { Text(tr("checkout_note_hint"), fontSize = 14.sp, color = TextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Section 1.6: Chegirma kodi
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = tr("checkout_discount_code"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = discountCode,
                                    onValueChange = { discountCode = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = appTextFieldColors(),
                                    placeholder = { Text(tr("checkout_discount_code_hint"), fontSize = 14.sp, color = TextSecondary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Section 1.7: To'lov usuli
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = tr("checkout_payment_method"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PaymentOption(
                                        label = tr("pay_cash"),
                                        selected = paymentMethod == "CASH",
                                        onClick = { paymentMethod = "CASH" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PaymentOption(
                                        label = tr("pay_card"),
                                        selected = paymentMethod == "CARD",
                                        onClick = { paymentMethod = "CARD" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PaymentOption(
                                        label = tr("pay_online"),
                                        selected = paymentMethod == "ONLINE",
                                        onClick = { paymentMethod = "ONLINE" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Section 2: Mahsulotlar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = tr("checkout_items"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                cart.lines.forEach { line ->
                                    CheckoutLineItem(line)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }

                        // Section 3: Order Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = tr("checkout_order_summary"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tr("checkout_subtotal"),
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${formatPrice(subtotal)}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF1F2937),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tr("checkout_shipping_fee"),
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = tr("checkout_free"),
                                        fontSize = 14.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr("checkout_total"),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Text(
                                        text = "${formatPrice(subtotal)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                        }

                        placeError?.let { error ->
                            Text(
                                text = error,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceBanner(served: Boolean, text: String) {
    val bg = if (served) Color(0xFFE8F8EF) else Color(0xFFFDECEC)
    val fg = if (served) Color(0xFF16A34A) else Color(0xFFEF4444)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (served) "✓ " else "✕ ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun PaymentOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
            .border(
                if (selected) 1.dp else 0.dp,
                Primary,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Primary else TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CheckoutLineItem(line: CartLineResponse) {
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
            if (line.imageUrl != null) {
                ProductImage(
                    url = line.imageUrl,
                    contentDescription = line.productName,
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
                text = line.productName ?: line.productId.take(8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                color = Color(0xFF1F2937)
            )
            if (line.color != null) {
                Text(text = line.color, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${line.quantity} × ${formatPrice(line.unitPrice ?: 0.0)}",
            fontSize = 13.sp,
            color = Primary,
            fontWeight = FontWeight.Medium
        )
    }
}
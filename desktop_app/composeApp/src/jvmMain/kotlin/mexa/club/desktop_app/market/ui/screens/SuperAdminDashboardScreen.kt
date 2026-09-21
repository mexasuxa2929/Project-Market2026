package mexa.club.desktop_app.market.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.presentation.DashboardScreenModel
import org.koin.compose.koinInject
import mexa.club.desktop_app.market.session.MarketSessionGate
import mexa.club.desktop_app.market.ui.components.DashboardBentoStatCard
import mexa.club.desktop_app.market.ui.components.RecentOrdersTableCard
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private fun formatStatDigits(raw: String): String {
    if (raw == "—") return raw
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return raw
    val n = digits.toLongOrNull() ?: return raw
    return n.toString().reversed().chunked(3).joinToString(" ").reversed()
}

@Composable
fun SuperAdminDashboardScreen(modifier: Modifier = Modifier) {
    val vm: DashboardScreenModel = koinInject()
    val ui by vm.state.collectAsState()

    LaunchedEffect(vm) {
        vm.startAfterSessionReady()
    }

    LaunchedEffect(vm) {
        MarketSessionGate.awaitReady()
        GatewayRealtimeHub.dashboardRefresh.collect {
            vm.onRealtimeRefreshTick()
        }
    }

    Box(modifier.fillMaxSize()) {
        when {
            ui.loading ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp),
                ) {
                    SuperAdminDashboardSkeletonContent(Modifier.fillMaxWidth())
                }

            ui.error != null ->
                DashboardEmptyErrorContent(
                    isError = true,
                    message = ui.error ?: "",
                    onRetry = { vm.load(silent = false) },
                    onClearFilter = { vm.load(silent = false) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp),
                )

            else ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Header row: title + action buttons (wraps on narrow screens)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val compact = maxWidth < 680.dp
                        if (compact) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text(
                                        "Boshqaruv Paneli",
                                        fontSize = 24.sp,
                                        lineHeight = 32.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.2).sp,
                                        color = MexaWarehouseColors.onSurface,
                                    )
                                    Text(
                                        "Bugungi ombor holati va faoliyat ko'rsatkichlari.",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MexaWarehouseColors.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {},
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.onSurface),
                                    ) {
                                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MexaWarehouseColors.onSurface)
                                    }
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary, contentColor = Color.White),
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "Boshqaruv Paneli",
                                        fontSize = 24.sp,
                                        lineHeight = 32.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.2).sp,
                                        color = MexaWarehouseColors.onSurface,
                                    )
                                    Text(
                                        "Bugungi ombor holati va faoliyat ko'rsatkichlari.",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MexaWarehouseColors.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = {},
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.onSurface),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MexaWarehouseColors.onSurface)
                                            Text("Oxirgi 30 kun", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                    }
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary, contentColor = Color.White),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                                            Text("Hisobot yuklash", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Stat cards — 4-in-a-row on wide screens, 2×2 on narrow
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val twoColumns = maxWidth < 720.dp
                        val cardGap = if (twoColumns) 16.dp else 24.dp
                        if (twoColumns) {
                            Column(verticalArrangement = Arrangement.spacedBy(cardGap)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(cardGap),
                                ) {
                                    DashboardBentoStatCard(
                                        label = "Jami foydalanuvchilar",
                                        value = formatStatDigits(ui.userCount),
                                        icon = Icons.Filled.SupervisorAccount,
                                        iconTint = MexaWarehouseColors.primary,
                                        iconBackground = MexaWarehouseColors.primary.copy(alpha = 0.1f),
                                        badgeText = "+3.2%",
                                        badgeBackground = MexaWarehouseColors.greenBadgeBg,
                                        badgeForeground = MexaWarehouseColors.greenBadgeFg,
                                        modifier = Modifier.weight(1f),
                                    )
                                    DashboardBentoStatCard(
                                        label = "Jami omborlar",
                                        value = formatStatDigits(ui.warehouseCount),
                                        icon = Icons.Filled.Inventory2,
                                        iconTint = Color(0xFF16A34A),
                                        iconBackground = MexaWarehouseColors.greenBadgeBg,
                                        badgeText = "Faol",
                                        badgeBackground = MexaWarehouseColors.greenBadgeBg,
                                        badgeForeground = MexaWarehouseColors.greenBadgeFg,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(cardGap),
                                ) {
                                    DashboardBentoStatCard(
                                        label = "Jami mahsulotlar",
                                        value = formatStatDigits(ui.productCount),
                                        icon = Icons.Filled.Inventory,
                                        iconTint = Color(0xFF2563EB),
                                        iconBackground = MexaWarehouseColors.blueBadgeBg,
                                        badgeText = "+24 yangi",
                                        badgeBackground = MexaWarehouseColors.blueBadgeBg,
                                        badgeForeground = MexaWarehouseColors.blueBadgeFg,
                                        modifier = Modifier.weight(1f),
                                    )
                                    DashboardBentoStatCard(
                                        label = "Bugungi buyurtmalar",
                                        value = formatStatDigits(ui.todayOrders),
                                        icon = Icons.Filled.ShoppingCart,
                                        iconTint = Color(0xFFD97706),
                                        iconBackground = MexaWarehouseColors.amberBadgeBg,
                                        badgeText = "+12%",
                                        badgeBackground = MexaWarehouseColors.amberBadgeBg,
                                        badgeForeground = MexaWarehouseColors.amberBadgeFg,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(cardGap),
                            ) {
                                DashboardBentoStatCard(
                                    label = "Jami foydalanuvchilar",
                                    value = formatStatDigits(ui.userCount),
                                    icon = Icons.Filled.SupervisorAccount,
                                    iconTint = MexaWarehouseColors.primary,
                                    iconBackground = MexaWarehouseColors.primary.copy(alpha = 0.1f),
                                    badgeText = "+3.2%",
                                    badgeBackground = MexaWarehouseColors.greenBadgeBg,
                                    badgeForeground = MexaWarehouseColors.greenBadgeFg,
                                    modifier = Modifier.weight(1f),
                                )
                                DashboardBentoStatCard(
                                    label = "Jami omborlar",
                                    value = formatStatDigits(ui.warehouseCount),
                                    icon = Icons.Filled.Inventory2,
                                    iconTint = Color(0xFF16A34A),
                                    iconBackground = MexaWarehouseColors.greenBadgeBg,
                                    badgeText = "Faol",
                                    badgeBackground = MexaWarehouseColors.greenBadgeBg,
                                    badgeForeground = MexaWarehouseColors.greenBadgeFg,
                                    modifier = Modifier.weight(1f),
                                )
                                DashboardBentoStatCard(
                                    label = "Jami mahsulotlar",
                                    value = formatStatDigits(ui.productCount),
                                    icon = Icons.Filled.Inventory,
                                    iconTint = Color(0xFF2563EB),
                                    iconBackground = MexaWarehouseColors.blueBadgeBg,
                                    badgeText = "+24 yangi",
                                    badgeBackground = MexaWarehouseColors.blueBadgeBg,
                                    badgeForeground = MexaWarehouseColors.blueBadgeFg,
                                    modifier = Modifier.weight(1f),
                                )
                                DashboardBentoStatCard(
                                    label = "Bugungi buyurtmalar",
                                    value = formatStatDigits(ui.todayOrders),
                                    icon = Icons.Filled.ShoppingCart,
                                    iconTint = Color(0xFFD97706),
                                    iconBackground = MexaWarehouseColors.amberBadgeBg,
                                    badgeText = "+12%",
                                    badgeBackground = MexaWarehouseColors.amberBadgeBg,
                                    badgeForeground = MexaWarehouseColors.amberBadgeFg,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    RecentOrdersTableCard(
                        rows = ui.orderRows,
                        page = ui.orderPage,
                        pageSize = ui.orderPageSize,
                        totalElements = ui.totalOrders.toIntOrNull() ?: 0,
                        onPageChange = { vm.setOrderPage(it) },
                        onPageSizeChange = { vm.setOrderPageSize(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
        }

        if (ui.loading) {
            DashboardFloatingLoadingToast(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            )
        }
    }
}

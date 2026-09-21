package mexa.club.desktop_app.market.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.components.ShimmerBox
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DashboardFloatingLoadingToast(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MexaWarehouseColors.surfaceLowest,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MexaWarehouseColors.primary,
            )
            Text(
                "Ma'lumotlar yuklanmoqda...",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MexaWarehouseColors.onSurface,
            )
        }
    }
}

@Composable
fun SuperAdminDashboardSkeletonContent(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Dashboard Umumiy Holati",
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.onSurface,
                )
                Text(
                    "Sizning omboringiz va tranzaksiyalaringiz haqida qisqacha ma'lumot",
                    fontSize = 14.sp,
                    color = MexaWarehouseColors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier
                        .alpha(0.5f)
                        .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp))
                        .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MexaWarehouseColors.onSurfaceVariant)
                    Text("Oxirgi 30 kun", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.Filled.ExpandMore,
                        null,
                        tint = MexaWarehouseColors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Row(
                    Modifier
                        .alpha(0.5f)
                        .background(MexaWarehouseColors.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        "Hisobot yuklash",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            repeat(4) {
                MetricSkeletonCard(Modifier.weight(1f))
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(400.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ChartSkeleton(Modifier.weight(2f))
            ActivitySkeleton(Modifier.weight(1f))
        }

        OrdersTableSkeleton(Modifier.fillMaxWidth())
    }
}

@Composable
private fun MetricSkeletonCard(modifier: Modifier) {
    Surface(
        modifier = modifier
            .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                ShimmerBox(Modifier.fillMaxWidth(0.5f).height(16.dp))
                Spacer(Modifier.height(16.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.65f).height(32.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.35f).height(12.dp))
            }
            ShimmerBox(Modifier.size(48.dp), RoundedCornerShape(8.dp))
        }
    }
}

@Composable
private fun ChartSkeleton(modifier: Modifier) {
    Surface(
        modifier
            .fillMaxHeight()
            .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ShimmerBox(Modifier.width(140.dp).height(24.dp))
                ShimmerBox(Modifier.width(72.dp).height(24.dp))
            }
            Spacer(Modifier.height(32.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val heights = listOf(48, 96, 64, 112, 80, 128, 88, 72, 104, 96)
                heights.forEach { h ->
                    ShimmerBox(
                        Modifier
                            .weight(1f)
                            .height(h.dp),
                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivitySkeleton(modifier: Modifier) {
    Surface(
        modifier
            .fillMaxHeight()
            .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(24.dp),
        ) {
            ShimmerBox(Modifier.fillMaxWidth(0.45f).height(24.dp))
            Spacer(Modifier.height(24.dp))
            repeat(4) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ShimmerBox(Modifier.size(40.dp), CircleShape)
                    Column(Modifier.weight(1f)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.75f).height(16.dp))
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.5f).height(12.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OrdersTableSkeleton(modifier: Modifier) {
    Surface(
        modifier = modifier.border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MexaWarehouseColors.tableHeaderBg)
                    .border(1.dp, MexaWarehouseColors.outlineVariant)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Oxirgi Buyurtmalar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.onSurface,
                )
                ShimmerBox(Modifier.width(96.dp).height(16.dp), RoundedCornerShape(4.dp))
            }
            val headers = listOf("Buyurtma ID", "Mijoz", "Sana", "Status", "Summa", "Amallar")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MexaWarehouseColors.tableHeaderBg)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                headers.forEachIndexed { i, h ->
                    Text(
                        h.uppercase(),
                        modifier = Modifier.weight(if (i == 5) 0.7f else 1f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.outline,
                    )
                }
            }
            repeat(5) { idx ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBox(Modifier.weight(1f).height(16.dp))
                    Spacer(Modifier.width(8.dp))
                    ShimmerBox(Modifier.weight(1f).height(16.dp))
                    Spacer(Modifier.width(8.dp))
                    ShimmerBox(Modifier.weight(1f).height(16.dp))
                    Spacer(Modifier.width(8.dp))
                    ShimmerBox(Modifier.weight(1f).height(24.dp), RoundedCornerShape(999.dp))
                    Spacer(Modifier.width(8.dp))
                    ShimmerBox(Modifier.weight(1f).height(16.dp))
                    Box(Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
                        ShimmerBox(Modifier.size(32.dp), CircleShape)
                    }
                }
                if (idx < 4) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MexaWarehouseColors.outlineVariant.copy(alpha = 0.35f)),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShimmerBox(Modifier.width(192.dp).height(16.dp).alpha(0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        ShimmerBox(Modifier.size(32.dp), RoundedCornerShape(8.dp))
                    }
                }
            }
        }
    }
}

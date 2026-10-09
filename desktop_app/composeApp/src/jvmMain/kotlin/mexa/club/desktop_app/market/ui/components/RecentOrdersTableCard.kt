package mexa.club.desktop_app.market.ui.components
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage


import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import kotlin.math.min

data class RecentOrderTableRow(
    val orderCode: String,
    val customerLine: String,
    val amountDisplay: String,
    val statusRaw: String,
    val timeDisplay: String,
)

fun formatDashboardOrderAmount(raw: String): String {
    val t = raw.trim()
    if (t.isEmpty()) return "—"
    val dec = t.toBigDecimalOrNull() ?: return "$t UZS"
    val plain = dec.stripTrailingZeros().toPlainString()
    val parts = plain.split('.')
    val whole = parts[0].let { w ->
        val sign = if (w.startsWith("-")) "-" else ""
        val d = w.removePrefix("-")
        sign + d.reversed().chunked(3).joinToString(" ").reversed()
    }
    val frac = parts.getOrNull(1)
    return if (frac != null && frac.isNotEmpty()) "$whole.$frac UZS" else "$whole UZS"
}

fun formatDashboardOrderTime(iso: String): String {
    val s = iso.trim()
    if (s.isEmpty()) return "—"
    return runCatching {
        val parsed = OffsetDateTime.parse(s)
        val z = parsed.atZoneSameInstant(ZoneId.systemDefault())
        z.format(DateTimeFormatter.ofPattern("HH:mm / d MMM", Locale.ENGLISH))
    }.getOrElse { s }
}

fun buildRecentOrderRowFromApi(
    orderNumber: String,
    userId: String,
    totalAmount: String,
    status: String,
    createdAt: String,
): RecentOrderTableRow {
    val code = orderNumber.trim().let { if (it.startsWith("#")) it else "#$it" }
    val customer = when {
        userId.isBlank() -> ""
        else -> "Foydalanuvchi · ${userId.take(8)}"
    }
    return RecentOrderTableRow(
        orderCode = code,
        customerLine = customer,
        amountDisplay = formatDashboardOrderAmount(totalAmount),
        statusRaw = status,
        timeDisplay = formatDashboardOrderTime(createdAt),
    )
}

@Composable
private fun OrderStatusPill(statusRaw: String) {
    val key = statusRaw.uppercase(Locale.getDefault())
    val triple = when {
        key.contains("DELIVER") -> Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "Yetkazildi")
        key.contains("PROCESS") || key.contains("SHIP") || key == "CONFIRMED" -> Triple(MexaWarehouseColors.infoBadgeBg, MexaWarehouseColors.indigoAccent, "Jarayonda")
        key.contains("PEND") || key == "CREATED" || key == "NEW" -> Triple(MexaWarehouseColors.amberBadgeBg, Color(0xFFB45309), "Kutilmoqda")
        key.contains("CANCEL") || key.contains("REFUND") -> Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "Bekor qilindi")
        else -> Triple(MexaWarehouseColors.borderSubtle, MexaWarehouseColors.textMuted, statusRaw.ifBlank { "—" })
    }
    val (bg, fg, label) = triple
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
fun RecentOrdersTableCard(
    rows: List<RecentOrderTableRow>,
    page: Int = 0,
    pageSize: Int = 5,
    totalElements: Int = 0,
    onPageChange: (Int) -> Unit = {},
    onPageSizeChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    onViewAll: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest)
            .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MexaWarehouseColors.outlineVariant)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "So'nggi buyurtmalar",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MexaWarehouseColors.onSurface,
            )
            Row(
                Modifier.clickable { onViewAll() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(AppStrings.allOrders(appLanguage()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.primary,
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MexaWarehouseColors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MexaWarehouseColors.tableHeaderBg)
                    .padding(vertical = 12.dp, horizontal = 24.dp),
            ) {
                listOf("Buyurtma №", "Summa", "Holat", "Vaqt", "").forEachIndexed { i, h ->
                    val w = when (i) {
                        0 -> 2.2f
                        4 -> 0.4f
                        else -> 1f
                    }
                    if (h.isEmpty()) {
                        Spacer(Modifier.weight(w))
                    } else {
                        Text(
                            h.uppercase(Locale.getDefault()),
                            modifier = Modifier.weight(w),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MexaWarehouseColors.outline,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MexaWarehouseColors.outlineVariant),
            )
            rows.forEachIndexed { idx, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest
                            else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f),
                        )
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(2.2f)) {
                        Text(
                            row.orderCode,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.onSurface,
                        )
                        if (row.customerLine.isNotBlank()) {
                            Text(
                                row.customerLine,
                                fontSize = 12.sp,
                                color = MexaWarehouseColors.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        row.amountDisplay,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.onSurface,
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        OrderStatusPill(row.statusRaw)
                    }
                    Text(
                        row.timeDisplay,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = MexaWarehouseColors.onSurfaceVariant,
                    )
                    Box(Modifier.weight(0.4f), contentAlignment = Alignment.CenterEnd) {
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = MexaWarehouseColors.outline,
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MexaWarehouseColors.outlineVariant.copy(alpha = 0.5f)),
                )
            }
        }
        HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
        run {
            val from = if (totalElements == 0) 0 else page * pageSize + 1
            val to = minOf((page + 1) * pageSize, totalElements)
            PaginationBar(
                page = page,
                pageSize = pageSize,
                totalElements = totalElements,
                pageSizeOptions = listOf(5, 10, 20, 50),
                infoText = "$from - $to / $totalElements ta buyurtma",
                onPageChange = onPageChange,
                onPageSizeChange = onPageSizeChange,
            )
        }
    }
}

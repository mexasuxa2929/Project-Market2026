package mexa.club.desktop_app.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Umumiy paginatsiya paneli (Products sahifasi andozasi) ────────────────────
// Chapda "from - to / total", o'ngda "QATORLAR:" + ⏮ ‹ raqamlar › ⏭.
// page — 0-based. Hamma list ekranlarda bir xil ko'rinish uchun ishlatiladi.

@Composable
fun PaginationBar(
    page: Int,
    pageSize: Int,
    totalElements: Int,
    totalPages: Int = if (totalElements == 0) 1 else (totalElements + pageSize - 1) / pageSize,
    pageSizeOptions: List<Int> = listOf(10, 20, 50, 100),
    /** Chapdagi matn. null bo'lsa standart "from - to / total" ko'rinadi. */
    infoText: String? = null,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sizeExpanded by remember { mutableStateOf(false) }
    val safePages = totalPages.coerceAtLeast(1)
    val safePage = page.coerceIn(0, safePages - 1)
    val from = if (totalElements == 0) 0 else safePage * pageSize + 1
    val to = minOf((safePage + 1) * pageSize, totalElements)

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            infoText ?: "$from - $to / $totalElements",
            fontSize = 13.sp,
            color = MexaWarehouseColors.textMuted,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Page size selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "QATORLAR:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textMuted,
                )
                Box {
                    Surface(
                        onClick = { sizeExpanded = true },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                        color = MexaWarehouseColors.surfaceLowest,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                pageSize.toString(),
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MexaWarehouseColors.textMuted,
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = sizeExpanded,
                        onDismissRequest = { sizeExpanded = false },
                    ) {
                        pageSizeOptions.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.toString(), fontSize = 13.sp) },
                                onClick = { onPageSizeChange(s); sizeExpanded = false },
                            )
                        }
                    }
                }
            }

            // Page nav
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PaginationNavBtn("⏮", safePage > 0) { onPageChange(0) }
                PaginationNavBtn("‹", safePage > 0) { onPageChange(safePage - 1) }

                // Visible page numbers (window of 5)
                val half = 2
                val start = (safePage - half).coerceAtLeast(0)
                val end = (safePage + half).coerceAtMost(safePages - 1)

                if (start > 0) {
                    PaginationNumBtn(1, safePage == 0) { onPageChange(0) }
                    if (start > 1) PaginationEllipsis()
                }
                for (p in start..end) {
                    PaginationNumBtn(p + 1, p == safePage) { onPageChange(p) }
                }
                if (end < safePages - 1) {
                    if (end < safePages - 2) PaginationEllipsis()
                    PaginationNumBtn(safePages, safePage == safePages - 1) { onPageChange(safePages - 1) }
                }

                PaginationNavBtn("›", safePage < safePages - 1) { onPageChange(safePage + 1) }
                PaginationNavBtn("⏭", safePage < safePages - 1) { onPageChange(safePages - 1) }
            }
        }
    }
}

@Composable
private fun PaginationNavBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(
                1.dp,
                if (enabled) MexaWarehouseColors.outlineVariant else MexaWarehouseColors.borderSubtle,
                RoundedCornerShape(6.dp),
            )
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (enabled) MexaWarehouseColors.textPrimary else MexaWarehouseColors.textMuted,
        )
    }
}

@Composable
private fun PaginationNumBtn(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.surfaceLowest,
                RoundedCornerShape(6.dp),
            )
            .border(
                1.dp,
                if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.outlineVariant,
                RoundedCornerShape(6.dp),
            )
            .then(if (!selected) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MexaWarehouseColors.textPrimary,
        )
    }
}

@Composable
private fun PaginationEllipsis() {
    Text(
        "…",
        fontSize = 13.sp,
        color = MexaWarehouseColors.textMuted,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

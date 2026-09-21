package mexa.club.desktop_app.market.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DashboardEmptyErrorContent(
    isError: Boolean,
    message: String,
    onRetry: () -> Unit,
    onClearFilter: () -> Unit = onRetry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Boshqaruv paneli",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.onSurface,
                )
                Text(
                    "Ombor statistikasi va umumiy hisobotlar",
                    fontSize = 14.sp,
                    color = MexaWarehouseColors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MexaWarehouseColors.surfaceLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                ) {
                    Row(Modifier.padding(4.dp)) {
                        listOf("Bugun", "Hafta", "Oy").forEachIndexed { i, label ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (i == 0) MexaWarehouseColors.primary.copy(alpha = 0.08f) else Color.Transparent,
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (i == 0) MexaWarehouseColors.primary else MexaWarehouseColors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("24.05.2024 - 31.05.2024", fontSize = 14.sp)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MexaWarehouseColors.surfaceLowest,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(192.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(192.dp)
                            .background(MexaWarehouseColors.surfaceContainerLow, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MexaWarehouseColors.primaryContainer.copy(alpha = 0.2f),
                        )
                    }
                    Box(
                        Modifier
                            .size(128.dp)
                            .offset(y = 8.dp)
                            .rotate(-6f)
                            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp))
                            .border(2.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Warehouse,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MexaWarehouseColors.outline,
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 16.dp, y = (-8).dp)
                            .size(48.dp)
                            .background(MexaWarehouseColors.errorContainer, CircleShape)
                            .border(4.dp, MexaWarehouseColors.surfaceLowest, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MexaWarehouseColors.error,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Text(
                    if (isError) "Ma'lumotlarni yuklab bo'lmadi" else "Ma'lumotlar topilmadi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isError) {
                        message
                    } else {
                        "Tanlangan vaqt oralig'i uchun statistikalar mavjud emas. Iltimos, boshqa sana tanlang yoki hisobotlarni tekshiring."
                    },
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MexaWarehouseColors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            if (isError) Icons.Filled.Refresh else Icons.Filled.FilterAltOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (isError) "Qayta urinish" else "Filtrni tozalash",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                    OutlinedButton(
                        onClick = { if (!isError) onClearFilter() else onRetry() },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            if (isError) "Filtrni tozalash" else "Yangi hisobot yaratish",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MexaWarehouseColors.outlineVariant),
                )
                Spacer(Modifier.height(32.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    GuidanceColumn(
                        icon = Icons.Filled.CalendarMonth,
                        title = "Vaqtni o'zgartiring",
                        body = "Ma'lumotlar mavjud bo'lgan boshqa davrni tanlang",
                        modifier = Modifier.weight(1f),
                    )
                    GuidanceColumn(
                        icon = Icons.Filled.Refresh,
                        title = "Yangilash",
                        body = "Sahifani yangilab qaytadan urinib ko'ring",
                        modifier = Modifier.weight(1f),
                    )
                    GuidanceColumn(
                        icon = Icons.AutoMirrored.Filled.ContactSupport,
                        title = "Yordam oling",
                        body = "Texnik yordam bo'limi bilan bog'laning",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            FeaturedPlaceholderCard(
                title = "Ombor operatsiyalari samaradorligi",
                modifier = Modifier.weight(1f),
            )
            FeaturedPlaceholderCard(
                title = "Tahliliy hisobotlar markazi",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GuidanceColumn(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MexaWarehouseColors.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MexaWarehouseColors.onSurface)
        Text(
            body,
            fontSize = 12.sp,
            color = MexaWarehouseColors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun FeaturedPlaceholderCard(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(192.dp)
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MexaWarehouseColors.primary.copy(alpha = 0.35f),
                        MexaWarehouseColors.surfaceContainerLow,
                    ),
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

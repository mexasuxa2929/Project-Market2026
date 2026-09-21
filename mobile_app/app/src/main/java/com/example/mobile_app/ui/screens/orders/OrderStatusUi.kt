package com.example.mobile_app.ui.screens.orders

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mobile_app.ui.theme.TextSecondary
import com.example.mobile_app.util.tr

fun orderStatusColor(status: String): Color = when (status.uppercase()) {
    "PENDING"                 -> Color(0xFFF59E0B)
    "CONFIRMED", "PROCESSING" -> Color(0xFF3B82F6)
    "SHIPPED", "DELIVERED"    -> Color(0xFF10B981)
    "CANCELLED"               -> Color(0xFFEF4444)
    else                      -> TextSecondary
}

@Composable
fun orderStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING"    -> tr("orders_status_pending")
    "CONFIRMED"  -> tr("orders_status_confirmed")
    "PROCESSING" -> tr("orders_status_processing")
    "SHIPPED"    -> tr("orders_status_shipped")
    "DELIVERED"  -> tr("orders_status_delivered")
    "CANCELLED"  -> tr("orders_status_cancelled")
    else         -> status
}

// Backend faqat PENDING holatdagi buyurtmani bekor qilishga ruxsat beradi
// (OrderService.cancel → "Only PENDING order can be cancelled").
fun orderCancellable(status: String): Boolean =
    status.uppercase() == "PENDING"
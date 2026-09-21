package mexa.club.desktop_app.market.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DeleteProductDialog(
    product: ProductRowData,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var deleting  by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    fun delete() {
        scope.launch {
            deleting = true
            deleteError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    ApiClient.delete("/api/products/${product.id}")
                }
            }.onSuccess {
                onDeleted()
                onDismiss()
            }.onFailure {
                val raw = it.message ?: ""
                deleteError = try {
                    val root = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<kotlinx.serialization.json.JsonObject>(raw)
                    (root["error"] as? kotlinx.serialization.json.JsonObject)?.let { err ->
                        (err["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    } ?: raw.take(200)
                } catch (_: Exception) { raw.take(200) } ?: "O'chirishda xatolik yuz berdi"
            }
            deleting = false
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 320.dp, max = 480.dp).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column {
                // ── Header ───────────────────────────────────────────────────
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Icon + title row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MexaWarehouseColors.danger.copy(alpha = 0.1f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = MexaWarehouseColors.danger,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Mahsulotni o'chirish",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.textPrimary,
                            )
                            Text(
                                "Bu amalni qaytarib bo'lmaydi",
                                fontSize = 12.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        }
                    }

                    HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                    // Product info
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MexaWarehouseColors.backgroundPage,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoRow("Mahsulot nomi", product.name)
                            InfoRow("Barcode", product.barcode)
                            if (product.categoryName.isNotBlank()) InfoRow("Kategoriya", product.categoryName)
                            if (product.brandName.isNotBlank()) InfoRow("Brend", product.brandName)
                        }
                    }

                    Text(
                        "Ushbu mahsulot tizimdan butunlay o'chiriladi. Davom etishni istaysizmi?",
                        fontSize = 13.sp,
                        color = MexaWarehouseColors.textMuted,
                        lineHeight = 19.sp,
                    )

                    if (deleteError != null) {
                        Text(
                            deleteError!!,
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.danger,
                        )
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer ───────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.backgroundPage)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (!deleting) onDismiss() },
                        enabled = !deleting,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                    ) {
                        Text("Bekor qilish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { delete() },
                        enabled = !deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (deleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("O'chirish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MexaWarehouseColors.textPrimary,
        )
    }
}

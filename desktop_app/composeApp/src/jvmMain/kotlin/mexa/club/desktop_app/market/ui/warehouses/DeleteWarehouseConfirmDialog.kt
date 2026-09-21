package mexa.club.desktop_app.market.ui.warehouses

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DeleteWarehouseConfirmDialog(
    warehouseName: String,
    isDeleting: Boolean = false,
    isForceDeleting: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onForceConfirm: () -> Unit,
) {
    var showForceOption by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 300.dp, max = 480.dp).fillMaxWidth(0.85f),
            shape = RoundedCornerShape(14.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 8.dp,
        ) {
            Column {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(52.dp).background(MexaWarehouseColors.statusBlockedBg, RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Omborni o'chirishni tasdiqlang",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary, textAlign = TextAlign.Center,
                    )
                    Text(
                        "Haqiqatan ham \"$warehouseName\" omborini o'chirmoqchimisiz?",
                        fontSize = 14.sp, color = MexaWarehouseColors.textMuted,
                        textAlign = TextAlign.Center, lineHeight = 21.sp,
                    )
                    Text(
                        "Bu amalni qaytarib bo'lmaydi. Ombor ma'lumotlari va unga biriktirilgan barcha poligonlar o'chadi.",
                        fontSize = 12.sp, color = MexaWarehouseColors.textMuted,
                        textAlign = TextAlign.Center, lineHeight = 18.sp,
                    )

                    if (!showForceOption) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Agar omborda tovar zaxirasi yoki xaridlar bo'lsa, avval ularni o'chiring yoki quyidagi forsmajor rejimidan foydalaning.",
                            fontSize = 12.sp, color = MexaWarehouseColors.statusBlockedFg,
                            textAlign = TextAlign.Center, lineHeight = 18.sp,
                        )
                        Text(
                            "Forsmajor o'chirish",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.danger,
                            modifier = Modifier.clickable { showForceOption = true },
                        )
                    }
                }

                if (showForceOption) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().background(
                                MexaWarehouseColors.danger.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            ).padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.DeleteForever, null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(18.dp))
                                    Text(
                                        "Forsmajor rejimi",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = MexaWarehouseColors.danger,
                                    )
                                }
                                Text(
                                    "Barcha tovar zaxiralari va xaridlar birgalikda o'chiriladi! Bu amalni qaytarib bo'lmaydi.",
                                    fontSize = 12.sp, color = MexaWarehouseColors.textMuted,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
                if (error != null) {
                    Text(
                        error,
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.danger,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (!isDeleting && !isForceDeleting) onDismiss() },
                        enabled = !isDeleting && !isForceDeleting,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textMuted),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("Bekor qilish") }
                    if (showForceOption) {
                        Button(
                            onClick = onForceConfirm,
                            enabled = !isDeleting && !isForceDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MexaWarehouseColors.danger,
                                disabledContainerColor = MexaWarehouseColors.danger.copy(alpha = 0.6f),
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (isForceDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (isForceDeleting) "O'chirilmoqda\u2026" else "Forsmajor o'chirish",
                                color = Color.White, fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = onConfirm,
                            enabled = !isDeleting && !isForceDeleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MexaWarehouseColors.danger,
                                disabledContainerColor = MexaWarehouseColors.danger.copy(alpha = 0.6f),
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isDeleting) "O'chirilmoqda\u2026" else "O'chirish", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

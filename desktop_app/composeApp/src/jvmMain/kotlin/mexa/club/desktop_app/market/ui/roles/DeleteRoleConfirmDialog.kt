package mexa.club.desktop_app.market.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mexa.club.desktop_app.market.model.RoleDefinition
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DeleteRoleConfirmDialog(
    role: RoleDefinition,
    isDeleting: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
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
                // ── Body ──────────────────────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Warning icon
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(MexaWarehouseColors.statusBlockedBg, RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MexaWarehouseColors.danger,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Rolni o'chirishni tasdiqlang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        "Haqiqatan ham \"${role.name}\" rolini o'chirmoqchimisiz?",
                        fontSize = 14.sp,
                        color = MexaWarehouseColors.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                    )

                    // Role code badge
                    Box(
                        Modifier
                            .background(MexaWarehouseColors.statusBlockedBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            role.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.danger,
                        )
                    }

                    Text(
                        "Bu amalni qaytarib bo'lmaydi. Ushbu rolga biriktirilgan foydalanuvchilar ta'sirlanishi mumkin.",
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )

                    // Error message
                    error?.let {
                        Text(
                            it,
                            color = MexaWarehouseColors.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))

                // ── Footer ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (!isDeleting) onDismiss() },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MexaWarehouseColors.textMuted,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MexaWarehouseColors.borderSubtle,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Bekor qilish")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MexaWarehouseColors.danger,
                            disabledContainerColor = MexaWarehouseColors.danger.copy(alpha = 0.6f),
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isDeleting) "O'chirilmoqda…" else "O'chirish",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

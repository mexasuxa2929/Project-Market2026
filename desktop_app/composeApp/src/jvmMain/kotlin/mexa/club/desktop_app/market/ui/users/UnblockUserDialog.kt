package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

/** Foydalanuvchini blokdan chiqarish dialogi (tasdiqlash). */
@Composable
fun UnblockUserDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    isSaving: Boolean = false,
    error: String? = null,
) {
    val lang = appLanguage()

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(min = 340.dp, max = 540.dp).fillMaxWidth(0.88f),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {

                // ── Header ──────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(Color(0xFFE7F6EC), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            AppStrings.unblockDialogTitle(lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                    }
                    IconButton(onClick = { if (!isSaving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MexaWarehouseColors.textMuted)
                    }
                }

                // ── Confirmation text ───────────────────────────────────────
                Text(
                    buildAnnotatedString {
                        append(if (lang.name == "UZ") "Haqiqatan ham " else "Действительно ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)) {
                            append(user.username)
                        }
                        append(if (lang.name == "UZ") " ni blokdan chiqarmoqchimisiz? Foydalanuvchi tizimga qayta kira oladi."
                        else " разблокировать? Пользователь снова сможет войти в систему.")
                    },
                    fontSize = 14.sp,
                    color = MexaWarehouseColors.textMuted,
                    lineHeight = 22.sp,
                )

                // ── Error ───────────────────────────────────────────────────
                error?.let { err ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF1F2), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(err, color = Color(0xFFDC2626), fontSize = 13.sp)
                    }
                }

                // ── Buttons ─────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = { if (!isSaving) onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSaving,
                    ) {
                        Text("Bekor qilish")
                    }
                    Button(
                        onClick = onConfirmed,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(AppStrings.unblockDialogSubmit(lang), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
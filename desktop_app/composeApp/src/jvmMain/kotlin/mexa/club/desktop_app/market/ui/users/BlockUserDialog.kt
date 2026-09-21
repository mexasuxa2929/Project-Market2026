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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

/**
 * Foydalanuvchini bloklash dialogi.
 * Haqiqiy bloklash amali [onConfirmed] orqali chaqiruvchiga topshiriladi
 * (UsersScreen → ViewModel → repository), shuning uchun dialog o'zida
 * to'g'ridan-to'g'ri HTTP chaqirmaydi.
 *
 * @param onConfirmed Bloklash bosilganda chiqariladi; [String] — sabab (trimlangan).
 */
@Composable
fun BlockUserDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit,
    isSaving: Boolean = false,
    error: String? = null,
    isSelf: Boolean = false,
) {
    val lang = appLanguage()
    var reason by remember { mutableStateOf("") }

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
                                .background(Color(0xFFFFE4E6), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            AppStrings.blockDialogTitle(lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                    }
                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MexaWarehouseColors.textMuted,
                        )
                    }
                }

                // ── Confirmation text ───────────────────────────────────────
                Text(
                    buildAnnotatedString {
                        append(if (lang.name == "UZ") "Haqiqatan ham " else "Вы действительно хотите заблокировать ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)) {
                            append(user.username)
                        }
                        append(
                            if (lang.name == "UZ") " ni bloklamoqchimisiz? Bloklangandan so'ng foydalanuvchi tizimga kira olmaydi."
                            else "? После блокировки пользователь не сможет войти в систему."
                        )
                    },
                    fontSize = 14.sp,
                    color = MexaWarehouseColors.textMuted,
                    lineHeight = 22.sp,
                )

                // ── Reason field ─────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        AppStrings.blockDialogReasonLabel(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = {
                            Text(
                                AppStrings.blockDialogReasonPlaceholder(lang),
                                color = MexaWarehouseColors.textCaption,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSaving,
                    )
                    Text(
                        AppStrings.blockDialogReasonHint(lang),
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textCaption,
                    )
                }

                // ── Self-protection warning ──────────────────────────────────
                if (isSelf) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(AppStrings.selfBlockWarning(lang), color = Color(0xFFB45309), fontSize = 13.sp)
                    }
                }

                // ── Error ────────────────────────────────────────────────────
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

                // ── Buttons ──────────────────────────────────────────────────
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
                        onClick = { onConfirmed(reason.trim()) },
                        enabled = !isSaving && !isSelf,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
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
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(AppStrings.blockDialogSubmit(lang), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
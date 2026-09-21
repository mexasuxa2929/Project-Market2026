package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import mexa.club.desktop_app.market.ui.users.common.RoleBadge
import mexa.club.desktop_app.market.ui.users.common.UserAvatarCircle
import mexa.club.desktop_app.market.users.lastActivityLabel
import mexa.club.desktop_app.market.users.parseIso
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Foydalanuvchi batafsil ma'lumoti dialogi. */
@Composable
fun UserDetailDialog(user: AdminUser, onDismiss: () -> Unit) {
    val lang = appLanguage()

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 380.dp, max = 520.dp).fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 8.dp,
        ) {
            Column {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        AppStrings.usersDetailTitle(lang),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

                    // ── Identity card ───────────────────────────────────────
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F3FF), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        UserAvatarCircle(user.fullName, size = 44.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(user.fullName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MexaWarehouseColors.textPrimary)
                            Text("@" + user.username, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Fields ─────────────────────────────────────────────
                    DetailField(AppStrings.usersDetailEmail(lang), user.email)
                    DetailField(AppStrings.usersDetailId(lang), user.id, monospace = true)

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(AppStrings.usersDetailRoles(lang), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val displayCodes = user.rawRoleNames.ifEmpty {
                                user.roles.map { mexa.club.desktop_app.market.model.userRoleToBackendName(it) }
                            }
                            displayCodes.forEach { code -> RoleBadge(code) }
                        }
                    }

                    DetailField(AppStrings.usersDetailStatus(lang), user.status.name)
                    DetailField(AppStrings.usersDetailVerified(lang), if (user.isVerified) AppStrings.verifiedYes(lang) else AppStrings.verifiedPending(lang))
                    DetailField(AppStrings.usersDetailProvider(lang), user.authProvider.name)
                    DetailField(AppStrings.usersDetailCreatedAt(lang), formatIso(user.createdAt))
                    DetailField(AppStrings.usersDetailLastActive(lang), lastActivityLabel(user.lastActiveAt, user.online, lang))
                    DetailField(AppStrings.usersDetailBlockReason(lang), user.blockReason?.ifBlank { null })
                    DetailField(AppStrings.usersDetailBlockedAt(lang), formatIso(user.blockedAt))
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer ─────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(AppStrings.usersDetailClose(lang))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String?, monospace: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        Text(
            value?.ifBlank { null } ?: "—",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MexaWarehouseColors.textPrimary,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 2,
        )
    }
}

private fun formatIso(iso: String?): String? {
    val parsed = parseIso(iso) ?: return null
    return parsed.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
}
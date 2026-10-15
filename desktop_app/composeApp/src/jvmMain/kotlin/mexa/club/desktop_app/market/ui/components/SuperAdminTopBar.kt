package mexa.club.desktop_app.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

/**
 * Yuqori header — global search, notification va help tugmalari olib tashlangan.
 * Chapda sahifa sarlavhasi + sana, o'ngda faqat profil.
 */
@Composable
fun SuperAdminTopBar(
    title: String,
    dateLine: String?,
    displayName: String,
    roleLabel: String,
    avatarInitials: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MexaWarehouseColors.surfaceLowest),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Chap: sahifa nomi + sana
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                    maxLines = 1,
                )
                if (!dateLine.isNullOrBlank()) {
                    Text(
                        dateLine,
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textMuted,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // O'ng: faqat profil
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                    Column(horizontalAlignment = Alignment.End) {
                        val name = remember(displayName) {
                            val u = displayName.trim()
                            if (u.isEmpty()) "Super Admin"
                            else if (u.length > 20) u.take(20) + "…" else u
                        }
                        Text(
                            name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            roleLabel.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MexaWarehouseColors.primary,
                            maxLines = 1,
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(MexaWarehouseColors.primaryContainer, CircleShape)
                            .border(2.dp, MexaWarehouseColors.infoBadgeBg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            avatarInitials,
                            color = MexaWarehouseColors.surfaceLowest,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
            }
        }
        HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
    }
}

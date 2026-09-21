package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.LocalLanguage
import mexa.club.desktop_app.theme.WarehousePalette

@Composable
fun LanguageToggleBar(modifier: Modifier = Modifier) {
    val controller = LocalLanguage.current
    val lang = controller.language
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = WarehousePalette.SurfaceContainerLow,
        border = BorderStroke(1.dp, WarehousePalette.OutlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.Language,
                contentDescription = null,
                tint = WarehousePalette.Outline,
                modifier = Modifier.padding(end = 4.dp),
            )
            TextButton(
                onClick = { controller.setLanguage(AppLanguage.UZ) },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    "UZ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (lang == AppLanguage.UZ) WarehousePalette.Primary else WarehousePalette.OnSurfaceVariant,
                )
            }
            Text("|", color = WarehousePalette.OutlineVariant, style = MaterialTheme.typography.labelSmall)
            TextButton(
                onClick = { controller.setLanguage(AppLanguage.RU) },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    "RU",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (lang == AppLanguage.RU) WarehousePalette.Primary else WarehousePalette.OnSurfaceVariant,
                )
            }
        }
    }
}

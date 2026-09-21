package mexa.club.desktop_app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.localization.AppLanguage
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.PasswordStrength
import mexa.club.desktop_app.localization.evaluatePasswordStrength
import mexa.club.desktop_app.localization.filledSegments
import mexa.club.desktop_app.theme.WarehousePalette

@Composable
fun PasswordStrengthMeter(
    password: String,
    lang: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val strength = remember(password) { evaluatePasswordStrength(password) }
    val filled = strength.filledSegments()
    val activeColor = when (strength) {
        PasswordStrength.Empty -> WarehousePalette.SurfaceVariant
        PasswordStrength.Weak -> Color(0xFFDC2626)
        PasswordStrength.Fair -> Color(0xFFF59E0B)
        PasswordStrength.Good -> WarehousePalette.Primary
        PasswordStrength.Strong -> WarehousePalette.Green600
    }
    val labelColor = when (strength) {
        PasswordStrength.Empty -> WarehousePalette.Outline
        PasswordStrength.Weak -> Color(0xFFDC2626)
        PasswordStrength.Fair -> Color(0xFFD97706)
        PasswordStrength.Good -> WarehousePalette.Primary
        PasswordStrength.Strong -> WarehousePalette.Green600
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { idx ->
                val isOn = idx < filled
                val segmentColor = if (isOn) activeColor else WarehousePalette.SurfaceVariant
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(segmentColor),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            AppStrings.passwordStrengthLabel(lang, strength),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = labelColor,
        )
    }
}

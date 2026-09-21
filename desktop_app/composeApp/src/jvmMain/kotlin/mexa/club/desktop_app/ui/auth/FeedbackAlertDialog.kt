package mexa.club.desktop_app.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import mexa.club.desktop_app.theme.WarehousePalette

data class FeedbackDialogState(
    val isSuccess: Boolean,
    val title: String,
    val message: String,
    /** OK bosilganda (va dialog yopilganda) chaqiriladi. */
    val onConfirm: () -> Unit = {},
)

@Composable
fun FeedbackAlertDialog(
    state: FeedbackDialogState?,
    onDismissState: () -> Unit,
    confirmLabel: String = "OK",
) {
    val s = state ?: return
    AlertDialog(
        onDismissRequest = onDismissState,
        icon = {
            Icon(
                imageVector = if (s.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (s.isSuccess) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(s.title) },
        text = {
            Text(
                s.message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarehousePalette.OnSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissState()
                    s.onConfirm()
                },
            ) {
                Text(confirmLabel, color = WarehousePalette.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

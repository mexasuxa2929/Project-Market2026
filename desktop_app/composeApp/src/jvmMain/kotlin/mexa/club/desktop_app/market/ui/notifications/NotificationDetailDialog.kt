package mexa.club.desktop_app.market.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun NotificationDetailDialog(
    notification: NotificationRow,
    onDismiss: () -> Unit,
    onResent: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var resending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun resend() {
        scope.launch {
            resending = true
            error = null
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.post("/api/admin/notifications/resend/${notification.id}", "{}") }
            }
            result.onSuccess { onResent() }
                .onFailure { e -> error = e.message ?: "Xatolik yuz berdi" }
            resending = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 420.dp, max = 540.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirishnoma tafsilotlari", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                DetailRow("Kanal", notification.channel)
                DetailRow("Turi", notification.type)
                if (notification.recipientEmail.isNotBlank()) DetailRow("Email", notification.recipientEmail)
                if (notification.recipientPhone.isNotBlank()) DetailRow("Telefon", notification.recipientPhone)
                DetailRow("Mavzu", notification.subject.ifBlank { "—" })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Holat", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    val (bg, fg) = notificationStatusColors(notification.status)
                    Row(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(notificationStatusLabel(notification.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
                DetailRow("Qayta urinishlar", notification.retryCount.toString())
                if (notification.errorMessage.isNotBlank()) {
                    Column {
                        Text("Xato xabari", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        Text(notification.errorMessage, fontSize = 13.sp, color = MexaWarehouseColors.danger)
                    }
                }

                error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) { Text("Yopish") }
                    Button(
                        onClick = { resend() },
                        enabled = !resending,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (resending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Qayta yuborish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        Text(value, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
    }
}

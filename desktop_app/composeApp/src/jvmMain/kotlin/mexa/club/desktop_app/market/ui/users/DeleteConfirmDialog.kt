package mexa.club.desktop_app.market.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import mexa.club.desktop_app.market.model.AdminUser
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun DeleteConfirmDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(28.dp))
                    Text("Foydalanuvchini o'chirish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "«${user.username}» (@${user.email}) butunlay o'chiriladi. Bu amalni bekor qilib bo'lmaydi.",
                    color = MexaWarehouseColors.textMuted,
                    fontSize = 14.sp,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Bekor qilish") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                    ) {
                        Text("O'chirish")
                    }
                }
            }
        }
    }
}

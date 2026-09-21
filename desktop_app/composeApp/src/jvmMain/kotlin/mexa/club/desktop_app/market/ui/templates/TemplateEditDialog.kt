package mexa.club.desktop_app.market.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
fun TemplateEditDialog(
    template: TemplateRow,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf(template.subject) }
    var bodyTemplate by remember { mutableStateOf(template.bodyTemplate) }
    var active by remember { mutableStateOf(template.active) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (subject.isBlank() || bodyTemplate.isBlank()) { error = "Mavzu va matnni to'ldiring"; return }
        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{")
                append("\"subject\":\"").append(subject.replace("\"", "\\\"").replace("\n", "\\n")).append("\",")
                append("\"bodyTemplate\":\"").append(bodyTemplate.replace("\"", "\\\"").replace("\n", "\\n")).append("\",")
                append("\"active\":").append(active)
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.put("/api/admin/templates/${template.id}", json) }
            }
            result.onSuccess { onSaved() }
                .onFailure { e -> error = e.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 420.dp, max = 580.dp).heightIn(max = 640.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Shablonni tahrirlash", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                        Text("${template.type} • ${template.channel}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mavzu", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                    OutlinedTextField(value = subject, onValueChange = { subject = it; error = null }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Matn shabloni", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                    Text("O'zgaruvchilar: {{userName}}, {{orderNumber}} va h.k.", fontSize = 11.sp, color = MexaWarehouseColors.textCaption)
                    OutlinedTextField(
                        value = bodyTemplate,
                        onValueChange = { bodyTemplate = it; error = null },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Faol", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Bekor qilish") }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Saqlash", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

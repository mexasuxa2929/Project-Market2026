package mexa.club.desktop_app.market.ui.warehouses

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWarehouseDialog(
    warehouseId: String,
    initialName: String,
    initialCapacity: Int,
    initialActive: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var capacity by remember { mutableStateOf(if (initialCapacity > 0) initialCapacity.toString() else "") }
    var isActive by remember { mutableStateOf(initialActive) }
    var statusExpanded by remember { mutableStateOf(false) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nameError = name.isBlank() && error != null
    val isValid = name.isNotBlank()

    fun save() {
        if (!isValid) { error = "Ombor nomi majburiy"; return }
        scope.launch {
            saving = true
            error = null
            val cap = capacity.toIntOrNull()
            val json = buildString {
                append("{")
                append("\"name\":${name.trim().jsonString()}")
                append(",\"active\":$isActive")
                if (cap != null) append(",\"capacity\":$cap")
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.put("/api/warehouses/$warehouseId", json) }
            }
            result.onSuccess {
                saving = false
                onSaved()
            }.onFailure { e ->
                error = e.message ?: "Xatolik yuz berdi"
                saving = false
            }
        }
    }

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
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Omborni tahrirlash",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Ombor nomi")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = { Text("Masalan: Farg'ona Filiali", color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError,
                        shape = RoundedCornerShape(8.dp),
                        supportingText = if (nameError) {
                            { Text("Ombor nomi bo'sh bo'lmasligi kerak", color = MexaWarehouseColors.danger, fontSize = 12.sp) }
                        } else null,
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Umumiy sig'im (m³)")
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = { if (it.all { c -> c.isDigit() }) capacity = it },
                            placeholder = { Text("5000", color = MexaWarehouseColors.textCaption) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Status")
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = if (isActive) "Faol" else "Nofaol",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false },
                            ) {
                                DropdownMenuItem(text = { Text("Faol") }, onClick = { isActive = true; statusExpanded = false })
                                DropdownMenuItem(text = { Text("Nofaol") }, onClick = { isActive = false; statusExpanded = false })
                            }
                        }
                    }
                }


                error?.let { err ->
                    Text(err, color = MexaWarehouseColors.danger, fontSize = 13.sp)
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = { if (!saving) onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Bekor qilish")
                    }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Saqlash", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
}

package mexa.club.desktop_app.market.ui.warehouses

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun EditMinStockDialog(
    warehouseId: String,
    productId: String,
    productName: String,
    currentMinStock: Double,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var minStockText by remember { mutableStateOf(currentMinStock.toInt().toString()) }
    var useDefault by remember { mutableStateOf(false) }

    val minValue = minStockText.toIntOrNull()
    val isValid = useDefault || (minStockText.isNotBlank() && minValue != null && minValue >= 0)

    fun save() {
        if (!isValid) {
            error = "Minimal stockni to'g'ri kiriting (0 yoki undan katta)"
            return
        }
        scope.launch {
            saving = true
            error = null
            val json = buildString {
                append("{\"productId\":${productId.jsonString()},\"minStockOverride\":")
                if (useDefault) append("null") else append(minValue)
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.put("/api/warehouses/$warehouseId/stock/min-stock", json) }
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
            modifier = Modifier.widthIn(min = 380.dp, max = 480.dp).fillMaxWidth(0.9f),
        ) {
            Column(
                Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Minimal stock",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                Text(
                    productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MexaWarehouseColors.textPrimary,
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("Minimal stock (dona)")
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) {
                                minStockText = it
                                useDefault = false
                            }
                        },
                        enabled = !useDefault && !saving,
                        placeholder = { Text("Masalan: 10", color = MexaWarehouseColors.textCaption) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                    )
                }

                OutlinedButton(
                    onClick = { useDefault = !useDefault },
                    enabled = !saving,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        if (useDefault) "Mahsulot default'dan foydalanish: YONIQ"
                        else "Mahsulot default'dan foydalanish",
                        fontSize = 13.sp,
                    )
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
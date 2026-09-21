package mexa.club.desktop_app.market.ui.delivery

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class CourierOption(val id: String, val name: String, val region: String, val currentDeliveries: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignCourierDialog(
    delivery: DeliveryRow,
    onDismiss: () -> Unit,
    onAssigned: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var couriers by remember { mutableStateOf<List<CourierOption>>(emptyList()) }
    var loadingCouriers by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<CourierOption?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun fetchCouriers(): List<CourierOption> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/couriers")
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val c = el as? JsonObject ?: return@mapNotNull null
            val active = c.stringField("active").toBooleanStrictOrNull() ?: true
            if (!active) return@mapNotNull null
            CourierOption(
                id = c.stringField("id").ifEmpty { return@mapNotNull null },
                name = c.stringField("name"),
                region = c.stringField("region"),
                currentDeliveries = c.stringField("currentDeliveries").toIntOrNull() ?: 0,
            )
        }
    }

    LaunchedEffect(Unit) {
        loadingCouriers = true
        runCatching { fetchCouriers() }
            .onSuccess { list ->
                couriers = list
                selected = list.find { it.id == delivery.courierId }
            }
        loadingCouriers = false
    }

    fun assign() {
        val courierId = selected?.id
        if (courierId.isNullOrBlank()) { error = "Kuryerni tanlang"; return }
        scope.launch {
            saving = true
            error = null
            val json = "{\"courierId\":\"$courierId\"}"
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.put("/api/admin/deliveries/${delivery.id}/assign", json) }
            }
            result.onSuccess { onAssigned() }
                .onFailure { e -> error = e.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 360.dp, max = 460.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kuryer tayinlash", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                Column {
                    Text(delivery.trackingCode.ifBlank { "Kuzatuv kodi yo'q" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    Text(delivery.deliveryAddress, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    Text("${delivery.recipientName} • ${delivery.recipientPhone}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                }

                Text("Kuryer", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                if (loadingCouriers) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else if (couriers.isEmpty()) {
                    Text("Faol kuryerlar topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                } else {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selected?.let { "${it.name} (${it.region}) — ${it.currentDeliveries} ta" } ?: "Tanlang",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            couriers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${c.region}) — ${c.currentDeliveries} ta") },
                                    onClick = { selected = c; expanded = false },
                                )
                            }
                        }
                    }
                }

                error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text("Bekor qilish") }
                    Button(
                        onClick = { assign() },
                        enabled = !saving && selected != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Tayinlash", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

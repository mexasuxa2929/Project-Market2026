package mexa.club.desktop_app.market.ui.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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

private data class ZoneRow(
    val id: String,
    val region: String,
    val district: String,
    val fee: String,
    val estimatedDays: Int,
)

@Composable
fun DeliveryZonesDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var zones by remember { mutableStateOf<List<ZoneRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var editZone by remember { mutableStateOf<ZoneRow?>(null) }

    suspend fun fetchZones(): List<ZoneRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/delivery-zones")
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val z = el as? JsonObject ?: return@mapNotNull null
            val id = z.stringField("id").ifEmpty { return@mapNotNull null }
            ZoneRow(
                id = id,
                region = z.stringField("region"),
                district = z.stringField("district"),
                fee = z.stringField("fee").ifEmpty { "0" },
                estimatedDays = z.stringField("estimatedDays").toIntOrNull() ?: 1,
            )
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { fetchZones() }.onSuccess { zones = it }
        loading = false
    }

    fun reload() {
        scope.launch {
            loading = true
            runCatching { fetchZones() }.onSuccess { zones = it }
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 500.dp, max = 580.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Yetkazib berish zonalari", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { editZone = null; showForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Yangi zona", fontSize = 13.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                        }
                    }
                }

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }
                } else if (zones.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Zonalar topilmadi", color = MexaWarehouseColors.textMuted)
                    }
                } else {
                    // Header
                    Row(Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg, RoundedCornerShape(8.dp)).padding(vertical = 10.dp, horizontal = 12.dp)) {
                        Text("Viloyat", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.outline)
                        Text("Tuman", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.outline)
                        Text("Narx", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.outline)
                        Text("Kun", modifier = Modifier.weight(0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.outline)
                        Text("", modifier = Modifier.weight(0.3f))
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                        items(zones) { z ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(z.region, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                Text(z.district, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                Text(formatDeliveryAmount(z.fee), modifier = Modifier.weight(0.7f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                Text("${z.estimatedDays}", modifier = Modifier.weight(0.4f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                ApiClient.delete("/api/admin/delivery-zones/${z.id}")
                                            }
                                            reload()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "O'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(16.dp))
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.5f)))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) { Text("Yopish") }
                }
            }
        }
    }
}

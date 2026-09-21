package mexa.club.desktop_app.market.ui.couriers

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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

private data class CourierDeliveryRow(
    val trackingCode: String,
    val status: String,
    val deliveryFee: String,
    val createdAt: String,
)

private data class CourierDetailData(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val region: String,
    val active: Boolean,
    val currentDeliveries: Int,
    val deliveries: List<CourierDeliveryRow>,
)

@Composable
fun CourierDetailDialog(
    courierId: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onUpdated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<CourierDetailData?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    suspend fun fetchDetail(): CourierDetailData = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/couriers/$courierId")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
            ?: throw IllegalStateException("Kuryer topilmadi")
        CourierDetailData(
            id = data.stringField("id"),
            userId = data.stringField("userId"),
            name = data.stringField("name"),
            phone = data.stringField("phone"),
            region = data.stringField("region"),
            active = data.stringField("active").toBooleanStrictOrNull() ?: true,
            currentDeliveries = data.stringField("currentDeliveries").toIntOrNull() ?: 0,
            deliveries = emptyList(),
        )
    }

    fun deleteCourier() {
        scope.launch {
            deleting = true
            deleteError = null
            runCatching {
                withContext(Dispatchers.IO) { ApiClient.delete("/api/admin/couriers/$courierId") }
            }.onSuccess { onDeleted() }
                .onFailure { e -> deleteError = e.message ?: "Xatolik yuz berdi" }
            deleting = false
        }
    }

    LaunchedEffect(courierId) {
        loading = true
        runCatching { fetchDetail() }
            .onSuccess { detail = it }
            .onFailure { error = it.message ?: "Xatolik" }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 420.dp, max = 520.dp).heightIn(max = 650.dp),
        ) {
            when {
                loading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                }
                error != null || detail == null -> Box(Modifier.fillMaxWidth().height(200.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Xatolik", color = MexaWarehouseColors.danger)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onDismiss) { Text("Yopish") }
                    }
                }
                else -> {
                    val d = detail!!
                    Column(
                        Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(d.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                                Text(d.phone, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val (bg, fg) = if (d.active) MexaWarehouseColors.statusActiveBg to MexaWarehouseColors.statusActiveFg
                                else MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
                                Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(if (d.active) "FAOL" else "NOFAOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                                }
                                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                                }
                            }
                        }

                        Column(Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoRow("Hudud", d.region.ifBlank { "—" })
                            InfoRow("Joriy yetkazmalar", d.currentDeliveries.toString())
                            InfoRow("Foydalanuvchi ID", d.userId.take(8).ifBlank { "—" })
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onEdit,
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tahrirlash")
                            }
                            if (d.currentDeliveries == 0) {
                                Button(
                                    onClick = { deleteCourier() },
                                    enabled = !deleting,
                                    colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    if (deleting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("O'chirish")
                                    }
                                }
                            }
                        }

                        deleteError?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 12.sp) }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = onDismiss) { Text("Yopish") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        Text(value, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
    }
}

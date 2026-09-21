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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.jsonString
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

@Composable
fun AdjustStockDialog(
    warehouseId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    initialProductId: String? = null,
    initialProductName: String? = null,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var stock by remember { mutableStateOf<List<WarehouseStockOption>>(emptyList()) }

    var selectedProduct by remember { mutableStateOf("") }
    var isIncoming by remember { mutableStateOf(true) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching {
            val stockText = withContext(Dispatchers.IO) { ApiClient.get("/api/warehouses/$warehouseId/stock?page=0&size=200") }
            val stockObj = ApiClient.parseJsonObject(stockText)?.let { ApiClient.dataObject(it) }
            stock = (stockObj?.itemsOrContentArray() ?: JsonArray(emptyList())).mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val id = o.stringField("id").ifEmpty { return@mapNotNull null }
                val productId = o.stringField("productId").ifEmpty { return@mapNotNull null }
                WarehouseStockOption(
                    id = id,
                    productId = productId,
                    name = o.stringField("productName").ifEmpty { "Mahsulot #${productId.take(8)}" },
                    available = (o["availableQuantity"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0,
                )
            }
        }.onFailure { error = it.message ?: "Xatolik" }
        loading = false
        if (!initialProductId.isNullOrBlank() || !initialProductName.isNullOrBlank()) {
            selectedProduct = stock.firstOrNull { it.productId == initialProductId }?.productId
                ?: stock.firstOrNull { it.name == initialProductName }?.productId
                ?: ""
        }
    }

    val qtyNum = quantity.toDoubleOrNull()
    val selectedStock = stock.firstOrNull { it.productId == selectedProduct }
    val isValid = selectedProduct.isNotEmpty() && qtyNum != null && qtyNum > 0 &&
        (isIncoming || selectedStock == null || qtyNum <= selectedStock.available)

    fun save() {
        if (!isValid) {
            error = "Mahsulot tanlang va miqdorni to'g'ri kiriting"
            return
        }
        scope.launch {
            saving = true
            error = null
            val delta = if (isIncoming) qtyNum else -qtyNum
            val json = buildString {
                append("{\"productId\":${selectedProduct.jsonString()},\"quantityDelta\":$delta")
                if (reason.isNotBlank()) append(",\"reason\":${reason.trim().jsonString()}")
                append("}")
            }
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.post("/api/warehouses/$warehouseId/stock/adjust", json) }
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
            modifier = Modifier.widthIn(min = 380.dp, max = 560.dp).fillMaxWidth(0.9f),
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
                        "Stockni tuzatish",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MexaWarehouseColors.textPrimary,
                    )
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormLabel("Mahsulot")
                            DropdownField(
                                value = selectedProduct,
                                placeholder = "Mahsulot tanlang...",
                                options = stock.map { it.productId to "${it.name} (${it.available.toInt()} dona)" },
                                onSelect = { selectedProduct = it },
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isIncoming = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = if (isIncoming)
                                    ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent)
                                else ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textMuted),
                            ) {
                                Text("Kirim (+)")
                            }
                            OutlinedButton(
                                onClick = { isIncoming = false },
                                shape = RoundedCornerShape(8.dp),
                                colors = if (!isIncoming)
                                    ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger)
                                else ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textMuted),
                            ) {
                                Text("Chiqim (-)")
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormLabel("Miqdor")
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) quantity = it },
                                placeholder = { Text("Masalan: 10", color = MexaWarehouseColors.textCaption) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormLabel("Sabab (ixtiyoriy)")
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                placeholder = { Text("Masalan: inventarizatsiya natijasi", color = MexaWarehouseColors.textCaption) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                            )
                        }

                        if (!isIncoming) {
                            selectedStock?.let { s ->
                                Text(
                                    "Mavjud: ${s.available.toInt()} dona",
                                    fontSize = 12.sp,
                                    color = MexaWarehouseColors.textMuted,
                                )
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
    }
}

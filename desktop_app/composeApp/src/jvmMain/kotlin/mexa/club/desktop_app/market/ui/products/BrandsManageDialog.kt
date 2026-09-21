package mexa.club.desktop_app.market.ui.products

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.jetbrains.skia.Image as SkiaImage

data class BrandItem(val id: String, val name: String, val logoUrl: String, val active: Boolean)

@Composable
private fun BrandLogo(logoUrl: String, modifier: Modifier = Modifier) {
    var bitmap by remember(logoUrl) { mutableStateOf<ImageBitmap?>(null) }

    if (logoUrl.isNotEmpty()) {
        LaunchedEffect(logoUrl) {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = ApiClient.getBytes(logoUrl)
                    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }
            }.onSuccess { bitmap = it }
        }
    }

    Box(
        modifier
            .size(36.dp)
            .background(MexaWarehouseColors.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Icon(
                Icons.Filled.Label,
                contentDescription = null,
                tint = MexaWarehouseColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun BrandsManageDialog(
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var brands       by remember { mutableStateOf<List<BrandItem>>(emptyList()) }
    var loading      by remember { mutableStateOf(true) }
    var error        by remember { mutableStateOf<String?>(null) }
    var deletingId   by remember { mutableStateOf<String?>(null) }
    var confirmItem  by remember { mutableStateOf<BrandItem?>(null) }
    var showAddBrand by remember { mutableStateOf(false) }
    var editingBrand by remember { mutableStateOf<BrandItem?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/brands?size=200&sort=name")
                    val root = ApiClient.parseJsonObject(text)
                    val data = root?.let { ApiClient.dataObject(it) }
                    val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    items.mapNotNull { el ->
                        val b = el as? JsonObject ?: return@mapNotNull null
                        val id = b.stringField("id").ifEmpty { return@mapNotNull null }
                        BrandItem(
                            id      = id,
                            name    = b.stringField("name"),
                            logoUrl = b.stringField("logoUrl"),
                            active  = (b["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                        )
                    }
                }
            }.onSuccess { brands = it }.onFailure { error = it.message }
            loading = false
        }
    }

    fun delete(id: String) {
        scope.launch {
            deletingId = id
            runCatching { withContext(Dispatchers.IO) { ApiClient.delete("/api/brands/$id") } }
                .onSuccess { onChanged(); load() }
                .onFailure { error = it.message ?: "O'chirishda xatolik" }
            deletingId = null; confirmItem = null
        }
    }

    LaunchedEffect(Unit) { load() }

    // ── AddBrandDialog ────────────────────────────────────────────────────────
    if (showAddBrand) {
        AddBrandDialog(
            onDismiss = { showAddBrand = false },
            onSaved   = { showAddBrand = false; onChanged(); load() },
        )
    }

    // ── EditBrandDialog ───────────────────────────────────────────────────────
    editingBrand?.let { brand ->
        AddBrandDialog(
            onDismiss  = { editingBrand = null },
            onSaved    = { editingBrand = null; onChanged(); load() },
            editBrand  = brand,
        )
    }

    // ── Delete confirm ────────────────────────────────────────────────────────
    confirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (deletingId == null) confirmItem = null },
            title = { Text("Brendni o'chirish", fontWeight = FontWeight.Bold) },
            text  = { Text("\"${item.name}\" brendini o'chirishni tasdiqlaysizmi?\nBu amalni qaytarib bo'lmaydi.") },
            confirmButton = {
                Button(
                    onClick = { delete(item.id) },
                    enabled = deletingId == null,
                    colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                ) {
                    if (deletingId == item.id) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("O'chirish")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmItem = null }, enabled = deletingId == null) {
                    Text("Bekor qilish")
                }
            },
        )
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 340.dp, max = 540.dp).fillMaxWidth(0.88f),
            shape    = RoundedCornerShape(16.dp),
            color    = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Brendlarni boshqarish",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        Text(
                            "${brands.size} ta brend",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        // + Yangi brend tugmasi
                        Button(
                            onClick = { showAddBrand = true },
                            colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                            shape   = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Yangi brend", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                        }
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── List ──────────────────────────────────────────────────────
                Box(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 440.dp)) {
                    when {
                        loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MexaWarehouseColors.primary, modifier = Modifier.size(32.dp))
                        }
                        error != null -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(error!!, color = MexaWarehouseColors.danger, fontSize = 13.sp)
                                OutlinedButton(onClick = { load() }) { Text("Qayta urinish") }
                            }
                        }
                        brands.isEmpty() -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Brendlar mavjud emas", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                                Button(
                                    onClick = { showAddBrand = true },
                                    colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                                    shape   = RoundedCornerShape(8.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Birinchi brendni qo'shing")
                                }
                            }
                        }
                        else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                            brands.forEachIndexed { index, brand ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index % 2 == 0) Color.Transparent
                                            else MexaWarehouseColors.backgroundPage.copy(alpha = 0.5f),
                                        )
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        BrandLogo(logoUrl = brand.logoUrl)
                                        Column {
                                            Text(
                                                brand.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MexaWarehouseColors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                if (brand.active) "Faol" else "Nofaol",
                                                fontSize = 11.sp,
                                                color = if (brand.active) MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.textMuted,
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        // Edit tugmasi
                                        IconButton(
                                            onClick  = { editingBrand = brand },
                                            enabled  = deletingId == null,
                                            modifier = Modifier.size(34.dp),
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Tahrirlash", tint = MexaWarehouseColors.primary, modifier = Modifier.size(17.dp))
                                        }
                                        // Delete tugmasi
                                        IconButton(
                                            onClick  = { confirmItem = brand },
                                            enabled  = deletingId == null,
                                            modifier = Modifier.size(34.dp),
                                        ) {
                                            if (deletingId == brand.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MexaWarehouseColors.danger)
                                            } else {
                                                Icon(Icons.Filled.Delete, contentDescription = "O'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                if (index < brands.lastIndex) {
                                    HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.backgroundPage)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                        shape   = RoundedCornerShape(8.dp),
                    ) {
                        Text("Yopish", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

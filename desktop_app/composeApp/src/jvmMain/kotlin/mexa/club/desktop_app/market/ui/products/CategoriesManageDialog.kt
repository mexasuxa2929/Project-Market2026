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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
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

@Composable
private fun CategoryThumbnail(imageUrl: String, isChild: Boolean, modifier: Modifier = Modifier) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

    if (imageUrl.isNotEmpty()) {
        LaunchedEffect(imageUrl) {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = ApiClient.getBytes(imageUrl)
                    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                }
            }.onSuccess { bitmap = it }
        }
    }

    Box(
        modifier
            .size(36.dp)
            .background(
                if (isChild) MexaWarehouseColors.textMuted.copy(alpha = 0.08f)
                else MexaWarehouseColors.primary.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Icon(
                if (isChild) Icons.Filled.SubdirectoryArrowRight else Icons.Filled.Category,
                contentDescription = null,
                tint = if (isChild) MexaWarehouseColors.textMuted else MexaWarehouseColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

data class CategoryItem(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val parentId: String,
    val parentName: String,
    val active: Boolean,
)

@Composable
fun CategoriesManageDialog(
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var categories       by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var loading          by remember { mutableStateOf(true) }
    var error            by remember { mutableStateOf<String?>(null) }
    var deletingId       by remember { mutableStateOf<String?>(null) }
    var confirmItem      by remember { mutableStateOf<CategoryItem?>(null) }
    var editItem         by remember { mutableStateOf<CategoryItem?>(null) }
    var showAddCategory  by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true; error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/categories?size=200&sort=name")
                    val root = ApiClient.parseJsonObject(text)
                    val data = root?.let { ApiClient.dataObject(it) }
                    val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    items.mapNotNull { el ->
                        val c = el as? JsonObject ?: return@mapNotNull null
                        val id = c.stringField("id").ifEmpty { return@mapNotNull null }
                        CategoryItem(
                            id          = id,
                            name        = c.stringField("name"),
                            description = c.stringField("description"),
                            imageUrl    = c.stringField("imageUrl"),
                            parentId    = c.stringField("parentId"),
                            parentName  = c.stringField("parentName"),
                            active      = (c["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                        )
                    }
                }
            }.onSuccess { categories = it }.onFailure { error = it.message }
            loading = false
        }
    }

    fun delete(id: String) {
        scope.launch {
            deletingId = id
            runCatching { withContext(Dispatchers.IO) { ApiClient.delete("/api/categories/$id") } }
                .onSuccess { onChanged(); load() }
                .onFailure { error = it.message ?: "O'chirishda xatolik" }
            deletingId = null; confirmItem = null
        }
    }

    LaunchedEffect(Unit) { load() }

    // ── AddCategoryDialog ─────────────────────────────────────────────────────
    if (showAddCategory) {
        val parentItems = categories.map { DropdownItem(id = it.id, name = it.name) }
        AddCategoryDialog(
            onDismiss        = { showAddCategory = false },
            parentCategories = parentItems,
            onSaved          = { showAddCategory = false; onChanged(); load() },
        )
    }

    // ── EditCategoryDialog ────────────────────────────────────────────────────
    editItem?.let { item ->
        val parentItems = categories.filter { it.id != item.id }.map { DropdownItem(id = it.id, name = it.name) }
        EditCategoryDialog(
            item             = item,
            parentCategories = parentItems,
            onDismiss        = { editItem = null },
            onSaved          = { editItem = null; onChanged(); load() },
        )
    }

    // ── Delete confirm ────────────────────────────────────────────────────────
    confirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (deletingId == null) confirmItem = null },
            title = { Text("Kategoriyani o'chirish", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("\"${item.name}\" kategoriyasini o'chirishni tasdiqlaysizmi?")
                    Text(
                        "Diqqat: bu kategoriyaga bog'liq mahsulotlarning kategoriyasi bo'sh qoladi.",
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textMuted,
                    )
                }
            },
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
            modifier = Modifier.widthIn(min = 340.dp, max = 560.dp).fillMaxWidth(0.88f),
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
                            "Kategoriyalarni boshqarish",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        Text(
                            "${categories.size} ta kategoriya",
                            fontSize = 12.sp,
                            color = MexaWarehouseColors.textMuted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick  = { showAddCategory = true },
                            colors   = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                            shape    = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Yangi kategoriya", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                        }
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── List ──────────────────────────────────────────────────────
                Box(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 460.dp)) {
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
                        categories.isEmpty() -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Kategoriyalar mavjud emas", fontSize = 14.sp, color = MexaWarehouseColors.textMuted)
                                Button(
                                    onClick = { showAddCategory = true },
                                    colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                                    shape   = RoundedCornerShape(8.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Birinchi kategoriyani qo'shing")
                                }
                            }
                        }
                        else -> {
                            val roots    = categories.filter { it.parentId.isEmpty() }
                            val children = categories.filter { it.parentId.isNotEmpty() }
                            val sorted   = roots + children

                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                sorted.forEachIndexed { index, cat ->
                                    val isChild = cat.parentId.isNotEmpty()
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (index % 2 == 0) Color.Transparent
                                                else MexaWarehouseColors.backgroundPage.copy(alpha = 0.5f),
                                            )
                                            .padding(
                                                start  = if (isChild) 44.dp else 20.dp,
                                                end    = 20.dp,
                                                top    = 10.dp,
                                                bottom = 10.dp,
                                            ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            CategoryThumbnail(
                                                imageUrl = cat.imageUrl,
                                                isChild  = isChild,
                                            )
                                            Column {
                                                Text(
                                                    cat.name,
                                                    fontSize   = if (isChild) 13.sp else 14.sp,
                                                    fontWeight = if (isChild) FontWeight.Normal else FontWeight.Medium,
                                                    color      = MexaWarehouseColors.textPrimary,
                                                    maxLines   = 1,
                                                    overflow   = TextOverflow.Ellipsis,
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (cat.parentName.isNotEmpty()) {
                                                        Text("↳ ${cat.parentName}", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                    }
                                                    Text(
                                                        if (cat.active) "Faol" else "Nofaol",
                                                        fontSize = 11.sp,
                                                        color    = if (cat.active) MexaWarehouseColors.statusActiveFg else MexaWarehouseColors.textMuted,
                                                    )
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                            IconButton(
                                                onClick  = { editItem = cat },
                                                enabled  = deletingId == null,
                                                modifier = Modifier.size(34.dp),
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Tahrirlash", tint = MexaWarehouseColors.primary, modifier = Modifier.size(17.dp))
                                            }
                                            IconButton(
                                                onClick  = { confirmItem = cat },
                                                enabled  = deletingId == null,
                                                modifier = Modifier.size(34.dp),
                                            ) {
                                                if (deletingId == cat.id) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MexaWarehouseColors.danger)
                                                } else {
                                                    Icon(Icons.Filled.Delete, contentDescription = "O'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    if (index < sorted.lastIndex) {
                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.5f))
                                    }
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

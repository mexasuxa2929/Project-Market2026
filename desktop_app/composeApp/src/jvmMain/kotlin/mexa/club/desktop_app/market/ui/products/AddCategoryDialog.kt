package mexa.club.desktop_app.market.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    parentCategories: List<DropdownItem> = emptyList(),
) {
    val scope = rememberCoroutineScope()

    var name        by remember { mutableStateOf("") }
    var nameError   by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var active      by remember { mutableStateOf(true) }

    var selParentId   by remember { mutableStateOf("") }
    var selParentName by remember { mutableStateOf("Asosiy kategoriya (yo'q)") }
    var parentExpanded by remember { mutableStateOf(false) }

    var selectedImageFile  by remember { mutableStateOf<java.io.File?>(null) }
    var previewBitmap      by remember { mutableStateOf<ImageBitmap?>(null) }

    var saving    by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun pickImage() {
        val chooser = javax.swing.JFileChooser()
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Rasmlar (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"
        )
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            if (file.length() <= 5L * 1024 * 1024) {
                selectedImageFile = file
                previewBitmap = runCatching {
                    SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                }.getOrNull()
            } else {
                saveError = "Rasm hajmi 5MB dan oshmasligi kerak"
            }
        }
    }

    fun validate(): Boolean {
        nameError = if (name.isBlank()) "Kategoriya nomi majburiy" else null
        return nameError == null
    }

    fun save() {
        if (!validate()) return
        scope.launch {
            saving    = true
            saveError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = buildString {
                        append("{")
                        append("\"name\":\"${name.trim().replace("\"", "\\\"")}\",")
                        if (description.isNotBlank())
                            append("\"description\":\"${description.trim().replace("\"", "\\\"")}\",")
                        if (selParentId.isNotBlank())
                            append("\"parentId\":\"$selParentId\",")
                        append("\"active\":$active")
                        append("}")
                    }
                    val responseText = ApiClient.post("/api/categories", body)
                    // Rasm yuklash — kategoriya yaratilgandan keyin
                    val imgFile = selectedImageFile
                    if (imgFile != null) {
                        val catId = runCatching {
                            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                .decodeFromString<kotlinx.serialization.json.JsonObject>(responseText)
                                .let { it["data"] as? kotlinx.serialization.json.JsonObject }
                                ?.let { (it["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        }.getOrNull()
                        if (!catId.isNullOrBlank()) {
                            ApiClient.uploadFile(
                                "/api/categories/$catId/image",
                                imgFile.name,
                                imgFile.readBytes()
                            )
                        }
                    }
                }
            }.onSuccess {
                onSaved()
                onDismiss()
            }.onFailure {
                saveError = it.message ?: "Saqlashda xatolik yuz berdi"
            }
            saving = false
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 340.dp, max = 580.dp).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.heightIn(max = 700.dp)) {

                // ── Header ──────────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            "Yangi kategoriya qo'shish",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        Text(
                            "Mahsulot kategoriyasi ma'lumotlarini kiriting",
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.textMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    IconButton(
                        onClick = { if (!saving) onDismiss() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Form ────────────────────────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {

                    // Kategoriya nomi
                    CategoryFieldBlock("Kategoriya nomi *", error = nameError) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = null },
                            placeholder = { Text("Masalan: Elektronika", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                            singleLine = true,
                            isError = nameError != null,
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = categoryFieldColors(),
                            textStyle = TextStyle(fontSize = 14.sp),
                        )
                    }

                    // Ota-kategoriya
                    CategoryFieldBlock("Ota-kategoriya") {
                        ExposedDropdownMenuBox(
                            expanded = parentExpanded,
                            onExpandedChange = { if (!saving) parentExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selParentName,
                                onValueChange = {},
                                readOnly = true,
                                enabled = !saving,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp),
                                colors = categoryFieldColors(),
                                textStyle = TextStyle(fontSize = 14.sp),
                            )
                            ExposedDropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Asosiy kategoriya (yo'q)", fontSize = 13.sp) },
                                    onClick = {
                                        selParentId = ""
                                        selParentName = "Asosiy kategoriya (yo'q)"
                                        parentExpanded = false
                                    },
                                )
                                parentCategories.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name, fontSize = 13.sp) },
                                        onClick = {
                                            selParentId = item.id
                                            selParentName = item.name
                                            parentExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Rasm
                    CategoryFieldBlock("Rasm (ixtiyoriy)") {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MexaWarehouseColors.backgroundPage)
                                    .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp))
                                    .clickable { if (!saving) pickImage() },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap!!,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(28.dp))
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { pickImage() },
                                    enabled = !saving,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MexaWarehouseColors.primary),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.primary),
                                ) {
                                    Text("Rasm tanlash", fontSize = 13.sp)
                                }
                                if (selectedImageFile != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(selectedImageFile!!.name, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { selectedImageFile = null; previewBitmap = null }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Filled.Close, contentDescription = null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                } else {
                                    Text("PNG, JPG, WEBP · max 5MB", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                }
                            }
                        }
                    }

                    // Tavsif
                    CategoryFieldBlock("Tavsif") {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Kategoriya haqida batafsil ma'lumot kiriting...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = categoryFieldColors(),
                            textStyle = TextStyle(fontSize = 14.sp),
                            maxLines = 4,
                        )
                    }

                    // Holat
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MexaWarehouseColors.backgroundPage,
                        border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Kategoriya holati",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MexaWarehouseColors.textPrimary,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Switch(
                                    checked = active,
                                    onCheckedChange = { if (!saving) active = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor    = Color.White,
                                        checkedTrackColor    = MexaWarehouseColors.primary,
                                        uncheckedThumbColor  = Color.White,
                                        uncheckedBorderColor = MexaWarehouseColors.outlineVariant,
                                        uncheckedTrackColor  = MexaWarehouseColors.outlineVariant,
                                    ),
                                )
                                Text(
                                    if (active) "Faol" else "Nofaol",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) MexaWarehouseColors.primary else MexaWarehouseColors.textMuted,
                                )
                            }
                        }
                    }
                }

                // Xato xabari
                if (saveError != null) {
                    Text(
                        saveError!!,
                        color = MexaWarehouseColors.danger,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                    )
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer ──────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.backgroundPage)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (!saving) onDismiss() },
                        enabled = !saving,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                    ) {
                        Text("Bekor qilish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Saqlash", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFieldBlock(
    label: String,
    error: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MexaWarehouseColors.textPrimary,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        content()
        if (error != null) {
            Text(error, fontSize = 11.sp, color = MexaWarehouseColors.danger, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun categoryFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor    = MexaWarehouseColors.outlineVariant,
    focusedBorderColor      = MexaWarehouseColors.primary,
    unfocusedContainerColor = MexaWarehouseColors.surfaceLowest,
    focusedContainerColor   = MexaWarehouseColors.surfaceLowest,
)

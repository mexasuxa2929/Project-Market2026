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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun AddBrandDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    editBrand: BrandItem? = null,           // null → yangi qo'shish; non-null → tahrirlash
) {
    val isEditMode = editBrand != null
    val scope = rememberCoroutineScope()

    var name        by remember { mutableStateOf(editBrand?.name ?: "") }
    var nameError   by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var active      by remember { mutableStateOf(editBrand?.active ?: true) }

    var logoFile   by remember { mutableStateOf<java.io.File?>(null) }
    var logoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var saving    by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun pickLogo() {
        scope.launch(Dispatchers.Main) {
            val chooser = javax.swing.JFileChooser()
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "Logo (PNG, JPG, SVG)", "png", "jpg", "jpeg", "svg",
            )
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                if (file.length() <= 2L * 1024 * 1024) {
                    logoFile = file
                    logoBitmap = if (!file.name.endsWith(".svg", ignoreCase = true)) {
                        runCatching {
                            SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                        }.getOrNull()
                    } else null
                }
            }
        }
    }

    fun validate(): Boolean {
        nameError = if (name.isBlank()) "Brend nomi majburiy" else null
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
                        if (description.isNotBlank()) append("\"description\":\"${description.trim().replace("\"", "\\\"")}\",")
                        append("\"active\":$active")
                        append("}")
                    }
                    val responseText = if (isEditMode) {
                        ApiClient.put("/api/brands/${editBrand!!.id}", body)
                    } else {
                        ApiClient.post("/api/brands", body)
                    }

                    val file = logoFile
                    if (file != null) {
                        val brandIdStr = if (isEditMode) {
                            editBrand!!.id
                        } else {
                            val root = ApiClient.parseJsonObject(responseText)
                            extractBrandId(root)
                        }
                        if (brandIdStr != null) {
                            ApiClient.uploadFile(
                                "/api/brands/$brandIdStr/logo",
                                file.name,
                                file.readBytes(),
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
            modifier = Modifier.widthIn(min = 340.dp, max = 560.dp).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.heightIn(max = 700.dp)) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            if (isEditMode) "Brendni tahrirlash" else "Yangi brend qo'shish",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        Text(
                            if (isEditMode) "\"${editBrand!!.name}\" brendini tahrirlash" else "Mahsulot brendi ma'lumotlarini kiriting",
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

                // ── Form ──────────────────────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Brend nomi
                    BrandFieldBlock("Brend nomi *", error = nameError) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = null },
                            placeholder = { Text("Masalan: Nike, Samsung", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                            singleLine = true,
                            isError = nameError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = brandFieldColors(),
                            textStyle = TextStyle(fontSize = 14.sp),
                        )
                    }

                    // Tavsif
                    BrandFieldBlock("Tavsif") {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Brend haqida qisqacha ma'lumot...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            maxLines = 4,
                            shape = RoundedCornerShape(8.dp),
                            colors = brandFieldColors(),
                            textStyle = TextStyle(fontSize = 14.sp),
                        )
                    }

                    // Logo yuklash
                    BrandFieldBlock("Brend logotipi") {
                        if (logoFile != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                                        .background(MexaWarehouseColors.surfaceContainerLow),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (logoBitmap != null) {
                                        Image(
                                            bitmap = logoBitmap!!,
                                            contentDescription = "Logo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Image,
                                            contentDescription = null,
                                            tint = MexaWarehouseColors.primary,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(logoFile!!.name, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            "O'zgartirish",
                                            fontSize = 12.sp,
                                            color = MexaWarehouseColors.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { pickLogo() },
                                        )
                                        Text(
                                            "O'chirish",
                                            fontSize = 12.sp,
                                            color = MexaWarehouseColors.danger,
                                            modifier = Modifier.clickable { logoFile = null; logoBitmap = null },
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                onClick = { pickLogo() },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MexaWarehouseColors.backgroundPage,
                                border = BorderStroke(1.5.dp, MexaWarehouseColors.borderSubtle),
                            ) {
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .background(MexaWarehouseColors.surfaceLowest, CircleShape)
                                            .border(1.dp, MexaWarehouseColors.borderSubtle, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null,
                                            tint = MexaWarehouseColors.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("Logo yuklash (Max 2MB)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                    Text("Format: PNG, JPG, SVG tavsiya etiladi.", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }

                    // Holati
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
                            Column {
                                Text("Holati", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                Text("Brend omborda ko'rinishini sozlash", fontSize = 12.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Switch(
                                    checked = active,
                                    onCheckedChange = { active = it },
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

                // Error
                if (saveError != null) {
                    Text(
                        saveError!!,
                        color = MexaWarehouseColors.danger,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Footer ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.backgroundPage)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                    ) {
                        Text("Bekor qilish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isEditMode) "Yangilash" else "Saqlash", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun extractBrandId(root: JsonObject?): String? {
    val data = root?.get("data") as? JsonObject ?: return null
    return (data["id"] as? JsonPrimitive)?.content?.ifBlank { null }
}

@Composable
private fun BrandFieldBlock(
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
private fun brandFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor    = MexaWarehouseColors.outlineVariant,
    focusedBorderColor      = MexaWarehouseColors.primary,
    unfocusedContainerColor = MexaWarehouseColors.surfaceLowest,
    focusedContainerColor   = MexaWarehouseColors.surfaceLowest,
)

package mexa.club.desktop_app.market.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddProductDialog(
    categories: List<DropdownItem>,
    brands: List<DropdownItem>,
    onDismiss: () -> Unit,
    onSaved: (List<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // ── Tabs ───────────────────────────────────────────────────────────────────
    val tabs = listOf("Asosiy", "Narx", "Qo'shimcha", "Rasmlar")
    var selectedTab by remember { mutableStateOf(0) }

    // ── Asosiy ────────────────────────────────────────────────────────────────
    var name         by remember { mutableStateOf("") }
    var nameError    by remember { mutableStateOf<String?>(null) }
    // Rang-barcode juftliklari: har bir rang o'z barcode'iga ega
    data class ColorBarcodeEntry(
        val colorName: String,
        val colorCode: String,   // HEX kodi, masalan "#FF0000"
        val barcode: String,
        val barcodeError: String? = null,
        val showPicker: Boolean = false,
    )
    var colorBarcodes by remember { mutableStateOf(listOf(ColorBarcodeEntry("", "", ""))) }
    var unit         by remember { mutableStateOf("dona") }
    var selCatId     by remember { mutableStateOf("") }
    var selCatName   by remember { mutableStateOf("Tanlang") }
    var selBrandId   by remember { mutableStateOf("") }
    var selBrandName by remember { mutableStateOf("Tanlang") }
    var manufacturerName by remember { mutableStateOf("") }
    var active       by remember { mutableStateOf(true) }
    var featured     by remember { mutableStateOf(false) }
    var digital      by remember { mutableStateOf(false) }
    var status       by remember { mutableStateOf("DRAFT") }
    var statusExp    by remember { mutableStateOf(false) }
    val statusOpts   = listOf("DRAFT" to "Qoralama", "PUBLISHED" to "Nashr etilgan", "ARCHIVED" to "Arxivlangan")
    var catExp       by remember { mutableStateOf(false) }
    var catSearch    by remember { mutableStateOf("") }
    var brandExp     by remember { mutableStateOf(false) }
    var brandSearch  by remember { mutableStateOf("") }

    // ── Narx ──────────────────────────────────────────────────────────────────
    var localTiers   by remember { mutableStateOf<List<PriceTierItem>>(emptyList()) }

    // ── Qo'shimcha ────────────────────────────────────────────────────────────
    var description      by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }
    var minStock         by remember { mutableStateOf("0") }
    var leadTimeDays     by remember { mutableStateOf("0") }
    var weight           by remember { mutableStateOf("") }
    var dimL             by remember { mutableStateOf("") }
    var dimW             by remember { mutableStateOf("") }
    var dimH             by remember { mutableStateOf("") }
    var packageType      by remember { mutableStateOf("") }
    var fragile          by remember { mutableStateOf(false) }
    var material         by remember { mutableStateOf("") }
    var countryOfOrigin  by remember { mutableStateOf("") }
    var warrantyMonths   by remember { mutableStateOf("") }
    var sku              by remember { mutableStateOf("") }
    var slug             by remember { mutableStateOf("") }
    var metaDescription  by remember { mutableStateOf("") }
    var tags             by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput         by remember { mutableStateOf("") }
    var suggestedTags    by remember { mutableStateOf<List<String>>(emptyList()) }

    // ── Rasmlar ───────────────────────────────────────────────────────────────
    var selectedFiles  by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var previewBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    // ── Tarjimalar ───────────────────────────────────────────────────────────
    var nameUz         by remember { mutableStateOf("") }
    var nameRu         by remember { mutableStateOf("") }
    var descUz         by remember { mutableStateOf("") }
    var descRu         by remember { mutableStateOf("") }
    var shortDescUz    by remember { mutableStateOf("") }
    var shortDescRu    by remember { mutableStateOf("") }
    var materialUz     by remember { mutableStateOf("") }
    var materialRu     by remember { mutableStateOf("") }
    var countryUz      by remember { mutableStateOf("") }
    var countryRu      by remember { mutableStateOf("") }
    var manufacturerUz by remember { mutableStateOf("") }
    var manufacturerRu by remember { mutableStateOf("") }

    // ── Saqlash ───────────────────────────────────────────────────────────────
    var saving    by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // ── Load suggested tags ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/products/tags")
                val root = ApiClient.parseJsonObject(text)
                (root?.get("data") as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    ?: emptyList()
            }
        }.onSuccess { suggestedTags = it }
    }

    fun addTag() {
        val t = tagInput.trim().lowercase()
        if (t.isNotEmpty() && t !in tags) tags = tags + t
        tagInput = ""
    }

    fun pickImages() {
        scope.launch(Dispatchers.Main) {
            val chooser = javax.swing.JFileChooser()
            chooser.isMultiSelectionEnabled = true
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "Rasmlar (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp",
            )
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                val remaining = 5 - selectedFiles.size
                val newFiles  = chooser.selectedFiles
                    .filter { it.length() <= 5L * 1024 * 1024 }
                    .take(remaining)
                val newBitmaps = newFiles.associate { f ->
                    f.name to runCatching {
                        SkiaImage.makeFromEncoded(f.readBytes()).toComposeImageBitmap()
                    }.getOrNull()
                }.filterValues { it != null }.mapValues { it.value!! }
                selectedFiles  = (selectedFiles + newFiles).take(5)
                previewBitmaps = previewBitmaps + newBitmaps
            }
        }
    }

    fun validate(): Boolean {
        nameError = if (name.isBlank()) "Mahsulot nomi majburiy" else null
        val updatedCB = colorBarcodes.map { entry ->
            entry.copy(barcodeError = if (entry.barcode.isBlank()) "Barcode majburiy" else null)
        }
        colorBarcodes = updatedCB
        val hasBarcodeError = updatedCB.any { it.barcodeError != null }
        if (nameError != null || hasBarcodeError) selectedTab = 0
        return nameError == null && !hasBarcodeError
    }

    fun save() {
        if (!validate()) return
        scope.launch {
            saving    = true
            saveError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val tagsJson = tags.joinToString(",") { "\"$it\"" }
                    val firstEntry = colorBarcodes.first()
                    val body = buildString {
                        append("{")
                        append("\"name\":\"${name.trim().replace("\"", "\\\"")}\",")
                        append("\"barcode\":\"${firstEntry.barcode.trim().replace("\"", "\\\"")}\",")
                        if (firstEntry.colorName.isNotBlank()) append("\"color\":\"${firstEntry.colorName.trim().replace("\"", "\\\"")}\",")
                        if (firstEntry.colorCode.isNotBlank()) append("\"colorCode\":\"${firstEntry.colorCode.trim().replace("\"", "\\\"")}\",")
                        append("\"unit\":\"${unit.replace("\"", "\\\"")}\",")
                        if (selCatId.isNotBlank())  append("\"categoryId\":\"$selCatId\",")
                        if (selBrandId.isNotBlank()) append("\"brandId\":\"$selBrandId\",")
                        if (manufacturerName.isNotBlank()) append("\"manufacturerName\":\"${manufacturerName.trim().replace("\"", "\\\"")}\",")
                        append("\"leadTimeDays\":${leadTimeDays.toIntOrNull() ?: 0},")
                        append("\"minStock\":${minStock.toIntOrNull() ?: 0},")
                        append("\"weight\":${weight.toDoubleOrNull() ?: 0.0},")
                        append("\"length\":${dimL.toDoubleOrNull() ?: 0.0},")
                        append("\"width\":${dimW.toDoubleOrNull() ?: 0.0},")
                        append("\"height\":${dimH.toDoubleOrNull() ?: 0.0},")
                        if (packageType.isNotBlank()) append("\"packageType\":\"${packageType.trim().replace("\"", "\\\"")}\",")
                        append("\"fragile\":$fragile,")
                        append("\"active\":$active,")
                        append("\"featured\":$featured,")
                        append("\"digital\":$digital,")
                        append("\"status\":\"$status\",")
                        if (description.isNotBlank())      append("\"description\":\"${description.trim().replace("\\", "\\\\").replace("\"", "\\\"")}\",")
                        if (shortDescription.isNotBlank()) append("\"shortDescription\":\"${shortDescription.trim().replace("\"", "\\\"")}\",")
                        if (sku.isNotBlank())              append("\"sku\":\"${sku.trim().replace("\"", "\\\"")}\",")
                        if (slug.isNotBlank())             append("\"slug\":\"${slug.trim().lowercase().replace("\"", "\\\"")}\",")
                        if (metaDescription.isNotBlank())  append("\"metaDescription\":\"${metaDescription.trim().replace("\"", "\\\"")}\",")
                        if (material.isNotBlank())         append("\"material\":\"${material.trim().replace("\"", "\\\"")}\",")
                        if (countryOfOrigin.isNotBlank())  append("\"countryOfOrigin\":\"${countryOfOrigin.trim().replace("\"", "\\\"")}\",")
                        val wm = warrantyMonths.toIntOrNull() ?: 0
                        if (wm > 0) append("\"warrantyMonths\":$wm,")
                        // Tarjimalar
                        append("\"nameTranslations\":{")
                        append("\"uz\":\"${nameUz.trim().replace("\"", "\\\"")}\",")
                        append("\"ru\":\"${nameRu.trim().replace("\"", "\\\"")}\"")
                        append("},")
                        append("\"descriptionTranslations\":{")
                        append("\"uz\":\"${descUz.trim().replace("\\", "\\\\").replace("\"", "\\\"")}\",")
                        append("\"ru\":\"${descRu.trim().replace("\\", "\\\\").replace("\"", "\\\"")}\"")
                        append("},")
                        append("\"shortDescriptionTranslations\":{")
                        append("\"uz\":\"${shortDescUz.trim().replace("\"", "\\\"")}\",")
                        append("\"ru\":\"${shortDescRu.trim().replace("\"", "\\\"")}\"")
                        append("},")
                        append("\"materialTranslations\":{")
                        append("\"uz\":\"${materialUz.trim().replace("\"", "\\\"")}\",")
                        append("\"ru\":\"${materialRu.trim().replace("\"", "\\\"")}\"")
                        append("},")
                        append("\"countryOfOriginTranslations\":{")
                        append("\"uz\":\"${countryUz.trim().replace("\"", "\\\"")}\",")
                        append("\"ru\":\"${countryRu.trim().replace("\"", "\\\"")}\"")
                        append("},")
                        append("\"manufacturerNameTranslations\":{")
                        append("\"uz\":\"${manufacturerUz.trim().replace("\"", "\\\"")}\",")
                        append("\"ru\":\"${manufacturerRu.trim().replace("\"", "\\\"")}\"")
                        append("},")
                        append("\"tags\":[$tagsJson]")
                        append("}")
                    }

                    val responseText = if (selectedFiles.isEmpty()) {
                        ApiClient.post("/api/products", body)
                    } else {
                        val filePairs = selectedFiles.map { f -> f.name to f.readBytes() }
                        ApiClient.postMultipart("/api/products", body, filePairs)
                    }

                    val newId = ApiClient.parseJsonObject(responseText)
                        ?.let { it["data"] as? kotlinx.serialization.json.JsonObject }
                        ?.let { (it["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content }

                    if (newId.isNullOrBlank()) {
                        throw IllegalStateException("Mahsulot yaratildi, lekin ID qaytarilmadi")
                    }

                    // Qo'shimcha qadamlar: xato bo'lsa ham mahsulot yaratilgan —
                    // xatolarni yig'ib, foydalanuvchiga ogohlantirish sifatida qaytaramiz.
                    val warnings = mutableListOf<String>()

                    // Tannarx warehouse-service da boshqariladi — product narxi bu yerda yaratilmaydi
                    if (localTiers.isNotEmpty()) {
                        runCatching {
                            val tiersJson = localTiers.joinToString(",") { t ->
                                buildString {
                                    append("{\"minQty\":${t.minQty},")
                                    if (t.maxQty != null) append("\"maxQty\":${t.maxQty},")
                                    append("\"price\":${t.price},")
                                    append("\"priceType\":\"${t.priceType}\",")
                                    append("\"currency\":\"${t.currency}\"}")
                                }
                            }
                            ApiClient.put("/api/products/$newId/price-tiers/bulk", "{\"tiers\":[$tiersJson]}")
                        }.onFailure { e ->
                            warnings += "Narx shkalalari saqlanmadi: ${e.message ?: "noma'lum xato"}"
                        }
                    }
                    // Qo'shimcha rang variantlarini sibling sifatida yaratish
                    colorBarcodes.drop(1).forEach { entry ->
                        if (entry.barcode.isNotBlank()) {
                            val siblingBody = buildString {
                                append("{")
                                append("\"name\":\"${name.trim().replace("\"", "\\\"")}\",")
                                append("\"barcode\":\"${entry.barcode.trim().replace("\"", "\\\"")}\",")
                                if (entry.colorName.isNotBlank()) append("\"color\":\"${entry.colorName.trim().replace("\"", "\\\"")}\",")
                                if (entry.colorCode.isNotBlank()) append("\"colorCode\":\"${entry.colorCode.trim().replace("\"", "\\\"")}\",")
                                if (selCatId.isNotBlank())   append("\"categoryId\":\"$selCatId\",")
                                if (selBrandId.isNotBlank()) append("\"brandId\":\"$selBrandId\",")
                                if (manufacturerName.isNotBlank()) append("\"manufacturerName\":\"${manufacturerName.trim().replace("\"", "\\\"")}\",")
                                                append("\"unit\":\"${unit.replace("\"", "\\\"")}\",")
                                                append("\"nameTranslations\":{")
                                                append("\"uz\":\"${nameUz.trim().replace("\"", "\\\"")}\",")
                                                append("\"ru\":\"${nameRu.trim().replace("\"", "\\\"")}\"")
                                                append("},")
                                                append("\"active\":$active,")
                                                append("\"status\":\"$status\"")
                                                append("}")
                            }
                            runCatching { ApiClient.post("/api/products/$newId/colors", siblingBody) }
                                .onFailure { e ->
                                    warnings += "Rang varianti (${entry.colorName.ifEmpty { entry.barcode }}) yaratilmadi: ${e.message ?: "noma'lum xato"}"
                                }
                        }
                    }

                    warnings
                }
            }.onSuccess { warnings ->
                onSaved(warnings)
                onDismiss()
            }.onFailure {
                saveError = it.message ?: "Saqlashda xatolik yuz berdi"
            }
            saving = false
        }
    }

    // ── Dialog ─────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier       = Modifier.widthIn(min = 460.dp, max = 800.dp).fillMaxWidth(0.92f),
            shape          = RoundedCornerShape(16.dp),
            color          = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.heightIn(max = 800.dp)) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Yangi mahsulot qo'shish",
                            fontSize   = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MexaWarehouseColors.textPrimary,
                        )
                        Text(
                            "Barcha majburiy maydonlarni (*) to'ldiring",
                            fontSize = 12.sp,
                            color    = MexaWarehouseColors.textMuted,
                        )
                    }
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Tabs ──────────────────────────────────────────────────────
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MexaWarehouseColors.surfaceLowest,
                    contentColor     = MexaWarehouseColors.primary,
                    indicator        = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(2.dp)
                                .background(MexaWarehouseColors.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        )
                    },
                    divider = {},
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick  = { selectedTab = idx },
                            text     = {
                                Text(
                                    title,
                                    fontSize   = 13.sp,
                                    fontWeight = if (selectedTab == idx) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (selectedTab == idx) MexaWarehouseColors.primary else MexaWarehouseColors.textMuted,
                                )
                            },
                        )
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                // ── Tab content ───────────────────────────────────────────────
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    (0..3).forEach { tabIdx ->
                        Box(
                            modifier = if (tabIdx == selectedTab) Modifier.fillMaxSize()
                                       else Modifier.size(0.dp)
                        ) {
                    when (tabIdx) {

                        // ══ ASOSIY ════════════════════════════════════════════
                        0 -> Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {

                            // Mahsulot nomi
                            PField("Mahsulot nomi *", nameError) {
                                PTextField(
                                    value    = name,
                                    onChange = { name = it; nameError = null },
                                    hint     = "Masalan: Samsung Galaxy S24",
                                    isError  = nameError != null,
                                )
                            }

                            // O'lchov birligi
                            PField("O'lchov birligi *") {
                                val units = listOf("dona", "kg", "litr", "m", "m²", "l", "box")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    units.forEach { u ->
                                        val sel = unit == u
                                        Surface(
                                            onClick  = { unit = u },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape    = RoundedCornerShape(8.dp),
                                            color    = if (sel) MexaWarehouseColors.primary.copy(alpha = 0.08f) else MexaWarehouseColors.surfaceLowest,
                                            border   = BorderStroke(1.dp, if (sel) MexaWarehouseColors.primary else MexaWarehouseColors.outlineVariant),
                                        ) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    u,
                                                    fontSize   = 12.sp,
                                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                                    color      = if (sel) MexaWarehouseColors.primary else MexaWarehouseColors.textMuted,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Kategoriya + Brend
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    PFieldLabel("Kategoriya")
                                    PDropdown(
                                        expanded       = catExp,
                                        onExpand       = { catExp = it; if (!it) catSearch = "" },
                                        selectedName   = selCatName,
                                        searchValue    = catSearch,
                                        onSearchChange = { catSearch = it },
                                        items          = categories.filter { catSearch.isBlank() || it.name.contains(catSearch, ignoreCase = true) },
                                        emptyLabel     = "Kategoriyani tanlang",
                                        onClear        = { selCatId = ""; selCatName = "Tanlang"; catExp = false; catSearch = "" },
                                        onSelect       = { selCatId = it.id; selCatName = it.name; catExp = false; catSearch = "" },
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    PFieldLabel("Brend")
                                    PDropdown(
                                        expanded       = brandExp,
                                        onExpand       = { brandExp = it; if (!it) brandSearch = "" },
                                        selectedName   = selBrandName,
                                        searchValue    = brandSearch,
                                        onSearchChange = { brandSearch = it },
                                        items          = brands.filter { brandSearch.isBlank() || it.name.contains(brandSearch, ignoreCase = true) },
                                        emptyLabel     = "Brendni tanlang",
                                        onClear        = { selBrandId = ""; selBrandName = "Tanlang"; brandExp = false; brandSearch = "" },
                                        onSelect       = { selBrandId = it.id; selBrandName = it.name; brandExp = false; brandSearch = "" },
                                    )
                                }
                            }

                            // Ishlab chiqaruvchi
                            Column(Modifier.fillMaxWidth()) {
                                PFieldLabel("Ishlab chiqaruvchi")
                                OutlinedTextField(
                                    value         = manufacturerName,
                                    onValueChange = { manufacturerName = it },
                                    placeholder   = { Text("Masalan: Samsung, Apple...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    singleLine    = true,
                                    enabled       = !saving,
                                    modifier      = Modifier.fillMaxWidth(),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 13.sp),
                                )
                            }

                            // Ranglar va barcodelar — har bir rang uchun alohida barcode
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "RANGLAR VA BARCODELAR",
                                        fontSize      = 11.sp,
                                        fontWeight    = FontWeight.SemiBold,
                                        color         = MexaWarehouseColors.textMuted,
                                        letterSpacing = 0.5.sp,
                                    )
                                    Text(
                                        "Har bir rang uchun alohida barcode",
                                        fontSize = 11.sp,
                                        color    = MexaWarehouseColors.textMuted,
                                    )
                                }

                                colorBarcodes.forEachIndexed { idx, entry ->
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(10.dp))
                                            .border(1.dp, if (entry.barcodeError != null) MexaWarehouseColors.danger else MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        // Sarlavha + o'chirish
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val previewColor = entry.colorCode.takeIf { it.isNotBlank() }?.let { hex ->
                                                val clean = hex.trim().removePrefix("#")
                                                runCatching {
                                                    val v = clean.toLong(16)
                                                    Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f)
                                                }.getOrNull()
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Box(
                                                    Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(previewColor ?: MexaWarehouseColors.outlineVariant)
                                                        .border(1.dp, MexaWarehouseColors.outlineVariant, CircleShape)
                                                )
                                                Text(
                                                    if (idx == 0) "Asosiy variant" else "Rang variant ${idx + 1}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MexaWarehouseColors.textMuted,
                                                )
                                            }
                                            if (colorBarcodes.size > 1) {
                                                Box(
                                                    Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(MexaWarehouseColors.danger.copy(alpha = 0.1f))
                                                        .clickable { colorBarcodes = colorBarcodes.toMutableList().also { it.removeAt(idx) } },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(Icons.Filled.Close, contentDescription = "O'chirish", tint = MexaWarehouseColors.danger, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }

                                        // Barcode + Rang nomi
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PField(
                                                label    = "Barcode *",
                                                error    = entry.barcodeError,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                PTextField(
                                                    value    = entry.barcode,
                                                    onChange = { v ->
                                                        colorBarcodes = colorBarcodes.toMutableList().also {
                                                            it[idx] = entry.copy(barcode = v, barcodeError = null)
                                                        }
                                                    },
                                                    hint    = "8690000123456",
                                                    isError = entry.barcodeError != null,
                                                    keyboard = KeyboardType.Number,
                                                )
                                            }
                                            PField(
                                                label    = "Rang nomi",
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                PTextField(
                                                    value    = entry.colorName,
                                                    onChange = { v ->
                                                        colorBarcodes = colorBarcodes.toMutableList().also {
                                                            it[idx] = entry.copy(colorName = v)
                                                        }
                                                    },
                                                    hint = "Qora, Oq, Ko'k...",
                                                )
                                            }
                                        }

                                        // Color picker — preset doiralar
                                        val presetColors = listOf(
                                            "#FFFFFF" to "Oq", "#F5F5F5" to "Kumush oq", "#D3D3D3" to "Kulrang och",
                                            "#808080" to "Kulrang", "#404040" to "To'q kulrang", "#000000" to "Qora",
                                            "#FFD700" to "Oltin", "#FFA500" to "To'q sariq", "#FF6347" to "Tarvuz",
                                            "#FF0000" to "Qizil", "#DC143C" to "Krem qizil", "#8B0000" to "To'q qizil",
                                            "#FFC0CB" to "Pushti", "#FF69B4" to "Pushti to'q", "#9400D3" to "Binafsha",
                                            "#4B0082" to "To'q binafsha", "#0000FF" to "Ko'k", "#1E90FF" to "Moviy",
                                            "#00BFFF" to "Och moviy", "#00CED1" to "Feruza", "#008080" to "Zangori",
                                            "#00FF7F" to "Yashil och", "#008000" to "Yashil", "#006400" to "To'q yashil",
                                            "#FFFF00" to "Sariq", "#F5DEB3" to "Bug'doy", "#D2691E" to "Shokolad",
                                            "#8B4513" to "Jigarrang", "#A0522D" to "Sienna",
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Preset doiralar
                                            androidx.compose.foundation.layout.FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement   = Arrangement.spacedBy(6.dp),
                                            ) {
                                                presetColors.forEach { (hex, colorLabel) ->
                                                    val bgColor = runCatching {
                                                        val clean = hex.removePrefix("#")
                                                        val v = clean.toLong(16)
                                                        Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f)
                                                    }.getOrElse { Color.Gray }
                                                    val isSelected = entry.colorCode.uppercase() == hex.uppercase()
                                                    Box(
                                                        Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(bgColor)
                                                            .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) MexaWarehouseColors.primary else Color(0xFFCCCCCC), CircleShape)
                                                            .clickable {
                                                                colorBarcodes = colorBarcodes.toMutableList().also {
                                                                    it[idx] = entry.copy(colorCode = hex.uppercase(), colorName = if (entry.colorName.isBlank()) colorLabel else entry.colorName)
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        if (isSelected) {
                                                            val lum = 0.299f * bgColor.red + 0.587f * bgColor.green + 0.114f * bgColor.blue
                                                            Icon(Icons.Filled.Close, contentDescription = null, tint = if (lum > 0.5f) Color(0xFF1A1A1A) else Color.White, modifier = Modifier.size(11.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            // HEX kod qo'lda kiritish
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                val hexPreview = runCatching {
                                                    val clean = entry.colorCode.removePrefix("#")
                                                    if (clean.length == 6) {
                                                        val v = clean.toLong(16)
                                                        Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f)
                                                    } else null
                                                }.getOrNull()
                                                Box(
                                                    Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                                                        .background(hexPreview ?: Color(0xFFEEEEEE))
                                                        .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(6.dp))
                                                )
                                                OutlinedTextField(
                                                    value         = entry.colorCode,
                                                    onValueChange = { v ->
                                                        colorBarcodes = colorBarcodes.toMutableList().also {
                                                            it[idx] = entry.copy(colorCode = v.uppercase().take(7))
                                                        }
                                                    },
                                                    placeholder   = { Text("HEX: #FF0000", fontSize = 12.sp, color = MexaWarehouseColors.textMuted) },
                                                    singleLine    = true,
                                                    modifier      = Modifier.weight(1f),
                                                    shape         = RoundedCornerShape(8.dp),
                                                    colors        = pFieldColors(),
                                                    textStyle     = TextStyle(fontSize = 13.sp),
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick  = { colorBarcodes = colorBarcodes + ColorBarcodeEntry("", "", "") },
                                    enabled  = !saving,
                                    shape    = RoundedCornerShape(8.dp),
                                    border   = BorderStroke(1.dp, MexaWarehouseColors.primary),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("+ Rang qo'shish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }

                            // Nashr holati
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    PFieldLabel("Nashr holati")
                                    ExposedDropdownMenuBox(
                                        expanded        = statusExp,
                                        onExpandedChange = { statusExp = it },
                                    ) {
                                        OutlinedTextField(
                                            value       = statusOpts.firstOrNull { it.first == status }?.second ?: status,
                                            onValueChange = {},
                                            readOnly    = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExp) },
                                            modifier    = Modifier.menuAnchor().fillMaxWidth(),
                                            shape       = RoundedCornerShape(8.dp),
                                            colors      = pFieldColors(),
                                            textStyle   = TextStyle(fontSize = 13.sp),
                                            singleLine  = true,
                                        )
                                        ExposedDropdownMenu(expanded = statusExp, onDismissRequest = { statusExp = false }) {
                                            statusOpts.forEach { (value, label) ->
                                                DropdownMenuItem(
                                                    text    = { Text(label, fontSize = 13.sp) },
                                                    onClick = { status = value; statusExp = false },
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PToggle("Faol", "Ro'yxatda ko'rinadi", active) { active = it }
                                }
                            }

                            // Featured + Digital
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.weight(1f)) { PToggle("Tavsiya etilgan", "Bosh sahifada ko'rsatish", featured) { featured = it } }
                                Box(Modifier.weight(1f)) { PToggle("Raqamli mahsulot", "Jismoniy yetkazib berish yo'q", digital) { digital = it } }
                            }
                        }

                        // ══ NARX ══════════════════════════════════════════════
                        1 -> Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Text(
                                "MIQDORGA BOG'LIQ NARX BOSQICHLARI",
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.SemiBold,
                                color         = MexaWarehouseColors.textMuted,
                                letterSpacing = 0.5.sp,
                            )

                            ProductPriceTierSection(
                                productId           = "",
                                enabled             = !saving,
                                externalTiers       = localTiers,
                                onLocalTiersChanged = { localTiers = it },
                            )
                        }

                        // ══ QO'SHIMCHA ════════════════════════════════════════
                        2 -> Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Tavsif
                            Text("TAVSIF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)

                            PField("To'liq tavsif") {
                                OutlinedTextField(
                                    value         = description,
                                    onValueChange = { description = it },
                                    placeholder   = { Text("Mahsulot haqida batafsil ma'lumot...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    modifier      = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 160.dp),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 14.sp),
                                    maxLines      = 7,
                                )
                            }

                            PField("Qisqa tavsif (max 300 belgi)") {
                                OutlinedTextField(
                                    value         = shortDescription,
                                    onValueChange = { if (it.length <= 300) shortDescription = it },
                                    placeholder   = { Text("Bir necha so'z bilan...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    singleLine    = true,
                                    modifier      = Modifier.fillMaxWidth(),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 14.sp),
                                    trailingIcon  = { Text("${shortDescription.length}/300", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(end = 8.dp)) },
                                )
                            }

                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                            Text("FIZIK XUSUSIYATLAR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)

                            // Og'irligi + O'lchamlari
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Og'irligi (kg)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = weight, onChange = { weight = it }, hint = "1.5", keyboard = KeyboardType.Decimal)
                                }
                                PField("Minimal qoldiq (dona)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = minStock, onChange = { if (it.all(Char::isDigit)) minStock = it }, hint = "10", keyboard = KeyboardType.Number)
                                }
                                PField("Yetkazib berish (kun)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = leadTimeDays, onChange = { if (it.all(Char::isDigit)) leadTimeDays = it }, hint = "3", keyboard = KeyboardType.Number)
                                }
                            }

                            PField("O'lchamlari: Uzunlik / Eni / Balandlik (sm)") {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        Triple(dimL, { v: String -> dimL = v }, "30"),
                                        Triple(dimW, { v: String -> dimW = v }, "20"),
                                        Triple(dimH, { v: String -> dimH = v }, "15"),
                                    ).forEach { (value, setter, hint) ->
                                        PTextField(value = value, onChange = setter, hint = hint, keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Materiali", modifier = Modifier.weight(1f)) {
                                    PTextField(value = material, onChange = { material = it }, hint = "Plastik, metal...")
                                }
                                PField("Ishlab chiqarilgan mamlakat", modifier = Modifier.weight(1f)) {
                                    PTextField(value = countryOfOrigin, onChange = { countryOfOrigin = it }, hint = "Xitoy, Germaniya...")
                                }
                                PField("Kafolat (oy)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = warrantyMonths, onChange = { if (it.all(Char::isDigit)) warrantyMonths = it }, hint = "12", keyboard = KeyboardType.Number)
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                                PField("Qadoq turi", modifier = Modifier.weight(2f)) {
                                    PTextField(value = packageType, onChange = { packageType = it }, hint = "Karton quti, polietilen...")
                                }
                                Row(
                                    Modifier.weight(1f).padding(bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Checkbox(
                                        checked         = fragile,
                                        onCheckedChange = { fragile = it },
                                        colors          = CheckboxDefaults.colors(checkedColor = MexaWarehouseColors.primary),
                                    )
                                    Text("Tez sinuvchan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                }
                            }

                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                            Text("TEGLAR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp))
                                    .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tags.forEach { tag ->
                                        Surface(shape = RoundedCornerShape(4.dp), color = MexaWarehouseColors.indigoAccent.copy(alpha = 0.10f)) {
                                            Row(
                                                Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.indigoAccent)
                                                Spacer(Modifier.width(4.dp))
                                                Box(Modifier.size(16.dp).clickable { tags = tags - tag }, contentAlignment = Alignment.Center) {
                                                    Text("×", fontSize = 14.sp, color = MexaWarehouseColors.indigoAccent.copy(alpha = 0.6f))
                                                }
                                            }
                                        }
                                    }
                                    BasicTextField(
                                        value         = tagInput,
                                        onValueChange = { raw ->
                                            if (raw.endsWith(",")) {
                                                val t = raw.dropLast(1).trim().lowercase()
                                                if (t.isNotEmpty() && t !in tags) tags = tags + t
                                                tagInput = ""
                                            } else tagInput = raw.lowercase()
                                        },
                                        modifier      = Modifier
                                            .widthIn(min = 140.dp)
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                            .onKeyEvent { e ->
                                                if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) { addTag(); true } else false
                                            },
                                        textStyle     = TextStyle(fontSize = 13.sp, color = MexaWarehouseColors.textPrimary),
                                        singleLine    = true,
                                        decorationBox = { inner ->
                                            if (tagInput.isEmpty() && tags.isEmpty()) {
                                                Text("Teg yozing... masalan: yangi,chegirma", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                                            }
                                            inner()
                                        },
                                    )
                                }
                            }

                            if (suggestedTags.isNotEmpty()) {
                                val notSel = suggestedTags.filter { it !in tags }.take(12)
                                if (notSel.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Tavsiya etilgan teglar:", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            notSel.forEach { tag ->
                                                Surface(
                                                    onClick  = { tags = tags + tag },
                                                    shape    = RoundedCornerShape(4.dp),
                                                    color    = MexaWarehouseColors.tableHeaderBg,
                                                    border   = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                                                ) {
                                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("+", fontSize = 11.sp, color = MexaWarehouseColors.primary, fontWeight = FontWeight.Bold)
                                                        Text(tag, fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                            Text("SEO VA ICHKI KODLAR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("SKU (ichki kod)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = sku, onChange = { sku = it }, hint = "SKU-001")
                                }
                                PField("Slug (URL uchun)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = slug, onChange = { slug = it.lowercase().replace(" ", "-") }, hint = "samsung-galaxy-s24")
                                }
                            }

                            PField("Meta tavsif (SEO, max 160 belgi)") {
                                OutlinedTextField(
                                    value         = metaDescription,
                                    onValueChange = { if (it.length <= 160) metaDescription = it },
                                    placeholder   = { Text("Qidiruvda ko'rinadigan qisqa tavsif...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    singleLine    = true,
                                    modifier      = Modifier.fillMaxWidth(),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 14.sp),
                                    trailingIcon  = { Text("${metaDescription.length}/160", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(end = 8.dp)) },
                                )
                            }

                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                            Text("TARJIMALAR (O'zbek va Rus tillarida)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Nomi (O'zbek)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = nameUz, onChange = { nameUz = it }, hint = "Mahsulot nomi o'zbekcha")
                                }
                                PField("Nomi (Русский)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = nameRu, onChange = { nameRu = it }, hint = "Название на русском")
                                }
                            }

                            PField("To'liq tavsif (O'zbek)") {
                                OutlinedTextField(
                                    value         = descUz,
                                    onValueChange = { descUz = it },
                                    placeholder   = { Text("O'zbek tilida to'liq tavsif...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    modifier      = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 14.sp),
                                    maxLines      = 4,
                                )
                            }

                            PField("Полное описание (Русский)") {
                                OutlinedTextField(
                                    value         = descRu,
                                    onValueChange = { descRu = it },
                                    placeholder   = { Text("Полное описание на русском...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    modifier      = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 14.sp),
                                    maxLines      = 4,
                                )
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Qisqa tavsif (O'zbek)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = shortDescUz, onChange = { shortDescUz = it }, hint = "Qisqacha o'zbekcha")
                                }
                                PField("Краткое описание (Русский)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = shortDescRu, onChange = { shortDescRu = it }, hint = "Кратко на русском")
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Material (O'zbek)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = materialUz, onChange = { materialUz = it }, hint = "Material o'zbekcha")
                                }
                                PField("Материал (Русский)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = materialRu, onChange = { materialRu = it }, hint = "Материал на русском")
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Ishlab chiqarilgan mamlakat (O'zbek)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = countryUz, onChange = { countryUz = it }, hint = "O'zbekcha")
                                }
                                PField("Страна производства (Русский)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = countryRu, onChange = { countryRu = it }, hint = "На русском")
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("Ishlab chiqaruvchi (O'zbek)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = manufacturerUz, onChange = { manufacturerUz = it }, hint = "O'zbekcha")
                                }
                                PField("Производитель (Русский)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = manufacturerRu, onChange = { manufacturerRu = it }, hint = "На русском")
                                }
                            }

                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                            Text("SEO VA ICHKI KODLAR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PField("SKU (ichki kod)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = sku, onChange = { sku = it }, hint = "SKU-001")
                                }
                                PField("Slug (URL uchun)", modifier = Modifier.weight(1f)) {
                                    PTextField(value = slug, onChange = { slug = it.lowercase().replace(" ", "-") }, hint = "samsung-galaxy-s24")
                                }
                            }

                            PField("Meta tavsif (SEO, max 160 belgi)") {
                                OutlinedTextField(
                                    value         = metaDescription,
                                    onValueChange = { if (it.length <= 160) metaDescription = it },
                                    placeholder   = { Text("Qidiruvda ko'rinadigan qisqa tavsif...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                                    singleLine    = true,
                                    modifier      = Modifier.fillMaxWidth(),
                                    shape         = RoundedCornerShape(8.dp),
                                    colors        = pFieldColors(),
                                    textStyle     = TextStyle(fontSize = 14.sp),
                                    trailingIcon  = { Text("${metaDescription.length}/160", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(end = 8.dp)) },
                                )
                            }
                        }

                        // ══ RASMLAR ═══════════════════════════════════════════
                        3 -> Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                "Mahsulot rasmlari (max 5 ta, har biri 5MB gacha)",
                                fontSize = 13.sp,
                                color    = MexaWarehouseColors.textMuted,
                            )

                            if (selectedFiles.size < 5) {
                                Surface(
                                    onClick  = { pickImages() },
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    shape    = RoundedCornerShape(12.dp),
                                    color    = MexaWarehouseColors.backgroundPage,
                                    border   = BorderStroke(1.5.dp, MexaWarehouseColors.borderSubtle),
                                ) {
                                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Box(
                                            Modifier.size(48.dp).background(MexaWarehouseColors.surfaceLowest, CircleShape).border(1.dp, MexaWarehouseColors.borderSubtle, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Filled.Image, contentDescription = null, tint = MexaWarehouseColors.primary, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text("Rasm yuklash", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                        Text("PNG, JPG, WEBP · 5MB gacha", fontSize = 12.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 2.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Fayl tanlash", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.primary)
                                    }
                                }
                            }

                            if (selectedFiles.isNotEmpty()) {
                                Text("${selectedFiles.size} ta rasm tanlangan", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    selectedFiles.forEach { file ->
                                        val bitmap = previewBitmaps[file.name]
                                        Box(
                                            Modifier
                                                .size(90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                                                .background(MexaWarehouseColors.surfaceContainerLow),
                                        ) {
                                            if (bitmap != null) {
                                                Image(bitmap = bitmap, contentDescription = file.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MexaWarehouseColors.outlineVariant)
                                                }
                                            }
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(22.dp)
                                                    .background(MexaWarehouseColors.danger, CircleShape)
                                                    .clickable { selectedFiles = selectedFiles - file; previewBitmaps = previewBitmaps - file.name },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "O'chirish", tint = Color.White, modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // when(tabIdx)
                        } // Box(tabIdx)
                    } // forEach
                }

                // ── Error ─────────────────────────────────────────────────────
                if (saveError != null) {
                    Text(
                        saveError!!,
                        color    = MexaWarehouseColors.danger,
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
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    // Tab navigatsiyasi
                    if (selectedTab > 0) {
                        OutlinedButton(
                            onClick  = { selectedTab-- },
                            enabled  = !saving,
                            shape    = RoundedCornerShape(8.dp),
                            border   = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                        ) {
                            Text("← Orqaga", fontSize = 13.sp)
                        }
                    }
                    if (selectedTab < tabs.lastIndex) {
                        OutlinedButton(
                            onClick  = { selectedTab++ },
                            enabled  = !saving,
                            shape    = RoundedCornerShape(8.dp),
                            border   = BorderStroke(1.dp, MexaWarehouseColors.primary),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.primary),
                        ) {
                            Text("Keyingi →", fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick  = { if (!saving) onDismiss() },
                        enabled  = !saving,
                        shape    = RoundedCornerShape(8.dp),
                        border   = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                    ) {
                        Text("Bekor qilish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick  = { save() },
                        enabled  = !saving,
                        colors   = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                        shape    = RoundedCornerShape(8.dp),
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

// ─── Private helpers ─────────────────────────────────────────────────────────────

@Composable
private fun PFieldLabel(text: String) {
    Text(
        text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.Medium,
        color      = MexaWarehouseColors.textPrimary,
        modifier   = Modifier.padding(bottom = 5.dp),
    )
}

@Composable
private fun PField(
    label: String,
    error: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        PFieldLabel(label)
        content()
        if (error != null) {
            Text(error, fontSize = 11.sp, color = MexaWarehouseColors.danger, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun PTextField(
    value: String,
    onChange: (String) -> Unit,
    hint: String = "",
    isError: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onChange,
        placeholder     = { Text(hint, fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
        singleLine      = true,
        isError         = isError,
        modifier        = modifier,
        shape           = RoundedCornerShape(8.dp),
        colors          = pFieldColors(),
        textStyle       = TextStyle(fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PDropdown(
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    selectedName: String,
    searchValue: String,
    onSearchChange: (String) -> Unit,
    items: List<DropdownItem>,
    emptyLabel: String,
    onClear: () -> Unit,
    onSelect: (DropdownItem) -> Unit,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpand) {
        OutlinedTextField(
            value         = if (expanded) searchValue else selectedName,
            onValueChange = onSearchChange,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp),
            colors        = pFieldColors(),
            textStyle     = TextStyle(fontSize = 13.sp),
            singleLine    = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
            DropdownMenuItem(
                text    = { Text(emptyLabel, fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                onClick = onClear,
            )
            if (items.isEmpty()) {
                DropdownMenuItem(text = { Text("Topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, onClick = {}, enabled = false)
            } else {
                items.forEach { item ->
                    DropdownMenuItem(
                        text    = { Text(item.name, fontSize = 13.sp) },
                        onClick = { onSelect(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(8.dp),
        color    = MexaWarehouseColors.backgroundPage,
        border   = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                Text(subtitle, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 1.dp))
            }
            Switch(
                checked         = checked,
                onCheckedChange = onChange,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor    = Color.White,
                    checkedTrackColor    = MexaWarehouseColors.primary,
                    uncheckedThumbColor  = Color.White,
                    uncheckedBorderColor = MexaWarehouseColors.outlineVariant,
                    uncheckedTrackColor  = MexaWarehouseColors.outlineVariant,
                ),
            )
        }
    }
}

@Composable
private fun pFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor    = MexaWarehouseColors.outlineVariant,
    focusedBorderColor      = MexaWarehouseColors.primary,
    unfocusedContainerColor = MexaWarehouseColors.surfaceLowest,
    focusedContainerColor   = MexaWarehouseColors.surfaceLowest,
)

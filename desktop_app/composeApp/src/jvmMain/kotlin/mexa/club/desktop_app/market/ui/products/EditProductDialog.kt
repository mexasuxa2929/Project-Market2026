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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.jetbrains.skia.Image as SkiaImage

data class ProductColorSiblingUi(
    val id: String,
    val color: String,
    val colorCode: String,
    val barcode: String,
    val imageUrl: String,
    val active: Boolean,
    val isCurrent: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProductDialog(
    product: ProductRowData,
    categories: List<DropdownItem>,
    brands: List<DropdownItem>,
    onDismiss: () -> Unit,
    onSaved: (List<String>) -> Unit,
    onOpenSibling: (ProductRowData) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    // Rang guruhidagi siblingga o'tganda butun dialogni yopib-qayta ochmaslik uchun (blink bo'lmasin):
    // faqat shu id ni almashtiramiz, LaunchedEffect(activeProductId) yangi mahsulot ma'lumotini yuklaydi.
    var activeProductId by remember { mutableStateOf(product.id) }

    var loadingProduct by remember { mutableStateOf(true) }
    // Sibling almashganda butun tabni blank qilmaslik uchun yengil yuklovchi:
    // faqat joriy item ichida kichik spinner ko'rsatiladi, dialog qayta yuklanmaydi.
    var switchingVariant by remember { mutableStateOf(false) }
    var initialLoad      by remember { mutableStateOf(true) }
    var loadError      by remember { mutableStateOf<String?>(null) }

    // ── Form state ─────────────────────────────────────────────────────────────
    var name          by remember { mutableStateOf(product.name) }
    var nameError     by remember { mutableStateOf<String?>(null) }
    var barcode       by remember { mutableStateOf(product.barcode) }
    var barcodeError  by remember { mutableStateOf<String?>(null) }
    var selCatId      by remember { mutableStateOf("") }
    var selCatName    by remember { mutableStateOf(product.categoryName.ifBlank { "Kategoriyani tanlang" }) }
    var selBrandId    by remember { mutableStateOf("") }
    var selBrandName  by remember { mutableStateOf(product.brandName.ifBlank { "Brendni tanlang" }) }
    var unit          by remember { mutableStateOf(product.unit) }
    var active        by remember { mutableStateOf(product.active) }

    var minStock      by remember { mutableStateOf("0") }
    var leadTimeDays  by remember { mutableStateOf("0") }
    var weight        by remember { mutableStateOf("") }
    var dimL          by remember { mutableStateOf("") }
    var dimW          by remember { mutableStateOf("") }
    var dimH          by remember { mutableStateOf("") }
    var packageType   by remember { mutableStateOf("") }
    var fragile       by remember { mutableStateOf(false) }

    var tier1Price      by remember { mutableStateOf("") }  // 1-liniya narxi — chegirma shu bazaga qo'llanadi
    var salePriceRow    by remember { mutableStateOf("") }  // flat sotuv narxi (liniya bo'lmasa baza)   // serverdan yuklangan asl qiymat
    var existingPriceId by remember { mutableStateOf<String?>(null) } // mavjud price yozuvi ID si

    // Chegirma (vaqt oralig'i bilan) — backend ProductPriceResponse yangi maydonlari
    var discountPercent      by remember { mutableStateOf("") } // % (0..100), bo'sh = o'zgartirilmaydi
    var discountStart        by remember { mutableStateOf("") } // yyyy-MM-dd
    var discountEnd          by remember { mutableStateOf("") } // yyyy-MM-dd
    var discountActiveFlag   by remember { mutableStateOf(false) } // serverdan: chegirma hozir faolmi
    var discountCurrentPrice by remember { mutableStateOf("") }    // serverdan: hisoblangan joriy narx
    var discountLoaded       by remember { mutableStateOf(false) } // narx yozuvi serverdan o'qildimi
    var startPickerOpen      by remember { mutableStateOf(false) } // sana tanlash oynasi (boshlanish)
    var endPickerOpen        by remember { mutableStateOf(false) } // sana tanlash oynasi (tugash)

    // Tavsif
    var description      by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }

    // SEO va kodlar
    var sku             by remember { mutableStateOf("") }
    var slug            by remember { mutableStateOf("") }
    var metaDescription by remember { mutableStateOf("") }

    // Qo'shimcha
    var material        by remember { mutableStateOf("") }
    var countryOfOrigin by remember { mutableStateOf("") }
    var warrantyMonths  by remember { mutableStateOf("") }

    // Status va flaglar
    var status         by remember { mutableStateOf("DRAFT") }
    var featured       by remember { mutableStateOf(false) }
    var digital        by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions  = listOf("DRAFT" to "Qoralama", "PUBLISHED" to "Nashr etilgan", "ARCHIVED" to "Arxivlangan")

    // Ranglar
    var colors     by remember { mutableStateOf<List<String>>(emptyList()) }
    var colorValue by remember { mutableStateOf("") }
    var colorCode  by remember { mutableStateOf("") }
    // "Asosiy variant" rang palitrasi default yopiq — chalkashlik bo'lmasligi uchun.
    var showColorPicker by remember { mutableStateOf(false) }

    var manufacturerName by remember { mutableStateOf("") }
    var selectedTab      by remember { mutableStateOf(0) }

    var tags          by remember { mutableStateOf(product.tags) }
    var tagInput      by remember { mutableStateOf("") }
    var suggestedTags by remember { mutableStateOf<List<String>>(emptyList()) }

    // Tarjimalar
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

    // Mavjud rasmlar (serverdan, tartibi o'zgaruvchan)
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var deletedUrls       by remember { mutableStateOf<Set<String>>(emptySet()) }
    var existingBitmaps   by remember { mutableStateOf<Map<String, ImageBitmap?>>(emptyMap()) }

    // Yangi qo'shilayotgan rasmlar
    var selectedFiles  by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var previewBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    var saving      by remember { mutableStateOf(false) }
    var saveError   by remember { mutableStateOf<String?>(null) }
    var activeUpdating by remember { mutableStateOf(false) }
    var activeError    by remember { mutableStateOf<String?>(null) }

    var catSearch   by remember { mutableStateOf("") }
    var brandSearch by remember { mutableStateOf("") }

    var catExpanded   by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }

    // Ranglar guruhi (sibling colors)
    var siblingColors        by remember { mutableStateOf<List<ProductColorSiblingUi>>(emptyList()) }
    var siblingColorsLoading by remember { mutableStateOf(false) }
    var siblingColorsError   by remember { mutableStateOf<String?>(null) }
    // Accordion: qaysi rang item ochiq (editor ko'rinadi). Default — joriy mahsulot.
    var expandedColorId      by remember { mutableStateOf(product.id) }

    // Yangi rang qo'shish formi
    var showAddColorForm  by remember { mutableStateOf(false) }
    var newColorName      by remember { mutableStateOf("") }
    var newColorCode      by remember { mutableStateOf("") }
    var newColorBarcode   by remember { mutableStateOf("") }
    var newColorSalePrice by remember { mutableStateOf("") }
    var newColorSaving    by remember { mutableStateOf(false) }
    var newColorError     by remember { mutableStateOf<String?>(null) }

    fun loadSiblingColors() {
        scope.launch {
            siblingColorsLoading = true
            siblingColorsError   = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/products/${activeProductId}/siblings")
                    val root = ApiClient.parseJsonObject(text)
                    (root?.get("data") as? JsonArray)
                        ?.filterIsInstance<JsonObject>()
                        ?.mapNotNull { o ->
                            val id = o.stringField("id").ifEmpty { return@mapNotNull null }
                            ProductColorSiblingUi(
                                id        = id,
                                color     = o.stringField("color"),
                                colorCode = o.stringField("colorCode"),
                                barcode   = o.stringField("barcode"),
                                imageUrl  = o.stringField("imageUrl"),
                                active    = (o["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                                isCurrent = id == activeProductId,
                            )
                        } ?: emptyList()
                }
            }.onSuccess { siblingColors = it }
             .onFailure { siblingColorsError = it.message }
            siblingColorsLoading = false
        }
    }

    // ── Load full product + suggested tags ─────────────────────────────────────
    LaunchedEffect(activeProductId) {
        // Birinchi ochilishda butun oyna spinner ko'rsatadi; siblingga o'tishda esa
        // faqat item ichida kichik spinner — dialog qayta yuklanmaydi (blink yo'q).
        if (initialLoad) loadingProduct = true else switchingVariant = true
        loadError = null
        expandedColorId = activeProductId
        // Siblingga o'tilganda oldingi mahsulotdan qolgan transient holatlarni tozalaymiz.
        deletedUrls     = emptySet()
        selectedFiles   = emptyList()
        previewBitmaps  = emptyMap()
        existingImageUrls = emptyList()
        existingBitmaps = emptyMap()
        showColorPicker = false
        showAddColorForm = false
        newColorName = ""; newColorCode = ""; newColorBarcode = ""; newColorSalePrice = ""; newColorError = null
        saveError       = null
        existingPriceId = null
        tier1Price      = ""
        salePriceRow    = ""
        discountPercent      = ""
        discountStart        = ""
        discountEnd          = ""
        discountActiveFlag   = false
        discountCurrentPrice = ""
        discountLoaded       = false
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/products/$activeProductId")
                val root = ApiClient.parseJsonObject(text)
                root?.let { ApiClient.dataObject(it) } as? JsonObject
            }
        }.onSuccess { data ->
            if (data != null) {
                // Sibling almashganda nom/barcode/rang serverdan yangilanadi.
                name         = data.stringField("name").ifBlank { name }
                barcode      = data.stringField("barcode")
                minStock     = (data["minStock"]     as? JsonPrimitive)?.content ?: "0"
                leadTimeDays = (data["leadTimeDays"] as? JsonPrimitive)?.content ?: "0"
                weight       = (data["weight"]       as? JsonPrimitive)?.content?.let {
                    if (it == "0.0" || it == "0") "" else it
                } ?: ""
                dimL         = (data["length"]       as? JsonPrimitive)?.content?.let {
                    if (it == "0.0" || it == "0") "" else it
                } ?: ""
                dimW         = (data["width"]        as? JsonPrimitive)?.content?.let {
                    if (it == "0.0" || it == "0") "" else it
                } ?: ""
                dimH         = (data["height"]       as? JsonPrimitive)?.content?.let {
                    if (it == "0.0" || it == "0") "" else it
                } ?: ""
                packageType  = data.stringField("packageType")
                fragile      = (data["fragile"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                active       = (data["active"]  as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
                featured     = (data["featured"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                digital      = (data["digital"]  as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                status       = data.stringField("status").ifBlank { "DRAFT" }
                description  = data.stringField("description")
                shortDescription = data.stringField("shortDescription")
                sku             = data.stringField("sku")
                slug            = data.stringField("slug")
                metaDescription = data.stringField("metaDescription")
                material        = data.stringField("material")
                countryOfOrigin = data.stringField("countryOfOrigin")
                warrantyMonths  = (data["warrantyMonths"] as? JsonPrimitive)?.content?.let {
                    if (it == "0") "" else it
                } ?: ""
                colors = (data["colors"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content?.ifEmpty { null } }
                    ?: emptyList()
                colorValue       = data.stringField("color")
                colorCode        = data.stringField("colorCode")
                manufacturerName = data.stringField("manufacturerName")

                // Tarjimalarni yuklash
                val parseTranslations = { key: String ->
                    (data[key] as? JsonObject)
                        ?.let { obj ->
                            val uz = (obj["uz"] as? JsonPrimitive)?.content?.ifEmpty { null }
                            val ru = (obj["ru"] as? JsonPrimitive)?.content?.ifEmpty { null }
                            Pair(uz ?: "", ru ?: "")
                        } ?: Pair("", "")
                }
                parseTranslations("nameTranslations").let { (uz, ru) -> nameUz = uz; nameRu = ru }
                parseTranslations("descriptionTranslations").let { (uz, ru) -> descUz = uz; descRu = ru }
                parseTranslations("shortDescriptionTranslations").let { (uz, ru) -> shortDescUz = uz; shortDescRu = ru }
                parseTranslations("materialTranslations").let { (uz, ru) -> materialUz = uz; materialRu = ru }
                parseTranslations("countryOfOriginTranslations").let { (uz, ru) -> countryUz = uz; countryRu = ru }
                parseTranslations("manufacturerNameTranslations").let { (uz, ru) -> manufacturerUz = uz; manufacturerRu = ru }

                val catId = data.stringField("categoryId")
                val bId   = data.stringField("brandId")
                if (catId.isNotBlank()) {
                    selCatId   = catId
                    selCatName = data.stringField("categoryName").ifBlank { selCatName }
                }
                if (bId.isNotBlank()) {
                    selBrandId   = bId
                    selBrandName = data.stringField("brandName").ifBlank { selBrandName }
                }

                tags = (data["tags"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content?.ifEmpty { null } }
                    ?: product.tags

                // Sotuv narxi — eng so'nggi yozuvni olish (backend effectiveDate DESC tartibida qaytaradi)
                // Backend ProductPriceResponse: { id, productId, salePrice, effectiveDate, endDate,
                //   discountPercent, discountStartDate, discountEndDate, discountActive, currentPrice }
                // priceType field yo'q — filter qilinmaydi
                runCatching {
                    val pricesText = ApiClient.get("/api/products/${activeProductId}/prices")
                    val pricesRoot = ApiClient.parseJsonObject(pricesText)
                    val pricesArr  = pricesRoot?.get("data") as? JsonArray
                    pricesArr
                        ?.filterIsInstance<JsonObject>()
                        ?.firstOrNull()                           // eng so'nggi (effectiveDate DESC)
                        ?.let { obj ->
                            val sp = (obj["salePrice"] as? JsonPrimitive)?.content
                            val priceId = (obj["id"] as? JsonPrimitive)?.content
                            salePriceRow = sp ?: ""
                            val value = if (sp != null && sp != "0" && sp != "0.0") sp else ""
                            // Chegirma ma'lumotlari (backend yangi maydonlari)
                            discountActiveFlag   = (obj["discountActive"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                            discountCurrentPrice = (obj["currentPrice"] as? JsonPrimitive)?.content?.ifEmpty { null } ?: ""
                            val dp = (obj["discountPercent"] as? JsonPrimitive)?.content
                            val ds = (obj["discountStartDate"] as? JsonPrimitive)?.content
                            val de = (obj["discountEndDate"] as? JsonPrimitive)?.content
                            if (dp != null && dp != "0") discountPercent = dp
                            if (ds != null && ds != "null") discountStart = ds.take(10)
                            if (de != null && de != "null") discountEnd = de.take(10)
                            discountLoaded = true
                            if (value.isNotEmpty()) Pair(value, priceId) else null
                        }
                }.getOrNull()?.let { (value, priceId) ->
                    salePriceRow = value
                    existingPriceId = priceId
                }
                // 1-liniya narxi — chegirma qaysi narx ustidan qo'llanishi (qoida: tier1 bazasi)
                runCatching {
                    val tiersText = ApiClient.get("/api/products/${activeProductId}/price-tiers")
                    val tiersRoot = ApiClient.parseJsonObject(tiersText)
                    tier1Price = (tiersRoot?.get("data") as? JsonArray)
                        ?.firstOrNull()?.let { it as? JsonObject }
                        ?.get("price")?.let { it as? JsonPrimitive }?.content ?: ""
                }
            }
        }.onFailure { loadError = it.message }

        // Mavjud rasmlarni yuklash
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/products/${activeProductId}/images")
                val root = ApiClient.parseJsonObject(text)
                (root?.get("data") as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content?.ifBlank { null } }
                    ?: emptyList()
            }
        }.onSuccess { urls ->
            existingImageUrls = urls
            // Bitmap larni parallel yuklash
            withContext(Dispatchers.IO) {
                val bitmaps = urls.associateWith { url ->
                    runCatching {
                        val fullUrl = ApiClient.fullPath(url)
                        val conn = java.net.URL(fullUrl).openConnection() as java.net.HttpURLConnection
                        val token = mexa.club.desktop_app.market.session.SessionManager.token.takeIf { it.isNotEmpty() }
                            ?: mexa.club.desktop_app.auth.AuthSession.accessToken
                        if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connectTimeout = 5_000; conn.readTimeout = 10_000; conn.connect()
                        val bytes = conn.inputStream.use { it.readBytes() }; conn.disconnect()
                        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    }.getOrNull()
                }
                existingBitmaps = bitmaps
            }
        }

        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get("/api/products/tags")
                val root = ApiClient.parseJsonObject(text)
                (root?.get("data") as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    ?: emptyList()
            }
        }.onSuccess { suggestedTags = it }

        loadingProduct = false
        switchingVariant = false
        initialLoad = false
        loadSiblingColors()
    }


    /** Joriy tagInput ni teg sifatida qo'shadi */
    fun addTag() {
        val t = tagInput.trim().lowercase()
        if (t.length >= 1 && t !in tags) tags = tags + t
        tagInput = ""
    }

    /** Vergul bosqichida so'nggi yozilgan qismni teg sifatida qo'shib, inputni tozalaydi */
    fun commitOnComma() {
        val t = tagInput.trimEnd(',').trim().lowercase()
        if (t.length >= 1 && t !in tags) tags = tags + t
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
                val activeExisting = existingImageUrls.count { it !in deletedUrls }
                val remaining = 10 - activeExisting - selectedFiles.size
                if (remaining <= 0) return@launch
                val newFiles = chooser.selectedFiles
                    .filter { it.length() <= 5L * 1024 * 1024 }
                    .take(remaining)
                val newBitmaps = newFiles.associate { file ->
                    file.name to runCatching {
                        SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                    }.getOrNull()
                }.filterValues { it != null }.mapValues { it.value!! }
                selectedFiles  = selectedFiles + newFiles
                previewBitmaps = previewBitmaps + newBitmaps
            }
        }
    }

    fun validate(): Boolean {
        nameError    = if (name.isBlank())    "Mahsulot nomi majburiy" else null
        barcodeError = if (barcode.isBlank()) "Barcode majburiy"       else null
        return nameError == null && barcodeError == null
    }

    fun save() {
        if (!validate()) return
        scope.launch {
            saving    = true
            saveError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val tagsJson = tags.joinToString(",") { "\"$it\"" }
                    val body = buildString {
                        append("{")
                        append("\"name\":\"${name.trim().replace("\"", "\\\"")}\",")
                        append("\"barcode\":\"${barcode.trim().replace("\"", "\\\"")}\",")
                        if (colorValue.isNotBlank()) append("\"color\":\"${colorValue.trim().replace("\"", "\\\"")}\",")
                        if (colorCode.isNotBlank()) append("\"colorCode\":\"${colorCode.trim().replace("\"", "\\\"")}\",")
                        if (manufacturerName.isNotBlank()) append("\"manufacturerName\":\"${manufacturerName.trim().replace("\"", "\\\"")}\",")
                        if (selCatId.isNotBlank()) append("\"categoryId\":\"$selCatId\",")
                        if (selBrandId.isNotBlank()) append("\"brandId\":\"$selBrandId\",")
                        append("\"unit\":\"${unit.replace("\"", "\\\"")}\",")
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
                        if (description.isNotBlank()) append("\"description\":\"${description.trim().replace("\\", "\\\\").replace("\"", "\\\"")}\",")
                        if (shortDescription.isNotBlank()) append("\"shortDescription\":\"${shortDescription.trim().replace("\"", "\\\"")}\",")
                        if (sku.isNotBlank()) append("\"sku\":\"${sku.trim().replace("\"", "\\\"")}\",")
                        if (slug.isNotBlank()) append("\"slug\":\"${slug.trim().lowercase().replace("\"", "\\\"")}\",")
                        if (metaDescription.isNotBlank()) append("\"metaDescription\":\"${metaDescription.trim().replace("\"", "\\\"")}\",")
                        if (material.isNotBlank()) append("\"material\":\"${material.trim().replace("\"", "\\\"")}\",")
                        if (countryOfOrigin.isNotBlank()) append("\"countryOfOrigin\":\"${countryOfOrigin.trim().replace("\"", "\\\"")}\",")
                        val wm = warrantyMonths.toIntOrNull() ?: 0
                        if (wm > 0) append("\"warrantyMonths\":$wm,")
                        // Tarjimalar
                        append("\"nameTranslations\":{\"uz\":\"${nameUz.trim().replace("\"", "\\\"")}\",\"ru\":\"${nameRu.trim().replace("\"", "\\\"")}\"},")
                        append("\"descriptionTranslations\":{\"uz\":\"${descUz.trim().replace("\\", "\\\\").replace("\"", "\\\"")}\",\"ru\":\"${descRu.trim().replace("\\", "\\\\").replace("\"", "\\\"")}\"},")
                        append("\"shortDescriptionTranslations\":{\"uz\":\"${shortDescUz.trim().replace("\"", "\\\"")}\",\"ru\":\"${shortDescRu.trim().replace("\"", "\\\"")}\"},")
                        append("\"materialTranslations\":{\"uz\":\"${materialUz.trim().replace("\"", "\\\"")}\",\"ru\":\"${materialRu.trim().replace("\"", "\\\"")}\"},")
                        append("\"countryOfOriginTranslations\":{\"uz\":\"${countryUz.trim().replace("\"", "\\\"")}\",\"ru\":\"${countryRu.trim().replace("\"", "\\\"")}\"},")
                        append("\"manufacturerNameTranslations\":{\"uz\":\"${manufacturerUz.trim().replace("\"", "\\\"")}\",\"ru\":\"${manufacturerRu.trim().replace("\"", "\\\"")}\"},")
                        val colorsJson = colors.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
                        append("\"colors\":[$colorsJson],")
                        append("\"tags\":[$tagsJson]")
                        append("}")
                    }

                    if (selectedFiles.isEmpty()) {
                        ApiClient.put("/api/products/${activeProductId}", body)
                    } else {
                        val filePairs = selectedFiles.map { f -> f.name to f.readBytes() }
                        ApiClient.putMultipart("/api/products/${activeProductId}", body, filePairs)
                    }

                    // Asosiy PUT muvaffaqiyatli bo'ldi — qo'shimcha qadamlar xatolarini yig'amiz.
                    val warnings = mutableListOf<String>()

                    // O'chirilgan rasmlarni serverdan o'chirish
                    for (url in deletedUrls) {
                        val filename = url.substringAfterLast("/")
                        runCatching { ApiClient.delete("/api/products/${activeProductId}/images/$filename") }
                            .onFailure { e -> warnings += "Rasm o'chirilmadi ($filename): ${e.message ?: "noma'lum xato"}" }
                    }

                    // Rasmlar tartibini saqlash
                    val activeOrdered = existingImageUrls.filter { it !in deletedUrls }
                    if (activeOrdered.isNotEmpty()) {
                        val urlsJson = activeOrdered.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
                        runCatching {
                            ApiClient.put("/api/products/${activeProductId}/images/reorder", "{\"urls\":[$urlsJson]}")
                        }.onFailure { e -> warnings += "Rasmlar tartibi saqlanmadi: ${e.message ?: "noma'lum xato"}" }
                    }

                    // Sotuv narxini yangilash: mavjud yozuv bo'lsa PUT, bo'lmasa POST
                    val salePriceBD = salePriceRow.trim().toBigDecimalOrNull()
                    if (salePriceBD != null && salePriceBD > java.math.BigDecimal.ZERO) {
                        val priceBody = buildString {
                            append("{\"salePrice\":$salePriceBD")
                            // Chegirma bloki: foiz bo'sh qoldirilsa backend mavjud chegirmani saqlaydi (qisman update),
                            // 0 yozilsa chegirma o'chiriladi. Chegirma faol bo'lsa liniyalar ishlamaydi (1-liniya narxiga qo'llanadi).
                            if (discountPercent.isNotBlank()) {
                                val dp = discountPercent.trim().toIntOrNull()
                                if (dp != null && dp in 0..100) {
                                    append(",\"discountPercent\":$dp")
                                    if (dp > 0) {
                                        if (discountStart.trim().isNotBlank()) append(",\"discountStartDate\":\"${discountStart.trim()}T00:00:00\"")
                                        if (discountEnd.trim().isNotBlank()) append(",\"discountEndDate\":\"${discountEnd.trim()}T23:59:59\"")
                                    }
                                }
                            }
                            append("}")
                        }
                        val pid = existingPriceId
                        if (pid != null) {
                            // Mavjud narxni yangilash
                            runCatching {
                                ApiClient.put("/api/products/${activeProductId}/prices/$pid", priceBody)
                            }.onFailure { e -> warnings += "Sotuv narxi yangilanmadi: ${e.message ?: "noma'lum xato"}" }
                        } else {
                            // Yangi narx yozuvi yaratish
                            runCatching {
                                ApiClient.post("/api/products/${activeProductId}/prices", priceBody)
                            }.onFailure { e -> warnings += "Sotuv narxi saqlanmadi: ${e.message ?: "noma'lum xato"}" }
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

    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 420.dp, max = 780.dp).fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.heightIn(max = 780.dp)) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            "Mahsulotni tahrirlash",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        Text(
                            "Mahsulot ma'lumotlarini o'zgartiring",
                            fontSize = 13.sp,
                            color = MexaWarehouseColors.textMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    IconButton(
                        onClick = { if (!saving && !loadingProduct) onDismiss() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                if (loadingProduct) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.primary)
                    }
                } else if (loadError != null) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(loadError!!, color = MexaWarehouseColors.danger, fontSize = 14.sp)
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary)) {
                                Text("Qayta urinish")
                            }
                        }
                    }
                } else {

                    // Tabs
                    val tabLabels = listOf("Asosiy", "Narx", "Qo'shimcha", "Rasmlar")
                    androidx.compose.material3.TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor   = MexaWarehouseColors.surfaceLowest,
                        contentColor     = MexaWarehouseColors.primary,
                    ) {
                        tabLabels.forEachIndexed { idx, label ->
                            androidx.compose.material3.Tab(
                                selected = selectedTab == idx,
                                onClick  = { selectedTab = idx },
                                text     = {
                                    Text(
                                        label,
                                        fontSize   = 13.sp,
                                        fontWeight = if (selectedTab == idx) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                            )
                        }
                    }

                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        (0..3).forEach { tabIdx ->
                            Box(
                                modifier = if (tabIdx == selectedTab) Modifier.fillMaxSize()
                                           else Modifier.size(0.dp)
                            ) {
                                when (tabIdx) {

                                    0 -> Column(
                                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        // Mahsulot nomi
                                        EditFieldBlock("Mahsulot nomi *", nameError) {
                                            OutlinedTextField(value = name, onValueChange = { name = it; nameError = null }, placeholder = { Text("Masalan: Nike Air Max 270", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, isError = nameError != null, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                        }

                                        // O'lchov birligi
                                        EditFieldBlock("O'lchov birligi *", null) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("dona", "kg", "litr", "m", "m²", "l", "box").forEach { u ->
                                                    val selected = unit == u
                                                    Surface(onClick = { if (!saving) unit = u }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp), color = if (selected) MexaWarehouseColors.primary.copy(alpha = 0.08f) else MexaWarehouseColors.surfaceLowest, border = BorderStroke(1.dp, if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.outlineVariant)) {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(u, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.textMuted) }
                                                    }
                                                }
                                            }
                                        }

                                        // Kategoriya + Brend (qidiruv bilan)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Column(Modifier.weight(1f)) {
                                                Text("Kategoriya", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, modifier = Modifier.padding(bottom = 5.dp))
                                                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { if (!saving) catExpanded = it; if (!it) catSearch = "" }) {
                                                    OutlinedTextField(value = if (catExpanded) catSearch else selCatName, onValueChange = { catSearch = it }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 13.sp), singleLine = true)
                                                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false; catSearch = "" }) {
                                                        DropdownMenuItem(text = { Text("Kategoriyani tanlang", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, onClick = { selCatId = ""; selCatName = "Tanlang"; catExpanded = false; catSearch = "" })
                                                        val filteredCats = categories.filter { catSearch.isBlank() || it.name.contains(catSearch, ignoreCase = true) }
                                                        if (filteredCats.isEmpty()) { DropdownMenuItem(text = { Text("Topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, onClick = {}, enabled = false) }
                                                        else filteredCats.forEach { cat -> DropdownMenuItem(text = { Text(cat.name, fontSize = 13.sp) }, onClick = { selCatId = cat.id; selCatName = cat.name; catExpanded = false; catSearch = "" }) }
                                                    }
                                                }
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text("Brend", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, modifier = Modifier.padding(bottom = 5.dp))
                                                ExposedDropdownMenuBox(expanded = brandExpanded, onExpandedChange = { if (!saving) brandExpanded = it; if (!it) brandSearch = "" }) {
                                                    OutlinedTextField(value = if (brandExpanded) brandSearch else selBrandName, onValueChange = { brandSearch = it }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 13.sp), singleLine = true)
                                                    ExposedDropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false; brandSearch = "" }) {
                                                        DropdownMenuItem(text = { Text("Brendni tanlang", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, onClick = { selBrandId = ""; selBrandName = "Tanlang"; brandExpanded = false; brandSearch = "" })
                                                        val filteredBrands = brands.filter { brandSearch.isBlank() || it.name.contains(brandSearch, ignoreCase = true) }
                                                        if (filteredBrands.isEmpty()) { DropdownMenuItem(text = { Text("Topilmadi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, onClick = {}, enabled = false) }
                                                        else filteredBrands.forEach { b -> DropdownMenuItem(text = { Text(b.name, fontSize = 13.sp) }, onClick = { selBrandId = b.id; selBrandName = b.name; brandExpanded = false; brandSearch = "" }) }
                                                    }
                                                }
                                            }
                                        }

                                        // Ishlab chiqaruvchi
                                        Column(Modifier.fillMaxWidth()) {
                                            Text("Ishlab chiqaruvchi", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, modifier = Modifier.padding(bottom = 5.dp))
                                            OutlinedTextField(value = manufacturerName, onValueChange = { manufacturerName = it }, placeholder = { Text("Masalan: Samsung, Apple...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 13.sp))
                                        }

                                        // Nashr holati + Faol
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Column(Modifier.weight(1f)) {
                                                Text("Nashr holati", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, modifier = Modifier.padding(bottom = 5.dp))
                                                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                                                    OutlinedTextField(value = statusOptions.firstOrNull { it.first == status }?.second ?: status, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 13.sp), singleLine = true)
                                                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                                        statusOptions.forEach { (value, label) -> DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { status = value; statusExpanded = false }) }
                                                    }
                                                }
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Spacer(Modifier.height(23.dp))
                                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MexaWarehouseColors.backgroundPage, border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle)) {
                                                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Column {
                                                            Text("Faol", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                                            Text("Ro'yxatda ko'rinadi", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 1.dp))
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            if (activeUpdating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MexaWarehouseColors.primary)
                                                            Switch(checked = active, onCheckedChange = { newValue ->
                                                                if (!saving && !activeUpdating) {
                                                                    activeError = null; active = newValue
                                                                    scope.launch {
                                                                        activeUpdating = true
                                                                        val err: String? = withContext(Dispatchers.IO) { runCatching { ApiClient.patch("/api/products/${activeProductId}/active", "{\"active\":$newValue}") }.exceptionOrNull()?.message }
                                                                        if (err != null) { active = !newValue; activeError = err } else activeError = null
                                                                        activeUpdating = false
                                                                    }
                                                                }
                                                            }, enabled = !saving && !activeUpdating, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MexaWarehouseColors.primary, uncheckedThumbColor = Color.White, uncheckedBorderColor = MexaWarehouseColors.outlineVariant, uncheckedTrackColor = MexaWarehouseColors.outlineVariant))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (activeError != null) Text(activeError!!, fontSize = 11.sp, color = MexaWarehouseColors.danger)

                                        // Tavsiya etilgan + Raqamli
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MexaWarehouseColors.backgroundPage, border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle)) {
                                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Column { Text("Tavsiya etilgan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary); Text("Bosh sahifada ko'rsatish", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 1.dp)) }
                                                    Switch(checked = featured, onCheckedChange = { featured = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MexaWarehouseColors.primary, uncheckedThumbColor = Color.White, uncheckedBorderColor = MexaWarehouseColors.outlineVariant, uncheckedTrackColor = MexaWarehouseColors.outlineVariant))
                                                }
                                            }
                                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MexaWarehouseColors.backgroundPage, border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle)) {
                                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Column { Text("Raqamli mahsulot", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary); Text("Jismoniy yetkazib berish yo'q", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 1.dp)) }
                                                    Switch(checked = digital, onCheckedChange = { digital = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MexaWarehouseColors.primary, uncheckedThumbColor = Color.White, uncheckedBorderColor = MexaWarehouseColors.outlineVariant, uncheckedTrackColor = MexaWarehouseColors.outlineVariant))
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            SectionLabel("RANGLAR VA BARCODE")
                                            if (siblingColors.size > 1) Text("${siblingColors.size} ta rang", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                        }
                                        Text("Rang ustiga bosing — barcode va rang shu yerda ochiladi.", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                        // Rang palitrasi (accordion item ichida ishlatiladi)
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
                                        when {
                                            siblingColorsLoading -> Box(Modifier.fillMaxWidth().height(60.dp), Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MexaWarehouseColors.primary) }
                                            siblingColorsError != null -> Text(siblingColorsError!!, fontSize = 12.sp, color = MexaWarehouseColors.danger)
                                            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                siblingColors.forEach { sib ->
                                                    val isExpanded = sib.id == expandedColorId
                                                    // Header rangi/nomi/barcodi — joriy item uchun jonli tahrir holatidan, aks holda serverdagi qiymatdan.
                                                    val headerColorCode = if (sib.isCurrent) colorCode else sib.colorCode
                                                    val headerName    = if (sib.isCurrent) colorValue else sib.color
                                                    val headerBarcode = if (sib.isCurrent) barcode else sib.barcode
                                                    val headerBox = headerColorCode.takeIf { it.isNotBlank() }?.let { hex -> runCatching { val c = hex.removePrefix("#"); val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) }.getOrNull() }
                                                    Column(
                                                        Modifier.fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(MexaWarehouseColors.surfaceLowest)
                                                            .border(1.dp, if (isExpanded) MexaWarehouseColors.primary else MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp)),
                                                    ) {
                                                        // ── Header (bosiladigan) ──
                                                        Row(
                                                            Modifier.fillMaxWidth()
                                                                .clickable(enabled = !saving && !switchingVariant) {
                                                                    if (sib.isCurrent) expandedColorId = if (isExpanded) "" else sib.id
                                                                    else activeProductId = sib.id
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        ) {
                                                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(headerBox ?: MexaWarehouseColors.tableHeaderBg).border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                                                if (headerBox == null) Text(headerName.take(1).uppercase().ifBlank { "?" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                                                            }
                                                            Column(Modifier.weight(1f)) {
                                                                Text(headerName.ifBlank { "(rangsiz)" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                                                Text(headerBarcode.ifBlank { "Barcode yo'q" }, fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                            }
                                                            when {
                                                                sib.isCurrent -> StatusBadge("Joriy",  MexaWarehouseColors.blueBadgeBg,     MexaWarehouseColors.blueBadgeFg)
                                                                sib.active    -> StatusBadge("Faol",   MexaWarehouseColors.statusActiveBg,  MexaWarehouseColors.statusActiveFg)
                                                                else          -> StatusBadge("Nofaol", MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg)
                                                            }
                                                            Text(if (isExpanded) "▲" else "▼", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                        }
                                                        // ── Editor (item ichida, inline) ──
                                                        if (isExpanded) {
                                                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                                            if (switchingVariant) {
                                                                Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MexaWarehouseColors.primary) }
                                                            } else if (sib.isCurrent) {
                                                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                                    // Barcode + Rang nomi
                                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                                        EditFieldBlock("Barcode *", barcodeError, Modifier.weight(1f)) {
                                                                            OutlinedTextField(value = barcode, onValueChange = { barcode = it; barcodeError = null }, placeholder = { Text("8690000123456", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, isError = barcodeError != null, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                                                        }
                                                                        EditFieldBlock("Rang nomi", null, Modifier.weight(1f)) {
                                                                            OutlinedTextField(value = colorValue, onValueChange = { colorValue = it }, placeholder = { Text("Qora, Oq, Ko'k...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                                                        }
                                                                    }
                                                                    // Rang xulosasi + palitrani ochish/yopish
                                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                                        val curColor = colorCode.takeIf { it.isNotBlank() }?.let { hex -> runCatching { val c = hex.removePrefix("#"); val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) }.getOrNull() }
                                                                        Box(Modifier.size(20.dp).clip(CircleShape).background(curColor ?: Color(0xFFEEEEEE)).border(1.dp, MexaWarehouseColors.outlineVariant, CircleShape))
                                                                        Text(colorValue.ifBlank { "Rang tanlanmagan" }, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                                                                        if (colorCode.isNotBlank()) Text(colorCode.uppercase(), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                                                                        Spacer(Modifier.weight(1f))
                                                                        OutlinedButton(onClick = { showColorPicker = !showColorPicker }, enabled = !saving, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant), colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.primary), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                                                            Text(if (showColorPicker) "Yopish" else "Rangni o'zgartirish", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                                        }
                                                                    }
                                                                    // Color picker (yig'iladigan)
                                                                    if (showColorPicker) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                            presetColors.forEach { (hex, colorLabel) ->
                                                                                val bgColor = runCatching { val c = hex.removePrefix("#"); val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) }.getOrElse { Color.Gray }
                                                                                val isSelected = colorCode.uppercase() == hex.uppercase()
                                                                                Box(Modifier.size(26.dp).clip(CircleShape).background(bgColor).border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) MexaWarehouseColors.primary else Color(0xFFCCCCCC), CircleShape).clickable { colorCode = hex.uppercase(); if (colorValue.isBlank()) colorValue = colorLabel }, contentAlignment = Alignment.Center) {
                                                                                    if (isSelected) { val lum = 0.299f * bgColor.red + 0.587f * bgColor.green + 0.114f * bgColor.blue; Icon(Icons.Filled.Close, contentDescription = null, tint = if (lum > 0.5f) Color(0xFF1A1A1A) else Color.White, modifier = Modifier.size(11.dp)) }
                                                                                }
                                                                            }
                                                                        }
                                                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                                            val hexPreview = runCatching { val c = colorCode.removePrefix("#"); if (c.length == 6) { val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) } else null }.getOrNull()
                                                                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(hexPreview ?: Color(0xFFEEEEEE)).border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(6.dp)))
                                                                            OutlinedTextField(value = colorCode, onValueChange = { colorCode = it.uppercase().take(7) }, placeholder = { Text("HEX: #FF0000", fontSize = 12.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !saving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 13.sp))
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (!showAddColorForm) {
                                            OutlinedButton(onClick = { showAddColorForm = true; newColorError = null }, enabled = !saving, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MexaWarehouseColors.primary), colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.primary), modifier = Modifier.fillMaxWidth()) {
                                                Text("+ Rang qo'shish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            }
                                        } else {
                                            // Yangi rang karta — Create dialog bilan bir xil ko'rinish
                                            Column(
                                                Modifier.fillMaxWidth().background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(10.dp)).border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp)).padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                // Sarlavha + yopish
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        val prevColor = newColorCode.takeIf { it.isNotBlank() }?.let { hex -> runCatching { val c = hex.removePrefix("#"); val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) }.getOrNull() }
                                                        Box(Modifier.size(18.dp).clip(CircleShape).background(prevColor ?: MexaWarehouseColors.outlineVariant).border(1.dp, MexaWarehouseColors.outlineVariant, CircleShape))
                                                        Text("Rang variant ${(siblingColors.size).coerceAtLeast(1) + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
                                                    }
                                                    Box(Modifier.size(22.dp).clip(CircleShape).background(MexaWarehouseColors.danger.copy(alpha = 0.1f)).clickable { showAddColorForm = false; newColorName = ""; newColorCode = ""; newColorBarcode = ""; newColorError = null }, contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Filled.Close, contentDescription = null, tint = MexaWarehouseColors.danger, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                // Barcode + Rang nomi
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    EditFieldBlock("Barcode *", null, Modifier.weight(1f)) {
                                                        OutlinedTextField(value = newColorBarcode, onValueChange = { newColorBarcode = it; newColorError = null }, placeholder = { Text("8690000123456", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !newColorSaving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                                    }
                                                    EditFieldBlock("Rang nomi", null, Modifier.weight(1f)) {
                                                        OutlinedTextField(value = newColorName, onValueChange = { newColorName = it }, placeholder = { Text("Qora, Oq, Ko'k...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !newColorSaving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                                    }
                                                }
                                                // Color picker — 29 rang
                                                val newPresetColors = listOf(
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
                                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        newPresetColors.forEach { (hex, colorLabel) ->
                                                            val bgColor = runCatching { val c = hex.removePrefix("#"); val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) }.getOrElse { Color.Gray }
                                                            val isSelected = newColorCode.uppercase() == hex.uppercase()
                                                            Box(Modifier.size(26.dp).clip(CircleShape).background(bgColor).border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) MexaWarehouseColors.primary else Color(0xFFCCCCCC), CircleShape).clickable { newColorCode = hex.uppercase(); if (newColorName.isBlank()) newColorName = colorLabel }, contentAlignment = Alignment.Center) {
                                                                if (isSelected) { val lum = 0.299f * bgColor.red + 0.587f * bgColor.green + 0.114f * bgColor.blue; Icon(Icons.Filled.Close, contentDescription = null, tint = if (lum > 0.5f) Color(0xFF1A1A1A) else Color.White, modifier = Modifier.size(11.dp)) }
                                                            }
                                                        }
                                                    }
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        val hexPreview = runCatching { val c = newColorCode.removePrefix("#"); if (c.length == 6) { val v = c.toLong(16); Color(red = ((v shr 16) and 0xFF).toInt() / 255f, green = ((v shr 8) and 0xFF).toInt() / 255f, blue = (v and 0xFF).toInt() / 255f) } else null }.getOrNull()
                                                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(hexPreview ?: Color(0xFFEEEEEE)).border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(6.dp)))
                                                        OutlinedTextField(value = newColorCode, onValueChange = { newColorCode = it.uppercase().take(7) }, placeholder = { Text("HEX: #FF0000", fontSize = 12.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !newColorSaving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 13.sp))
                                                    }
                                                }
                                                if (newColorError != null) Text(newColorError!!, fontSize = 12.sp, color = MexaWarehouseColors.danger)
                                                Button(onClick = {
                                                            if (newColorBarcode.isBlank()) { newColorError = "Barcode majburiy"; return@Button }
                                                            scope.launch {
                                                                newColorSaving = true; newColorError = null
                                                                runCatching {
                                                                    withContext(Dispatchers.IO) {
                                                                        val body = buildString {
                                                                            append("{")
                                                                            append("\"name\":\"${name.trim().replace("\"", "\\\"")}\",")
                                                                            append("\"barcode\":\"${newColorBarcode.trim().replace("\"", "\\\"")}\",")
                                                                            append("\"color\":\"${newColorName.trim().replace("\"", "\\\"")}\",")
                                                                            if (newColorCode.isNotBlank()) append("\"colorCode\":\"${newColorCode.trim().replace("\"", "\\\"")}\",")
                                                                            if (selCatId.isNotBlank()) append("\"categoryId\":\"$selCatId\",")
                                                                            if (selBrandId.isNotBlank()) append("\"brandId\":\"$selBrandId\",")
                                                                            append("\"unit\":\"${unit.replace("\"", "\\\"")}\",")
                                                                            append("\"active\":true,")
                                                                            append("\"status\":\"$status\"")
                                                                            append("}")
                                                                        }
                                                                        val responseText = ApiClient.post("/api/products/${activeProductId}/colors", body)
                                                                        val newSiblingId = ApiClient.parseJsonObject(responseText)?.let { it["data"] as? JsonObject }?.let { (it["id"] as? JsonPrimitive)?.content }
                                                                        if (!newSiblingId.isNullOrBlank()) {
                                                                            val saleBD = newColorSalePrice.trim().toBigDecimalOrNull()
                                                                            if (saleBD != null) {
                                                                                runCatching {
                                                                                    ApiClient.post("/api/products/$newSiblingId/prices", "{\"salePrice\":$saleBD}")
                                                                                }.onFailure { e ->
                                                                                    siblingColorsError = "Rang yaratildi, lekin narx saqlanmadi: ${e.message ?: "noma'lum xato"}"
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }.onSuccess { showAddColorForm = false; newColorName = ""; newColorCode = ""; newColorBarcode = ""; newColorSalePrice = ""; loadSiblingColors() }
                                                                 .onFailure { newColorError = it.message ?: "Saqlashda xatolik yuz berdi" }
                                                                newColorSaving = false
                                                            }
                                                        }, enabled = !newColorSaving, colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary), shape = RoundedCornerShape(8.dp)) {
                                                            if (newColorSaving) { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
                                                            Text("Qo'shish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                        }
                                                    }
                                                }
                                    } // Tab 0

                                    1 -> Column(
                                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(20.dp),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("CHEGIRMA (VAQT ORALIG'I BILAN)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                                                if (discountLoaded && discountActiveFlag) {
                                                    Row(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF0F0)).border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                        Text("FAOL:", fontSize = 11.sp, color = Color(0xFFDC2626))
                                                        Text("−$discountPercent%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB91C1C))
                                                        if (discountCurrentPrice.isNotBlank()) Text("· joriy narx: $discountCurrentPrice so'm", fontSize = 11.sp, color = Color(0xFFB91C1C))
                                                    }
                                                } else if (discountLoaded && discountPercent.isNotBlank() && discountPercent.trim().toIntOrNull() != 0) {
                                                    Row(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF7ED)).border(1.dp, Color(0xFFFBD38D), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                        Text("Chegirma muddati o'tgan yoki hali boshlanmagan — narx avvalgi holatda", fontSize = 11.sp, color = Color(0xFFB45309))
                                                    }
                                                }
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                                                EditFieldBlock("Chegirma foizi (%)", null, Modifier.weight(1f)) {
                                                    OutlinedTextField(value = discountPercent, onValueChange = { discountPercent = it.filter { c -> c.isDigit() } }, placeholder = { Text("20", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                                }
                                                EditFieldBlock("Boshlanish sanasi", null, Modifier.weight(1f)) {
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth().clickable(enabled = !saving) { startPickerOpen = true },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MexaWarehouseColors.surfaceLowest,
                                                        border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                                                    ) {
                                                        Row(
                                                            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                        ) {
                                                            Text(
                                                                text = discountStart.ifBlank { "Tanlash..." },
                                                                fontSize = 14.sp,
                                                                color = if (discountStart.isNotBlank()) Color.Unspecified else MexaWarehouseColors.textMuted,
                                                                modifier = Modifier.weight(1f),
                                                            )
                                                            if (discountStart.isNotBlank() && !saving) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Tozalash",
                                                                    tint = MexaWarehouseColors.textMuted,
                                                                    modifier = Modifier.size(16.dp).clickable { discountStart = "" },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                EditFieldBlock("Tugash sanasi", null, Modifier.weight(1f)) {
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth().clickable(enabled = !saving) { endPickerOpen = true },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MexaWarehouseColors.surfaceLowest,
                                                        border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                                                    ) {
                                                        Row(
                                                            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                        ) {
                                                            Text(
                                                                text = discountEnd.ifBlank { "Tanlash..." },
                                                                fontSize = 14.sp,
                                                                color = if (discountEnd.isNotBlank()) Color.Unspecified else MexaWarehouseColors.textMuted,
                                                                modifier = Modifier.weight(1f),
                                                            )
                                                            if (discountEnd.isNotBlank() && !saving) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Tozalash",
                                                                    tint = MexaWarehouseColors.textMuted,
                                                                    modifier = Modifier.size(16.dp).clickable { discountEnd = "" },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (startPickerOpen) {
                                                DiscountDatePickerDialog(
                                                    current = discountStart,
                                                    onConfirm = { discountStart = it },
                                                    onDismiss = { startPickerOpen = false },
                                                )
                                            }
                                            if (endPickerOpen) {
                                                DiscountDatePickerDialog(
                                                    current = discountEnd,
                                                    onConfirm = { discountEnd = it },
                                                    onDismiss = { endPickerOpen = false },
                                                )
                                            }
                                            val previewPct  = discountPercent.trim().toIntOrNull()
                                            // Chegirma bazasi: 1-liniya narxi, bo'lmasa flat sotuv narxi
                                            val previewBase = tier1Price.trim().toBigDecimalOrNull()
                                                ?: salePriceRow.trim().toBigDecimalOrNull()
                                            if (previewPct != null && previewPct in 1..100 && previewBase != null) {
                                                val discounted = previewBase.multiply(java.math.BigDecimal.valueOf((100 - previewPct).toLong()))
                                                    .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                                                val fmt: (java.math.BigDecimal) -> String = { bd ->
                                                    bd.toBigInteger().toString().let { s ->
                                                        buildString {
                                                            var i = 0; s.reversed().forEach { ch -> if (i > 0 && i % 3 == 0) append(' '); append(ch); i++ }
                                                        }.reversed()
                                                    }
                                                }
                                                Text("Natijada joriy narx: ${fmt(discounted)} so'm (asl: ${fmt(previewBase)} so'm)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB91C1C))
                                            }
                                            Text("Bo'sh qoldirilsa mavjud chegirma o'zgarmaydi; 0 yozilsa chegirma o'chiriladi. Tugash sanasi o'tgach narx avtomatik avvalgi holatga qaytadi.", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                        }
                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                        Text("MIQDORGA BOG'LIQ NARX BOSQICHLARI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
                                        val baseForTier = tier1Price.trim().toBigDecimalOrNull()
                                            ?: salePriceRow.trim().toBigDecimalOrNull()
                                        ProductPriceTierSection(productId = activeProductId, enabled = !saving, basePriceForDisplay = baseForTier)
                                    } // Tab 1

                                    2 -> Column(
                                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        SectionLabel("TAVSIF")
                                        EditFieldBlock("To'liq tavsif", null) {
                                            OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text("Mahsulot haqida batafsil...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 160.dp), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), maxLines = 6)
                                        }
                                        EditFieldBlock("Qisqa tavsif (max 300 belgi)", null) {
                                            OutlinedTextField(value = shortDescription, onValueChange = { if (it.length <= 300) shortDescription = it }, placeholder = { Text("Bir necha so'z bilan tavsif...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), trailingIcon = { Text("${shortDescription.length}/300", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(end = 8.dp)) })
                                        }
                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                        SectionLabel("QO'SHIMCHA MA'LUMOTLAR")
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Minimal qoldiq", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = minStock, onValueChange = { if (it.all { c -> c.isDigit() }) minStock = it }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                            }
                                            EditFieldBlock("Yetkazib berish (kun)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = leadTimeDays, onValueChange = { if (it.all { c -> c.isDigit() }) leadTimeDays = it }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                            }
                                            EditFieldBlock("Og'irligi (kg)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = weight, onValueChange = { weight = it }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                            }
                                        }
                                        EditFieldBlock("O'lchamlari (Uzunlik / Eni / Balandlik — sm)") {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(Triple(dimL, { v: String -> dimL = v }, "30"), Triple(dimW, { v: String -> dimW = v }, "20"), Triple(dimH, { v: String -> dimH = v }, "15")).forEach { (value, setter, hint) ->
                                                    OutlinedTextField(value = value, onValueChange = setter, placeholder = { Text(hint, fontSize = 13.sp, color = MexaWarehouseColors.textMuted) }, singleLine = true, enabled = !saving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                                }
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                                            EditFieldBlock("Qadoq turi", null, Modifier.weight(2f)) {
                                                OutlinedTextField(value = packageType, onValueChange = { packageType = it }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            Row(Modifier.weight(1f).padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Checkbox(checked = fragile, onCheckedChange = { if (!saving) fragile = it }, colors = CheckboxDefaults.colors(checkmarkColor = Color.White, checkedColor = MexaWarehouseColors.primary))
                                                Text("Tez sinuvchan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Materiali", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = material, onValueChange = { material = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Ishlab chiqarilgan mamlakat", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = countryOfOrigin, onValueChange = { countryOfOrigin = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Kafolat (oy)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = warrantyMonths, onValueChange = { if (it.all { c -> c.isDigit() }) warrantyMonths = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            SectionLabel("TEGLAR")
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                TagKeyHint("Enter"); Text("yoki", fontSize = 10.sp, color = MexaWarehouseColors.textMuted); TagKeyHint(","); Text("bilan qo'shing", fontSize = 10.sp, color = MexaWarehouseColors.textMuted)
                                            }
                                        }
                                        Box(Modifier.fillMaxWidth().border(1.dp, if (tagInput.isNotEmpty()) MexaWarehouseColors.primary else MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp)).background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                tags.forEach { tag ->
                                                    Surface(shape = RoundedCornerShape(4.dp), color = MexaWarehouseColors.indigoAccent.copy(alpha = 0.10f)) {
                                                        Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.indigoAccent)
                                                            Spacer(Modifier.width(4.dp))
                                                            Box(Modifier.size(16.dp).clickable { tags = tags - tag }, contentAlignment = Alignment.Center) { Text("x", fontSize = 14.sp, color = MexaWarehouseColors.indigoAccent.copy(alpha = 0.6f)) }
                                                        }
                                                    }
                                                }
                                                BasicTextField(value = tagInput, onValueChange = { raw -> if (raw.endsWith(",")) { val t = raw.dropLast(1).trim().lowercase(); if (t.isNotEmpty() && t !in tags) tags = tags + t; tagInput = "" } else tagInput = raw.lowercase() }, enabled = !saving, textStyle = TextStyle(fontSize = 13.sp, color = MexaWarehouseColors.textPrimary), modifier = Modifier.widthIn(min = 160.dp).padding(horizontal = 4.dp, vertical = 6.dp).onKeyEvent { ev -> if (ev.type == KeyEventType.KeyDown && ev.key == Key.Enter) { addTag(); true } else false }, decorationBox = { inner -> if (tagInput.isEmpty() && tags.isEmpty()) Text("Teg yozing...", fontSize = 13.sp, color = MexaWarehouseColors.textMuted); inner() })
                                            }
                                        }
                                        if (suggestedTags.isNotEmpty()) {
                                            val notSelected = suggestedTags.filter { it !in tags }.take(12)
                                            if (notSelected.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("Tavsiya etilgan teglar:", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        notSelected.forEach { tag ->
                                                            Surface(onClick = { tags = tags + tag }, shape = RoundedCornerShape(4.dp), color = MexaWarehouseColors.tableHeaderBg, border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle)) {
                                                                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                    Text("+", fontSize = 11.sp, color = MexaWarehouseColors.primary, fontWeight = FontWeight.Bold); Text(tag, fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                        SectionLabel("SEO VA KODLAR")
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("SKU (ichki kod)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = sku, onValueChange = { sku = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Slug (URL uchun)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = slug, onValueChange = { slug = it.lowercase().replace(" ", "-") }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                        EditFieldBlock("Meta tavsif (SEO, max 160 belgi)") {
                                            OutlinedTextField(value = metaDescription, onValueChange = { if (it.length <= 160) metaDescription = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), trailingIcon = { Text("${metaDescription.length}/160", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(end = 8.dp)) })
                                        }
                                        HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                        SectionLabel("TARJIMALAR (O'zbek va Rus)")
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Nomi (O'zbek)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = nameUz, onValueChange = { nameUz = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Название (Русский)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = nameRu, onValueChange = { nameRu = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                        EditFieldBlock("To'liq tavsif (O'zbek)") {
                                            OutlinedTextField(value = descUz, onValueChange = { descUz = it }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), maxLines = 4)
                                        }
                                        EditFieldBlock("Полное описание (Русский)") {
                                            OutlinedTextField(value = descRu, onValueChange = { descRu = it }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp), maxLines = 4)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Qisqa tavsif (O'zbek)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = shortDescUz, onValueChange = { shortDescUz = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Краткое описание (Русский)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = shortDescRu, onValueChange = { shortDescRu = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Material (O'zbek)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = materialUz, onValueChange = { materialUz = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Материал (Русский)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = materialRu, onValueChange = { materialRu = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Mamlakat (O'zbek)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = countryUz, onValueChange = { countryUz = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Страна (Русский)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = countryRu, onValueChange = { countryRu = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            EditFieldBlock("Ishlab chiqaruvchi (O'zbek)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = manufacturerUz, onValueChange = { manufacturerUz = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                            EditFieldBlock("Производитель (Русский)", null, Modifier.weight(1f)) {
                                                OutlinedTextField(value = manufacturerRu, onValueChange = { manufacturerRu = it }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = editFieldColors(), textStyle = TextStyle(fontSize = 14.sp))
                                            }
                                        }
                                    } // Tab 2

                                    3 -> Column(
                                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        val activeExisting = existingImageUrls.filter { it !in deletedUrls }
                                        val totalCount     = activeExisting.size + selectedFiles.size
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Mahsulot rasmlari (max 10 ta, har biri 5MB gacha)", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                                            if (totalCount > 0) Text("$totalCount ta rasm", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                        }
                                        if (totalCount > 0) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    activeExisting.forEachIndexed { idx, url ->
                                                        ExistingImageCard(url = url, bitmap = existingBitmaps[url], isFirst = idx == 0, canMoveLeft = idx > 0, canMoveRight = idx < activeExisting.size - 1, saving = saving,
                                                            onMoveLeft  = { val list = existingImageUrls.toMutableList(); val prevUrl = activeExisting.getOrNull(idx - 1); if (prevUrl != null) { val ri = list.indexOf(url); val pi = list.indexOf(prevUrl); list[ri] = prevUrl; list[pi] = url; existingImageUrls = list } },
                                                            onMoveRight = { val list = existingImageUrls.toMutableList(); val nextUrl = activeExisting.getOrNull(idx + 1); if (nextUrl != null) { val ri = list.indexOf(url); val ni = list.indexOf(nextUrl); list[ri] = nextUrl; list[ni] = url; existingImageUrls = list } },
                                                            onDelete    = { deletedUrls = deletedUrls + url })
                                                    }
                                                    selectedFiles.forEachIndexed { idx, file ->
                                                        NewImageCard(file = file, bitmap = previewBitmaps[file.name], isFirst = activeExisting.isEmpty() && idx == 0, canMoveLeft = idx > 0, canMoveRight = idx < selectedFiles.size - 1, saving = saving,
                                                            onMoveLeft  = { val list = selectedFiles.toMutableList(); list[idx] = list[idx - 1].also { list[idx - 1] = list[idx] }; selectedFiles = list },
                                                            onMoveRight = { val list = selectedFiles.toMutableList(); list[idx] = list[idx + 1].also { list[idx + 1] = list[idx] }; selectedFiles = list },
                                                            onDelete    = { selectedFiles = selectedFiles - file; previewBitmaps = previewBitmaps - file.name })
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(Modifier.background(MexaWarehouseColors.primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                                                    Text("Birinchi rasm — mahsulotning asosiy rasmi", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                }
                                            }
                                        }
                                        if (totalCount < 10) {
                                            Surface(onClick = { pickImages() }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(10.dp), color = MexaWarehouseColors.backgroundPage, border = BorderStroke(1.5.dp, MexaWarehouseColors.borderSubtle), enabled = !saving) {
                                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Icon(Icons.Filled.Image, contentDescription = null, tint = MexaWarehouseColors.primary, modifier = Modifier.size(28.dp))
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(if (totalCount == 0) "Rasm yuklash" else "Yana rasm qo'shish", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.primary)
                                                    Text("PNG, JPG, WEBP max 5 MB ${10 - totalCount} ta joy qoldi", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                    Spacer(Modifier.height(4.dp))
                                                    Text("Fayl tanlash", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.primary)
                                                }
                                            }
                                        }
                                    } // Tab 3
                                } // when(tabIdx)
                            } // Box(tabIdx)
                        } // forEach
                    } // outer Box
                }

                // Save error
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
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedTab > 0 && !loadingProduct) {
                        OutlinedButton(
                            onClick = { selectedTab-- },
                            enabled = !saving,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                        ) { Text("<- Orqaga", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    } else { Spacer(Modifier.width(1.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { if (!saving && !loadingProduct) onDismiss() },
                            enabled = !saving,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                        ) { Text("Bekor qilish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                        if (selectedTab < 3 && !loadingProduct) {
                            Button(
                                onClick = { selectedTab++ },
                                enabled = !saving,
                                colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary.copy(alpha = 0.15f), contentColor = MexaWarehouseColors.primary),
                                shape   = RoundedCornerShape(8.dp),
                            ) { Text("Keyingi ->", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        }
                        Button(
                            onClick = { save() },
                            enabled = !saving && !loadingProduct,
                            colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                            shape   = RoundedCornerShape(8.dp),
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
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted, letterSpacing = 0.5.sp)
}

@Composable
private fun TagKeyHint(key: String) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .background(
                MexaWarehouseColors.tableHeaderBg,
                RoundedCornerShape(4.dp),
            )
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            key,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MexaWarehouseColors.textPrimary,
        )
    }
}

@Composable
private fun EditFieldBlock(label: String, error: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary, modifier = Modifier.padding(bottom = 5.dp))
        content()
        if (error != null) Text(error, fontSize = 11.sp, color = MexaWarehouseColors.danger, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor    = MexaWarehouseColors.outlineVariant,
    focusedBorderColor      = MexaWarehouseColors.primary,
    unfocusedContainerColor = MexaWarehouseColors.surfaceLowest,
    focusedContainerColor   = MexaWarehouseColors.surfaceLowest,
)

// ─── Rasm kartochkasi — mavjud (serverdan) ─────────────────────────────────────

@Composable
private fun ExistingImageCard(
    url: String,
    bitmap: ImageBitmap?,
    isFirst: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    saving: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
) {
    ImageCardBase(
        bitmap       = bitmap,
        isFirst      = isFirst,
        canMoveLeft  = canMoveLeft,
        canMoveRight = canMoveRight,
        saving       = saving,
        isNew        = false,
        onMoveLeft   = onMoveLeft,
        onMoveRight  = onMoveRight,
        onDelete     = onDelete,
    )
}

// ─── Rasm kartochkasi — yangi (hali yuklanmagan) ───────────────────────────────

@Composable
private fun NewImageCard(
    file: java.io.File,
    bitmap: ImageBitmap?,
    isFirst: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    saving: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
) {
    ImageCardBase(
        bitmap       = bitmap,
        isFirst      = isFirst,
        canMoveLeft  = canMoveLeft,
        canMoveRight = canMoveRight,
        saving       = saving,
        isNew        = true,
        onMoveLeft   = onMoveLeft,
        onMoveRight  = onMoveRight,
        onDelete     = onDelete,
    )
}

// ─── Umumiy rasm kartochka bazasi ─────────────────────────────────────────────

@Composable
private fun ImageCardBase(
    bitmap: ImageBitmap?,
    isFirst: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    saving: Boolean,
    isNew: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFirst) 2.dp else 1.dp,
                color = if (isFirst) MexaWarehouseColors.primary else MexaWarehouseColors.borderSubtle,
                shape = RoundedCornerShape(10.dp),
            )
            .background(MexaWarehouseColors.surfaceContainerLow),
    ) {
        // Rasm yoki placeholder
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MexaWarehouseColors.primary,
                )
            }
        }

        // "ASOSIY" badge — birinchi rasm
        if (isFirst) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(MexaWarehouseColors.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text("ASOSIY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // "YANGI" badge
        if (isNew) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(if (isFirst) 4.dp else 4.dp)
                    .padding(top = if (isFirst) 22.dp else 0.dp)
                    .background(MexaWarehouseColors.greenBadgeBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text("YANGI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.greenBadgeFg)
            }
        }

        // O'chirish tugmasi (yuqori o'ng)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .background(MexaWarehouseColors.danger, CircleShape)
                .clickable(enabled = !saving) { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "O'chirish",
                tint = Color.White,
                modifier = Modifier.size(10.dp),
            )
        }

        // Tartib tugmalari — pastda
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sol (<)
            Box(
                Modifier
                    .size(32.dp)
                    .then(
                        if (canMoveLeft && !saving) Modifier.clickable { onMoveLeft() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "←",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canMoveLeft) Color.White else Color.White.copy(alpha = 0.3f),
                )
            }
            // O'ng (>)
            Box(
                Modifier
                    .size(32.dp)
                    .then(
                        if (canMoveRight && !saving) Modifier.clickable { onMoveRight() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "→",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canMoveRight) Color.White else Color.White.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscountDatePickerDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = current.takeIf { it.isNotBlank() }?.let {
        runCatching { java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onConfirm(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString())
                }
                onDismiss()
            }) { Text("Tanlash", color = MexaWarehouseColors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor", color = MexaWarehouseColors.textMuted) }
        },
    ) {
        DatePicker(state = state)
    }
}

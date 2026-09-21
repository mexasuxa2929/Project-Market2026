package mexa.club.desktop_app.market.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.auth.AuthSession
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

// ─── Data models ───────────────────────────────────────────────────────────────

data class TierInfo(
    val minQty: Int,
    val maxQty: Int?,
    val price: String,       // formatted e.g. "150 000"
    val priceType: String,   // RETAIL / WHOLESALE / PURCHASE
)

data class ProductRowData(
    val id: String,
    val name: String,
    val barcode: String,
    val categoryName: String,
    val brandName: String,
    val brandId: String = "",
    val unit: String,
    val active: Boolean,
    val tags: List<String>,
    val imageUrl: String = "",
    /** Joriy narx (chegirma qo'llangan holda) — formatlangan, masalan "120 000" */
    val price: String = "",
    /** Aktiv chegirma foizi (0 = chegirma yo'q) */
    val discountPercent: Int = 0,
    /** Narx bosqichlari — faqat tooltip (hover) ochilganda lazily yuklanadi */
    val tiers: List<TierInfo> = emptyList(),
    /** Guruhdagi ranglar soni (vakil + siblingColors). 1 bo'lsa oddiy mahsulot, >1 bo'lsa "N rang" belgisi ko'rsatiladi. */
    val colorCount: Int = 1,
)

/** "150000" → "150 000" formatlash (umumiy yordamchi). */
private fun formatAmount(bd: java.math.BigDecimal): String =
    bd.toLong().toString().let { s ->
        buildString {
            var i = 0
            s.reversed().forEach { ch ->
                if (i > 0 && i % 3 == 0) append(' ')
                append(ch); i++
            }
        }.reversed()
    }

data class DropdownItem(val id: String, val name: String, val active: Boolean = true)

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    // FAQAT super admin mahsulotlarni qo'sha/tahrirlay/o'chiradi; oddiy admin faqat ko'radi.
    val canManageProducts = AuthSession.isSuperAdmin

    // ── State ──────────────────────────────────────────────────────────────────
    var products        by remember { mutableStateOf<List<ProductRowData>>(emptyList()) }
    var categories      by remember { mutableStateOf<List<DropdownItem>>(emptyList()) }
    var brands          by remember { mutableStateOf<List<DropdownItem>>(emptyList()) }
    var manufacturers   by remember { mutableStateOf<List<DropdownItem>>(emptyList()) }
    var loading         by remember { mutableStateOf(true) }
    var error           by remember { mutableStateOf<String?>(null) }
    var showAddDialog              by remember { mutableStateOf(false) }
    var showManageBrandsDialog     by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var viewingProduct             by remember { mutableStateOf<ProductRowData?>(null) }
    var editingProduct             by remember { mutableStateOf<ProductRowData?>(null) }
    var deletingProduct            by remember { mutableStateOf<ProductRowData?>(null) }
    val snackbarHostState          = remember { SnackbarHostState() }

    // Filters (real-time)
    var searchQuery     by remember { mutableStateOf("") }
    var catId           by remember { mutableStateOf("") }
    var catName         by remember { mutableStateOf("Barcha kategoriyalar") }
    var brandId         by remember { mutableStateOf("") }
    var brandName       by remember { mutableStateOf("Barcha brendlar") }
    var activeTab       by remember { mutableStateOf("Hammasi") }

    // Pagination
    var page            by remember { mutableStateOf(0) }
    var pageSize        by remember { mutableStateOf(20) }
    var totalElements   by remember { mutableStateOf(0) }
    var totalPages      by remember { mutableStateOf(1) }
    var refreshKey      by remember { mutableStateOf(0) }

    // Dropdown state
    var catExpanded       by remember { mutableStateOf(false) }
    var brandExpanded     by remember { mutableStateOf(false) }
    var pageSizeExpanded  by remember { mutableStateOf(false) }
    var addMenuExpanded   by remember { mutableStateOf(false) }

    // ── Fetch products ─────────────────────────────────────────────────────────
    LaunchedEffect(page, pageSize, refreshKey, searchQuery, catId, brandId, activeTab) {
        // Search uchun 400ms debounce
        if (searchQuery.isNotBlank()) delay(400)

        val activeParam = when (activeTab) {
            "Faol"   -> "&active=true"
            "Nofaol" -> "&active=false"
            else     -> ""
        }
        val nameParam   = if (searchQuery.isNotBlank()) "&name=${searchQuery.trim()}" else ""
        val catParam    = if (catId.isNotBlank()) "&categoryId=$catId" else ""
        val brandParam  = if (brandId.isNotBlank()) "&brandId=$brandId" else ""
        val url = "/api/products?page=$page&size=$pageSize$nameParam$catParam$brandParam$activeParam"

        loading = true
        error   = null
        runCatching {
            withContext(Dispatchers.IO) {
                val text = ApiClient.get(url)
                val root = ApiClient.parseJsonObject(text)
                val data = root?.let { ApiClient.dataObjectOrSelf(it) }
                val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                totalElements = (data?.get("totalElements") as? JsonPrimitive)?.content?.toIntOrNull() ?: items.size
                totalPages    = (data?.get("totalPages")    as? JsonPrimitive)?.content?.toIntOrNull()
                    ?.coerceAtLeast(1) ?: 1
                items.mapNotNull { el ->
                    val p = el as? JsonObject ?: return@mapNotNull null
                    val id = p.stringField("id").ifEmpty { return@mapNotNull null }
                    val rawTags = (p["tags"] as? JsonArray)
                        ?.mapNotNull { (it as? JsonPrimitive)?.content?.ifEmpty { null } }
                        ?: emptyList()
                    val firstImage = (p["imageUrls"] as? JsonArray)
                        ?.firstOrNull()
                        ?.let { (it as? JsonPrimitive)?.content }
                        ?: ""
                    // Rang soni = guruhdagi boshqa ranglar (siblingColors) + shu vakil o'zi.
                    val colorCount = ((p["siblingColors"] as? JsonArray)?.size ?: 0) + 1
                    // Joriy narx (backend chegirmani qo'llagan holda qaytaradi: basePrice)
                    val price = (p["basePrice"] as? JsonPrimitive)?.content
                        ?.toBigDecimalOrNull()
                        ?.takeIf { it > java.math.BigDecimal.ZERO }
                        ?.let { formatAmount(it) } ?: ""
                    val discount = (p["discountPercent"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                    ProductRowData(
                        id           = id,
                        name         = p.stringField("name"),
                        barcode      = p.stringField("barcode"),
                        categoryName = p.stringField("categoryName"),
                        brandName    = p.stringField("brandName"),
                        brandId      = p.stringField("brandId"),
                        unit         = p.stringField("unit").ifEmpty { "dona" },
                        active       = (p["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                        tags         = rawTags,
                        imageUrl     = firstImage,
                        colorCount   = colorCount,
                        price        = price,
                        discountPercent = discount,
                    )
                }
            }
        }.onSuccess { list ->
            products = list
        }
         .onFailure { error = it.message ?: "Xatolik" }
        loading = false
    }

    // ── Fetch categories + brands once ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/categories?size=100")
                    val root = ApiClient.parseJsonObject(text)
                    val data = root?.let { ApiClient.dataObject(it) }
                    val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    items.mapNotNull { el ->
                        val c = el as? JsonObject ?: return@mapNotNull null
                        val id = c.stringField("id").ifEmpty { return@mapNotNull null }
                        DropdownItem(id = id, name = c.stringField("name"))
                    }
                }
            }.onSuccess { categories = it }
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/brands?size=200")
                    val root = ApiClient.parseJsonObject(text)
                    val data = root?.let { ApiClient.dataObject(it) }
                    val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    items.mapNotNull { el ->
                        val b = el as? JsonObject ?: return@mapNotNull null
                        val id = b.stringField("id").ifEmpty { return@mapNotNull null }
                        val isActive = (b["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
                        DropdownItem(id = id, name = b.stringField("name"), active = isActive)
                    }
                }
            }.onSuccess { brands = it }
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/manufacturers?size=200")
                    val root = ApiClient.parseJsonObject(text)
                    val data = root?.let { ApiClient.dataObject(it) }
                    val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                    items.mapNotNull { el ->
                        val m = el as? JsonObject ?: return@mapNotNull null
                        val id = m.stringField("id").ifEmpty { return@mapNotNull null }
                        DropdownItem(id = id, name = m.stringField("name"))
                    }
                }
            }.onSuccess { manufacturers = it }
        }
    }

    fun clearFilters() {
        searchQuery = ""
        catId = ""; catName = "Barcha kategoriyalar"
        brandId = ""; brandName = "Barcha brendlar"
        activeTab = "Hammasi"
        page = 0
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    viewingProduct?.let { prod ->
        ProductViewDialog(
            product   = prod,
            onDismiss = { viewingProduct = null },
            onEdit    = if (canManageProducts) {
                {
                    viewingProduct = null
                    editingProduct = prod
                }
            } else {
                null
            },
        )
    }

    if (showAddDialog) {
        AddProductDialog(
            categories    = categories,
            brands        = brands.filter { it.active },
            onDismiss     = { showAddDialog = false },
            onSaved    = { warnings ->
                showAddDialog = false
                if (warnings.isNotEmpty()) {
                    scope.launch { snackbarHostState.showSnackbar(warnings.joinToString("\n")) }
                }
                if (page == 0) refreshKey++ else page = 0
            },
        )
    }

    if (showManageBrandsDialog) {
        BrandsManageDialog(
            onDismiss = { showManageBrandsDialog = false },
            onChanged = {
                // brands listni yangilaymiz
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val text = ApiClient.get("/api/brands?size=200")
                            val root = ApiClient.parseJsonObject(text)
                            val data = root?.let { ApiClient.dataObject(it) }
                            val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                            items.mapNotNull { el ->
                                val b = el as? JsonObject ?: return@mapNotNull null
                                val id = b.stringField("id").ifEmpty { return@mapNotNull null }
                                val isActive = (b["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
                                DropdownItem(id = id, name = b.stringField("name"), active = isActive)
                            }
                        }
                    }.onSuccess { brands = it }
                }
            },
        )
    }

    if (showManageCategoriesDialog) {
        CategoriesManageDialog(
            onDismiss = { showManageCategoriesDialog = false },
            onChanged = {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val text = ApiClient.get("/api/categories?size=100")
                            val root = ApiClient.parseJsonObject(text)
                            val data = root?.let { ApiClient.dataObject(it) }
                            val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
                            items.mapNotNull { el ->
                                val c = el as? JsonObject ?: return@mapNotNull null
                                val id = c.stringField("id").ifEmpty { return@mapNotNull null }
                                DropdownItem(id = id, name = c.stringField("name"))
                            }
                        }
                    }.onSuccess { categories = it }
                }
            },
        )
    }

    editingProduct?.let { product ->
        EditProductDialog(
            product       = product,
            categories    = categories,
            brands        = brands.filter { it.active || it.id == product.brandId },
            onDismiss     = { editingProduct = null },
            onSaved       = { warnings ->
                editingProduct = null
                if (warnings.isNotEmpty()) {
                    scope.launch { snackbarHostState.showSnackbar(warnings.joinToString("\n")) }
                }
                if (page == 0) refreshKey++ else page = 0
            },
            onOpenSibling = { sibling -> editingProduct = sibling },
        )
    }

    deletingProduct?.let { product ->
        DeleteProductDialog(
            product   = product,
            onDismiss = { deletingProduct = null },
            onDeleted = {
                deletingProduct = null
                if (page == 0) refreshKey++ else page = 0
            },
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Mahsulotlar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary,
                )
                Text(
                    "Katalogdagi barcha mahsulotlar",
                    fontSize = 13.sp,
                    color = MexaWarehouseColors.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ── Add dropdown menu ─────────────────────────────────────
                if (canManageProducts) Box {
                    Button(
                        onClick = { addMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Qo'shish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = addMenuExpanded,
                        onDismissRequest = { addMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.primary)
                                    Text("Mahsulot qo'shish", fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                addMenuExpanded = false
                                showAddDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                    Text("Brendlarni boshqarish", fontSize = 14.sp)
                                }
                            },
                            onClick = { addMenuExpanded = false; showManageBrandsDialog = true },
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                                    Text("Kategoriyalarni boshqarish", fontSize = 14.sp)
                                }
                            },
                            onClick = { addMenuExpanded = false; showManageCategoriesDialog = true },
                        )
                    }
                }
            }
        }

        // ── Filter panel ──────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MexaWarehouseColors.surfaceLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Search
                Column(Modifier.weight(1.6f)) {
                    Text(
                        "QIDIRUV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; page = 0 },
                        placeholder = { Text("Nomi, kodi yoki barcode.", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MexaWarehouseColors.textMuted,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                            focusedBorderColor   = MexaWarehouseColors.primary,
                        ),
                        textStyle = TextStyle(fontSize = 14.sp),
                    )
                }

                // Category dropdown
                Column(Modifier.weight(1f)) {
                    Text(
                        "KATEGORIYA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = catName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                                focusedBorderColor   = MexaWarehouseColors.primary,
                            ),
                            textStyle = TextStyle(fontSize = 13.sp),
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Barcha kategoriyalar") },
                                onClick = { catId = ""; catName = "Barcha kategoriyalar"; catExpanded = false; page = 0 },
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { catId = cat.id; catName = cat.name; catExpanded = false; page = 0 },
                                )
                            }
                        }
                    }
                }

                // Brand dropdown
                Column(Modifier.weight(1f)) {
                    Text(
                        "BREND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MexaWarehouseColors.textMuted,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    ExposedDropdownMenuBox(
                        expanded = brandExpanded,
                        onExpandedChange = { brandExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = brandName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MexaWarehouseColors.outlineVariant,
                                focusedBorderColor   = MexaWarehouseColors.primary,
                            ),
                            textStyle = TextStyle(fontSize = 13.sp),
                        )
                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Barcha brendlar") },
                                onClick = { brandId = ""; brandName = "Barcha brendlar"; brandExpanded = false; page = 0 },
                            )
                            brands.forEach { brand ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(brand.name, fontSize = 13.sp)
                                            if (!brand.active) {
                                                Text(
                                                    "(Nofaol)",
                                                    fontSize = 11.sp,
                                                    color = MexaWarehouseColors.textMuted,
                                                )
                                            }
                                        }
                                    },
                                    onClick = { brandId = brand.id; brandName = brand.name; brandExpanded = false; page = 0 },
                                )
                            }
                        }
                    }
                }

                // Filtrni tozalash tugmasi
                IconButton(
                    onClick = { clearFilters() },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp))
                        .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp)),
                ) {
                    Icon(
                        Icons.Filled.FilterAltOff,
                        contentDescription = "Filtrni tozalash",
                        tint = MexaWarehouseColors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ── Status tabs ───────────────────────────────────────────────────────
        Row(
            Modifier
                .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
                .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf("Hammasi", "Faol", "Nofaol").forEach { tab ->
                val selected = activeTab == tab
                Surface(
                    onClick = {
                        if (activeTab != tab) {
                            activeTab = tab
                            if (page == 0) refreshKey++ else page = 0
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) MexaWarehouseColors.primary else Color.Transparent,
                ) {
                    Text(
                        tab,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else MexaWarehouseColors.textMuted,
                    )
                }
            }
        }

        // ── Table ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MexaWarehouseColors.surfaceLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        ) {
            Column {
                // Table header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MexaWarehouseColors.tableHeaderBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // image col
                    Spacer(Modifier.width(56.dp))
                    Spacer(Modifier.width(14.dp))
                    HeaderCell("NOMI VA TEGLAR", Modifier.weight(1.6f))
                    HeaderCell("NARX",           Modifier.weight(0.9f))
                    HeaderCell("KATEGORIYA",     Modifier.weight(1f))
                    HeaderCell("BREND",          Modifier.weight(0.8f))
                    HeaderCell("BIRLIK",         Modifier.width(64.dp))
                    HeaderCell("BARCODE",        Modifier.weight(1f))
                    HeaderCell("HOLATI",         Modifier.width(90.dp))
                    Spacer(Modifier.width(96.dp))
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)

                // Body
                when {
                    loading -> Box(
                        Modifier.fillMaxWidth().height(280.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MexaWarehouseColors.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    error != null -> Box(
                        Modifier.fillMaxWidth().height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                error ?: "Xatolik yuz berdi",
                                color = MexaWarehouseColors.danger,
                                fontSize = 14.sp,
                            )
                            Button(
                                onClick = { refreshKey++ },
                                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                            ) {
                                Text("Qayta urinish")
                            }
                        }
                    }

                    products.isEmpty() -> Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MexaWarehouseColors.outlineVariant,
                            )
                            Text(
                                "Mahsulotlar topilmadi",
                                fontSize = 15.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        }
                    }

                    else -> products.forEachIndexed { index, product ->
                        ProductTableRow(
                            product          = product,
                            canEdit          = canManageProducts,
                            onView           = { viewingProduct = product },
                            onEdit           = { editingProduct = product },
                            onDelete         = { deletingProduct = product },
                        )
                        if (index < products.lastIndex) {
                            HorizontalDivider(
                                color = MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f),
                                thickness = 1.dp,
                            )
                        }
                    }
                }

                // ── Pagination ────────────────────────────────────────────────
                HorizontalDivider(color = MexaWarehouseColors.borderSubtle, thickness = 1.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val from = if (totalElements == 0) 0 else page * pageSize + 1
                    val to   = minOf((page + 1) * pageSize, totalElements)
                    Text(
                        "$from - $to / $totalElements ta mahsulot",
                        fontSize = 13.sp,
                        color = MexaWarehouseColors.textMuted,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Page size selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "QATORLAR:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MexaWarehouseColors.textMuted,
                            )
                            Box {
                                Surface(
                                    onClick = { pageSizeExpanded = true },
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                                    color = MexaWarehouseColors.surfaceLowest,
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            pageSize.toString(),
                                            fontSize = 13.sp,
                                            color = MexaWarehouseColors.textPrimary,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MexaWarehouseColors.textMuted,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = pageSizeExpanded,
                                    onDismissRequest = { pageSizeExpanded = false },
                                ) {
                                    listOf(10, 20, 50, 100).forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s.toString(), fontSize = 13.sp) },
                                            onClick = { pageSize = s; page = 0; pageSizeExpanded = false },
                                        )
                                    }
                                }
                            }
                        }

                        // Page nav
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PageNavBtn("⏮", page > 0) { page = 0 }
                            PageNavBtn("‹", page > 0) { page-- }

                            // Visible page numbers (window of 5)
                            val half  = 2
                            val start = (page - half).coerceAtLeast(0)
                            val end   = (page + half).coerceAtMost(totalPages - 1)

                            if (start > 0) {
                                PageNumBtn(1, page == 0) { page = 0 }
                                if (start > 1) EllipsisLabel()
                            }
                            for (p in start..end) {
                                PageNumBtn(p + 1, p == page) { page = p }
                            }
                            if (end < totalPages - 1) {
                                if (end < totalPages - 2) EllipsisLabel()
                                PageNumBtn(totalPages, page == totalPages - 1) { page = totalPages - 1 }
                            }

                            PageNavBtn("›", page < totalPages - 1) { page++ }
                            PageNavBtn("⏭", page < totalPages - 1) { page = totalPages - 1 }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

// ─── Table header cell ─────────────────────────────────────────────────────────

@Composable
private fun HeaderCell(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MexaWarehouseColors.textMuted,
    )
}

// ─── Product row ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductTableRow(
    product: ProductRowData,
    canEdit: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Product image
        ProductImage(url = product.imageUrl, size = 56.dp)
        Spacer(Modifier.width(14.dp))

        // Name + tags
        Column(Modifier.weight(1.6f)) {
            Text(
                product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MexaWarehouseColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (product.tags.isNotEmpty() || product.colorCount > 1) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (product.colorCount > 1) ColorCountBadge(product.colorCount)
                    product.tags.take(3).forEach { tag -> TagBadge(tag) }
                }
            }
        }

        // Narx (hover → barcha tierlar)
        PriceCell(product = product, modifier = Modifier.weight(0.9f))

        Text(
            product.categoryName,
            fontSize = 13.sp,
            color = MexaWarehouseColors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            product.brandName,
            fontSize = 13.sp,
            color = MexaWarehouseColors.textPrimary,
            modifier = Modifier.weight(0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            product.unit,
            fontSize = 13.sp,
            color = MexaWarehouseColors.textPrimary,
            modifier = Modifier.width(64.dp),
        )
        Text(
            product.barcode,
            fontSize = 12.sp,
            color = MexaWarehouseColors.textMuted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Status badge
        Box(Modifier.width(90.dp)) {
            val (bg, fg, label) = if (product.active)
                Triple(MexaWarehouseColors.statusActiveBg,  MexaWarehouseColors.statusActiveFg,  "FAOL")
            else
                Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
            Box(
                Modifier
                    .background(bg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
            }
        }

        // Actions
        Row(
            Modifier.width(96.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onView, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = "Ko'rish",
                    tint = MexaWarehouseColors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (canEdit) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Tahrirlash",
                        tint = MexaWarehouseColors.indigoAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "O'chirish",
                        tint = MexaWarehouseColors.danger,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ─── Tag badge ─────────────────────────────────────────────────────────────────

@Composable
private fun ColorCountBadge(count: Int) {
    Box(
        Modifier
            .background(Color(0xFFEDE9FE), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text("$count rang", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6D28D9))
    }
}

@Composable
private fun TagBadge(tag: String) {
    val (bg, fg) = when (tag.lowercase()) {
        "yangi"     -> MexaWarehouseColors.blueBadgeBg  to MexaWarehouseColors.blueBadgeFg
        "ommabop"   -> Color(0xFFEDE9FE)                to Color(0xFF6D28D9)
        "luks"      -> MexaWarehouseColors.amberBadgeBg to MexaWarehouseColors.amberBadgeFg
        "kam qoldi" -> MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
        "chegirma"  -> MexaWarehouseColors.greenBadgeBg to MexaWarehouseColors.greenBadgeFg
        else        -> MexaWarehouseColors.tableHeaderBg to MexaWarehouseColors.textMuted
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(tag, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ─── Pagination helpers ─────────────────────────────────────────────────────────

@Composable
private fun PageNavBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(
                1.dp,
                if (enabled) MexaWarehouseColors.outlineVariant else MexaWarehouseColors.borderSubtle,
                RoundedCornerShape(6.dp),
            )
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (enabled) MexaWarehouseColors.textPrimary else MexaWarehouseColors.textMuted,
        )
    }
}

@Composable
private fun PageNumBtn(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.surfaceLowest,
                RoundedCornerShape(6.dp),
            )
            .border(
                1.dp,
                if (selected) MexaWarehouseColors.primary else MexaWarehouseColors.outlineVariant,
                RoundedCornerShape(6.dp),
            )
            .then(if (!selected) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MexaWarehouseColors.textPrimary,
        )
    }
}

@Composable
private fun EllipsisLabel() {
    Text(
        "…",
        fontSize = 13.sp,
        color = MexaWarehouseColors.textMuted,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

// ─── Product image ─────────────────────────────────────────────────────────────
// toPublicUrl() nisbiy yo'l qaytaradi (/api/files/...), shuning uchun:
// 1. ApiClient.fullPath() bilan to'liq URL yasaymiz
// 2. /api/files/** auth talab qiladi → JWT header qo'shamiz

@Composable
private fun ProductImage(url: String, size: androidx.compose.ui.unit.Dp) {
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isBlank()) return@LaunchedEffect
        bitmap = runCatching {
            withContext(Dispatchers.IO) {
                val fullUrl = ApiClient.fullPath(url)
                val connection = java.net.URL(fullUrl).openConnection() as java.net.HttpURLConnection
                val token = mexa.club.desktop_app.market.session.SessionManager.token.takeIf { it.isNotEmpty() }
                    ?: mexa.club.desktop_app.auth.AuthSession.accessToken
                if (!token.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                connection.connectTimeout = 5_000
                connection.readTimeout    = 10_000
                connection.connect()
                val bytes = connection.inputStream.use { it.readBytes() }
                connection.disconnect()
                SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        }.getOrNull()
    }

    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MexaWarehouseColors.surfaceContainerLow)
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                tint = MexaWarehouseColors.outlineVariant,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

// ─── Price cell with hover tooltip ────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PriceCell(product: ProductRowData, modifier: Modifier = Modifier) {
    val typeLabel = mapOf(
        "RETAIL"    to "Chakana",
        "WHOLESALE" to "Ulgurji",
        "PURCHASE"  to "Kirim",
    )
    // Narx bosqichlari tooltip (hover) ochilganda bir marta lazily yuklanadi.
    var tiers by remember(product.id) { mutableStateOf(product.tiers) }
    var tiersLoading by remember(product.id) { mutableStateOf(false) }

    // Asl narx = joriy narx / (100 - pct) * 100 (chegirma tugagach qaytadigan narx)
    val originalPrice = if (product.discountPercent > 0 && product.price.isNotBlank()) {
        product.price.replace(" ", "").toBigDecimalOrNull()?.let {
            it.multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf((100 - product.discountPercent).toLong()), 0, java.math.RoundingMode.HALF_UP)
                .let { bd -> formatAmount(bd) }
        }
    } else null

    TooltipArea(
        modifier = modifier,
        tooltip = {
            if (tiers.isEmpty() && !tiersLoading) {
                LaunchedEffect(product.id) {
                    tiersLoading = true
                    val loaded = withContext(Dispatchers.IO) {
                        runCatching {
                            val text = ApiClient.get("/api/products/${product.id}/price-tiers")
                            val arr  = ApiClient.parseJsonObject(text)?.get("data") as? JsonArray
                            arr?.filterIsInstance<JsonObject>()?.mapNotNull { t ->
                                val bd = (t["price"] as? JsonPrimitive)?.content
                                    ?.toBigDecimalOrNull()
                                    ?.takeIf { it > java.math.BigDecimal.ZERO }
                                    ?: return@mapNotNull null
                                TierInfo(
                                    minQty    = (t["minQty"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                                    maxQty    = (t["maxQty"] as? JsonPrimitive)?.content?.toIntOrNull(),
                                    price     = formatAmount(bd),
                                    priceType = (t["priceType"] as? JsonPrimitive)?.content ?: "RETAIL",
                                )
                            }?.sortedBy { it.minQty } ?: emptyList()
                        }.getOrElse { emptyList() }
                    }
                    tiers = loaded
                    tiersLoading = false
                }
            }
            if (tiers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MexaWarehouseColors.surfaceLowest,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                ) {
                    Text(
                        if (tiersLoading) "Bosqichlar yuklanmoqda..." else "Narx bosqichlari yo'q",
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MexaWarehouseColors.surfaceLowest,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Narx bosqichlari",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.textMuted,
                        )
                        tiers.forEachIndexed { idx, tier ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Tier raqami
                                Box(
                                    Modifier
                                        .size(18.dp)
                                        .background(MexaWarehouseColors.primary, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${idx + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                                // Miqdor oraliq
                                val range = if (tier.maxQty != null)
                                    "${tier.minQty}–${tier.maxQty} ${product.unit}"
                                else
                                    "${tier.minQty}+ ${product.unit}"
                                Text(
                                    range,
                                    fontSize = 12.sp,
                                    color = MexaWarehouseColors.textMuted,
                                    modifier = Modifier.width(90.dp),
                                )
                                // Tur (Chakana/Ulgurji/Kirim)
                                Text(
                                    typeLabel[tier.priceType] ?: tier.priceType,
                                    fontSize = 11.sp,
                                    color = MexaWarehouseColors.textMuted,
                                    modifier = Modifier.width(52.dp),
                                )
                                // Narx
                                Text(
                                    "${tier.price} so'm",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (idx == 0) MexaWarehouseColors.primary
                                            else MexaWarehouseColors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
        },
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.BottomEnd,
            offset = DpOffset(0.dp, 8.dp),
        ),
        delayMillis = 200,
    ) {
        if (product.price.isNotBlank()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${product.price} so'm",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (product.discountPercent > 0) MexaWarehouseColors.danger
                                else MexaWarehouseColors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (product.discountPercent > 0) {
                        Box(
                            Modifier
                                .background(MexaWarehouseColors.danger.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "−${product.discountPercent}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.danger,
                            )
                        }
                    }
                    if (tiers.size > 1) {
                        Box(
                            Modifier
                                .background(MexaWarehouseColors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "${tiers.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.primary,
                            )
                        }
                    }
                }
                if (originalPrice != null) {
                    Text(
                        "${originalPrice} so'm",
                        fontSize = 11.sp,
                        color = MexaWarehouseColors.textMuted,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Text("—", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
        }
    }
}

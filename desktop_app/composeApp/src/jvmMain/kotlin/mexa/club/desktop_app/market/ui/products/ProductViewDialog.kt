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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import org.jetbrains.skia.Image as SkiaImage

private fun fmtMoney(n: Long): String {
    val s = n.toString()
    return buildString {
        var i = 0; s.reversed().forEach { ch -> if (i > 0 && i % 3 == 0) append(' '); append(ch); i++ }
    }.reversed()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductViewDialog(
    product: ProductRowData,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    // ── State ──────────────────────────────────────────────────────────────────
    var loading  by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Detail fields
    var barcode      by remember { mutableStateOf(product.barcode) }
    var categoryName by remember { mutableStateOf(product.categoryName) }
    var brandName    by remember { mutableStateOf(product.brandName) }
    var unit         by remember { mutableStateOf(product.unit) }
    var active       by remember { mutableStateOf(product.active) }
    var tags         by remember { mutableStateOf(product.tags) }
    var minStock     by remember { mutableStateOf(0) }
    var leadTimeDays by remember { mutableStateOf(0) }
    var weight       by remember { mutableStateOf(0.0) }
    var dimL         by remember { mutableStateOf(0.0) }
    var dimW         by remember { mutableStateOf(0.0) }
    var dimH         by remember { mutableStateOf(0.0) }
    var packageType  by remember { mutableStateOf("") }
    var fragile      by remember { mutableStateOf(false) }
    var productCode  by remember { mutableStateOf("") }

    // Ranglar guruhi (group_id orqali bog'langan boshqa rangdagi mahsulotlar) — faqat o'qish uchun
    var siblingColors by remember { mutableStateOf<List<ProductColorSiblingUi>>(emptyList()) }

    // Narx tiers
    var tiers by remember { mutableStateOf(product.tiers) }

    // Chegirma (backend ProductResponse: discountPercent + basePrice = joriy narx)
    var discountPercent by remember { mutableStateOf(0) }
    var currentPrice    by remember { mutableStateOf("") }

    // Rasmlar
    var imageUrls    by remember { mutableStateOf<List<String>>(listOfNotNull(product.imageUrl.ifBlank { null })) }
    var imageBitmaps by remember { mutableStateOf<Map<String, ImageBitmap?>>(emptyMap()) }
    var selectedIdx  by remember { mutableStateOf(0) }

    // Reyting & Sharhlar
    var avgRating     by remember { mutableStateOf(0.0) }
    var reviewCount   by remember { mutableStateOf(0L) }
    var ratingDist    by remember { mutableStateOf<Map<Int,Long>>(emptyMap()) }
    var reviews       by remember { mutableStateOf<List<ReviewItem>>(emptyList()) }
    var reviewsLoading by remember { mutableStateOf(false) }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(product.id) {
        loading   = true
        loadError = null
        runCatching {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    // 1. To'liq ma'lumot
                    val detailJob = async {
                        val text = ApiClient.get("/api/products/${product.id}")
                        ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) } as? JsonObject
                    }
                    // 2. Rasmlar
                    val imagesJob = async {
                        runCatching {
                            val text = ApiClient.get("/api/products/${product.id}/images")
                            (ApiClient.parseJsonObject(text)?.get("data") as? JsonArray)
                                ?.mapNotNull { (it as? JsonPrimitive)?.content?.ifBlank { null } }
                                ?: emptyList()
                        }.getOrElse { emptyList() }
                    }
                    // 3. Narx bosqichlari
                    val tiersJob = async {
                        runCatching {
                            val text = ApiClient.get("/api/products/${product.id}/price-tiers")
                            val arr  = ApiClient.parseJsonObject(text)?.get("data") as? JsonArray
                            arr?.filterIsInstance<JsonObject>()
                                ?.mapNotNull { t ->
                                    val bd = (t["price"] as? JsonPrimitive)?.content
                                        ?.toBigDecimalOrNull()
                                        ?.takeIf { it > java.math.BigDecimal.ZERO }
                                        ?: return@mapNotNull null
                                    val fmt = bd.toLong().toString().let { s ->
                                        buildString {
                                            var i = 0; s.reversed().forEach { ch ->
                                                if (i > 0 && i % 3 == 0) append(' '); append(ch); i++
                                            }
                                        }.reversed()
                                    }
                                    TierInfo(
                                        minQty    = (t["minQty"]    as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                                        maxQty    = (t["maxQty"]    as? JsonPrimitive)?.content?.toIntOrNull(),
                                        price     = fmt,
                                        priceType = (t["priceType"] as? JsonPrimitive)?.content ?: "RETAIL",
                                    )
                                }
                                ?.sortedBy { it.minQty }
                                ?: emptyList()
                        }.getOrElse { emptyList() }
                    }

                    Triple(detailJob.await(), imagesJob.await(), tiersJob.await())
                }
            }
        }.onSuccess { (data, imgs, tierList) ->
            if (data != null) {
                barcode      = data.stringField("barcode").ifBlank { product.barcode }
                categoryName = data.stringField("categoryName").ifBlank { product.categoryName }
                brandName    = data.stringField("brandName").ifBlank { product.brandName }
                unit         = data.stringField("unit").ifBlank { product.unit }
                active       = (data["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: product.active
                minStock     = (data["minStock"]     as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                leadTimeDays = (data["leadTimeDays"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                weight       = (data["weight"]       as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                dimL         = (data["length"]       as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                dimW         = (data["width"]        as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                dimH         = (data["height"]       as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                packageType  = data.stringField("packageType")
                fragile      = (data["fragile"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                productCode  = data.stringField("productCode").ifBlank { "PRD-${product.id.take(8).uppercase()}" }
                discountPercent = (data["discountPercent"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                currentPrice    = (data["basePrice"] as? JsonPrimitive)?.content
                    ?.toBigDecimalOrNull()
                    ?.let { fmtMoney(it.toLong()) }
                    ?: ""
                tags         = (data["tags"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content?.ifEmpty { null } }
                    ?: product.tags
                siblingColors = (data["siblingColors"] as? JsonArray)
                    ?.filterIsInstance<JsonObject>()
                    ?.mapNotNull { o ->
                        val id = o.stringField("id").ifEmpty { return@mapNotNull null }
                        ProductColorSiblingUi(
                            id       = id,
                            color    = o.stringField("color"),
                            colorCode = o.stringField("colorCode"),
                            barcode  = o.stringField("barcode"),
                            imageUrl = o.stringField("imageUrl"),
                            active   = (o["active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                            isCurrent = id == product.id,
                        )
                    }
                    ?: emptyList()
            }
            if (imgs.isNotEmpty()) imageUrls = imgs
            if (tierList.isNotEmpty()) tiers = tierList

            // Bitmap larni yuklash
            val bitmaps = imageUrls.associateWith { url ->
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
            imageBitmaps = bitmaps
        }.onFailure { loadError = it.message }

        loading = false

        // Reyting + sharhlarni yuklash (parallel, xato bo'lsa e'tiborsiz)
        reviewsLoading = true
        runCatching {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val ratingJob = async {
                        runCatching {
                            val text = ApiClient.get("/api/shops/products/${product.id}/rating")
                            val data = ApiClient.parseJsonObject(text)?.get("data") as? JsonObject
                            if (data != null) {
                                val avg = (data["avgRating"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                                val cnt = (data["reviewCount"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                                val dist = mutableMapOf<Int, Long>()
                                (data["distribution"] as? JsonObject)?.entries?.forEach { (k, v) ->
                                    dist[k.toIntOrNull() ?: 0] = (v as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                                }
                                Triple(avg, cnt, dist.toMap())
                            } else null
                        }.getOrNull()
                    }
                    val reviewsJob = async {
                        runCatching {
                            val text = ApiClient.get("/api/shops/products/${product.id}/reviews?page=0&size=10")
                            val data = ApiClient.parseJsonObject(text)?.get("data") as? JsonObject
                            val arr  = data?.get("content") as? JsonArray
                            arr?.filterIsInstance<JsonObject>()?.map { r ->
                                ReviewItem(
                                    id        = r.stringField("id"),
                                    username  = r.stringField("username").ifBlank { "Foydalanuvchi" },
                                    rating    = (r["rating"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                                    comment   = r.stringField("comment"),
                                    createdAt = r.stringField("createdAt").take(10),
                                )
                            } ?: emptyList()
                        }.getOrElse { emptyList() }
                    }
                    Pair(ratingJob.await(), reviewsJob.await())
                }
            }
        }.onSuccess { (ratingData, reviewList) ->
            ratingData?.let { (avg, cnt, dist) ->
                avgRating   = avg
                reviewCount = cnt
                ratingDist  = dist
            }
            reviews = reviewList
        }
        reviewsLoading = false
    }

    // ── Dialog ────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside   = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 560.dp, max = 860.dp)
                .fillMaxWidth(0.95f),
            shape    = RoundedCornerShape(16.dp),
            color    = MexaWarehouseColors.surfaceLowest,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.heightIn(max = 820.dp)) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            product.name,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MexaWarehouseColors.textPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false),
                        )
                        // PRD badge
                        if (productCode.isNotBlank()) {
                            Box(
                                Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(productCode, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                        // Holat badge
                        val (bg, fg, label) = if (active)
                            Triple(MexaWarehouseColors.statusActiveBg,  MexaWarehouseColors.statusActiveFg,  "FAOL")
                        else
                            Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
                        Box(
                            Modifier
                                .background(bg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                if (loading) {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MexaWarehouseColors.primary, modifier = Modifier.size(40.dp))
                    }
                } else if (loadError != null) {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(loadError!!, color = MexaWarehouseColors.danger, fontSize = 14.sp)
                            Button(
                                onClick = {},
                                colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                            ) { Text("Qayta urinish") }
                        }
                    }
                } else {
                    // ── Body ─────────────────────────────────────────────────
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        // ── LEFT: Gallery ─────────────────────────────────────
                        Column(
                            Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 24.dp, top = 20.dp, bottom = 20.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Asosiy katta rasm
                            val mainBitmap = imageBitmaps[imageUrls.getOrNull(selectedIdx)]
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MexaWarehouseColors.backgroundPage)
                                    .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (mainBitmap != null) {
                                    Image(
                                        bitmap           = mainBitmap,
                                        contentDescription = product.name,
                                        contentScale     = ContentScale.Fit,
                                        modifier         = Modifier.fillMaxSize().padding(8.dp),
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint     = MexaWarehouseColors.outlineVariant,
                                        modifier = Modifier.size(64.dp),
                                    )
                                }
                            }

                            // Thumbnaillar qatori
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                imageUrls.forEachIndexed { idx, url ->
                                    val bm       = imageBitmaps[url]
                                    val selected = idx == selectedIdx
                                    Box(
                                        Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MexaWarehouseColors.primary
                                                        else MexaWarehouseColors.borderSubtle,
                                                shape = RoundedCornerShape(8.dp),
                                            )
                                            .background(MexaWarehouseColors.backgroundPage)
                                            .clickable { selectedIdx = idx },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (bm != null) {
                                            Image(
                                                bitmap           = bm,
                                                contentDescription = null,
                                                contentScale     = ContentScale.Crop,
                                                modifier         = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                modifier    = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color       = MexaWarehouseColors.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Divider
                        HorizontalDivider(
                            modifier  = Modifier.fillMaxHeight().width(1.dp),
                            color     = MexaWarehouseColors.borderSubtle,
                            thickness = 1.dp,
                        )

                        // ── RIGHT: Ma'lumotlar ────────────────────────────────
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {

                            // ── ASOSIY MA'LUMOTLAR ────────────────────────────
                            ViewSectionLabel("ASOSIY MA'LUMOTLAR")

                            // 2 ustunli grid
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    ViewInfoField("Barcode", barcode, Modifier.weight(1f), icon = true)
                                    ViewInfoField("Kategoriya", categoryName.ifBlank { "—" }, Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    ViewInfoField("Brend", brandName.ifBlank { "—" }, Modifier.weight(1f), brandIcon = true)
                                    ViewInfoField("O'lchov birligi", unit, Modifier.weight(1f))
                                }
                            }

                            if (siblingColors.size > 1) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Mavjud ranglar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MexaWarehouseColors.textMuted,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        siblingColors.forEach { sib ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (sib.isCurrent) MexaWarehouseColors.tableHeaderBg else MexaWarehouseColors.surfaceLowest,
                                                border = BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                                            ) {
                                                Row(
                                                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        sib.color.ifBlank { "(rangsiz)" },
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MexaWarehouseColors.textPrimary,
                                                    )
                                                    Text(
                                                        sib.barcode,
                                                        fontSize = 11.sp,
                                                        color = MexaWarehouseColors.textMuted,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)

                            // ── NARX BELGILASH ────────────────────────────────
                            ViewSectionLabel("NARX BELGILASH")

                            if (discountPercent > 0 && currentPrice.isNotBlank()) {
                                val orig = currentPrice.replace(" ", "").toBigDecimalOrNull()?.let {
                                    it.multiply(java.math.BigDecimal.valueOf(100))
                                        .divide(java.math.BigDecimal.valueOf((100 - discountPercent).toLong()), 0, java.math.RoundingMode.HALF_UP)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF0F0),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFDC2626)).padding(horizontal = 8.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("−$discountPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Column {
                                            Text("Joriy narx: $currentPrice so'm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB91C1C))
                                            Text(
                                                "Asl narx: ${fmtMoney(orig?.toLong() ?: 0L)} so'm — chegirma muddati o'tgach avtomatik qaytadi",
                                                fontSize = 11.sp,
                                                color = Color(0xFF9F1239),
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            if (tiers.isEmpty()) {
                                Text("Narx belgilanmagan", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tiers.forEachIndexed { idx, tier ->
                                        val isFirst   = idx == 0
                                        val rangeText = if (tier.maxQty != null)
                                            "${tier.minQty}-${tier.maxQty} $unit"
                                        else
                                            "${tier.minQty}+ $unit"
                                        val typeLabel = when (tier.priceType) {
                                            "RETAIL"    -> "Chakana"
                                            "WHOLESALE" -> "Ulgurji"
                                            "PURCHASE"  -> "Kirim"
                                            else        -> tier.priceType
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isFirst) MexaWarehouseColors.primary.copy(alpha = 0.06f)
                                                    else MexaWarehouseColors.backgroundPage,
                                            border = BorderStroke(
                                                width = if (isFirst) 1.dp else 1.dp,
                                                color = if (isFirst) MexaWarehouseColors.primary.copy(alpha = 0.3f)
                                                        else MexaWarehouseColors.borderSubtle,
                                            ),
                                        ) {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                // T1, T2 badge
                                                Box(
                                                    Modifier
                                                        .size(26.dp)
                                                        .background(
                                                            if (isFirst) MexaWarehouseColors.primary
                                                            else MexaWarehouseColors.textMuted.copy(alpha = 0.15f),
                                                            RoundedCornerShape(6.dp),
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        "T${idx + 1}",
                                                        fontSize   = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color      = if (isFirst) Color.White
                                                                     else MexaWarehouseColors.textMuted,
                                                    )
                                                }
                                                // Tier nomi + range
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        "Tier ${idx + 1}  ·  $typeLabel",
                                                        fontSize   = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color      = if (isFirst) MexaWarehouseColors.primary
                                                                     else MexaWarehouseColors.textPrimary,
                                                    )
                                                    Text(
                                                        rangeText,
                                                        fontSize = 11.sp,
                                                        color    = MexaWarehouseColors.textMuted,
                                                    )
                                                }
                                                // Narx
                                                Text(
                                                    "${tier.price} so'm",
                                                    fontSize   = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color      = if (isFirst) MexaWarehouseColors.primary
                                                                 else MexaWarehouseColors.textPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ── QO'SHIMCHA MA'LUMOTLAR ────────────────────────
                            val hasExtra = minStock > 0 || leadTimeDays > 0 || weight > 0 ||
                                           dimL > 0 || dimW > 0 || dimH > 0 ||
                                           packageType.isNotBlank() || fragile

                            if (hasExtra) {
                                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                ViewSectionLabel("QO'SHIMCHA MA'LUMOTLAR")

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (minStock > 0)
                                            ViewInfoField("Minimal qoldiq", "$minStock $unit", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                        if (leadTimeDays > 0)
                                            ViewInfoField("Yetkazib berish", "$leadTimeDays kun", Modifier.weight(1f))
                                        else Spacer(Modifier.weight(1f))
                                    }
                                    if (weight > 0 || dimL > 0) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            if (weight > 0)
                                                ViewInfoField("Og'irligi", "$weight kg", Modifier.weight(1f))
                                            else Spacer(Modifier.weight(1f))
                                            if (dimL > 0 || dimW > 0 || dimH > 0)
                                                ViewInfoField(
                                                    "O'lchamlari",
                                                    "${dimL}×${dimW}×${dimH} sm",
                                                    Modifier.weight(1f),
                                                )
                                            else Spacer(Modifier.weight(1f))
                                        }
                                    }
                                    if (packageType.isNotBlank() || fragile) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            if (packageType.isNotBlank())
                                                ViewInfoField("Qadoq turi", packageType, Modifier.weight(1f))
                                            else Spacer(Modifier.weight(1f))
                                            if (fragile) {
                                                Column(Modifier.weight(1f)) {
                                                    Text("Ehtiyotkorlik", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                                                    Spacer(Modifier.height(3.dp))
                                                    Box(
                                                        Modifier
                                                            .background(
                                                                MexaWarehouseColors.amberBadgeBg,
                                                                RoundedCornerShape(6.dp),
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                                    ) {
                                                        Text(
                                                            "Tez sinuvchan",
                                                            fontSize   = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color      = MexaWarehouseColors.amberBadgeFg,
                                                        )
                                                    }
                                                }
                                            } else Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // ── TEGLAR ────────────────────────────────────────
                            if (tags.isNotEmpty()) {
                                HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                                ViewSectionLabel("TEGLAR")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement   = Arrangement.spacedBy(6.dp),
                                ) {
                                    tags.forEach { tag -> ViewTagBadge(tag) }
                                }
                            }

                            // ── REYTING VA SHARHLAR ───────────────────────────
                            HorizontalDivider(color = MexaWarehouseColors.borderSubtle)
                            ViewSectionLabel("REYTING VA SHARHLAR")

                            if (reviewsLoading) {
                                Box(Modifier.fillMaxWidth().height(60.dp), Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MexaWarehouseColors.primary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            } else {
                                // Reyting statistikasi
                                RatingStatsBlock(
                                    avgRating   = avgRating,
                                    reviewCount = reviewCount,
                                    dist        = ratingDist,
                                )

                                // So'nggi sharhlar
                                if (reviews.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "So'nggi sharhlar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MexaWarehouseColors.textMuted,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    reviews.forEach { review ->
                                        ReviewRow(review = review)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                } else if (reviewCount == 0L) {
                                    Text(
                                        "Hali sharh yo'q",
                                        fontSize = 13.sp,
                                        color = MexaWarehouseColors.textMuted,
                                    )
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
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape   = RoundedCornerShape(8.dp),
                        border  = BorderStroke(1.dp, MexaWarehouseColors.outlineVariant),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textPrimary),
                    ) {
                        Text("Yopish", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    if (onEdit != null) {
                        Button(
                            onClick = onEdit,
                            colors  = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.primary),
                            shape   = RoundedCornerShape(8.dp),
                        ) {
                            Text("Tahrirlash", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun ViewSectionLabel(text: String) {
    Text(
        text,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MexaWarehouseColors.textMuted,
        letterSpacing = 0.8f.sp,
    )
}

@Composable
private fun ViewInfoField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: Boolean      = false,
    brandIcon: Boolean = false,
) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(bottom = 3.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icon) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint     = MexaWarehouseColors.textMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (brandIcon) {
                Box(
                    Modifier
                        .size(18.dp)
                        .background(MexaWarehouseColors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        value.firstOrNull()?.uppercase() ?: "B",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MexaWarehouseColors.primary,
                    )
                }
            }
            Text(
                value,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = MexaWarehouseColors.textPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Review helpers ───────────────────────────────────────────────────────────

data class ReviewItem(
    val id: String,
    val username: String,
    val rating: Int,
    val comment: String,
    val createdAt: String,
)

@Composable
private fun RatingStatsBlock(avgRating: Double, reviewCount: Long, dist: Map<Int, Long>) {
    if (reviewCount == 0L) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(10.dp))
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Katta ball
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "%.1f".format(avgRating),
                fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                color = MexaWarehouseColors.textPrimary,
            )
            StarRow(rating = avgRating, size = 14.dp)
            Spacer(Modifier.height(2.dp))
            Text("$reviewCount ta sharh", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
        }

        // Taqsimot
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (5 downTo 1).forEach { star ->
                val cnt   = dist[star] ?: 0L
                val frac  = if (reviewCount > 0) cnt.toFloat() / reviewCount else 0f
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$star ★", fontSize = 11.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.width(28.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(MexaWarehouseColors.borderSubtle, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac)
                                .background(Color(0xFFF59E0B), RoundedCornerShape(3.dp))
                        )
                    }
                    Text("$cnt", fontSize = 10.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.width(22.dp))
                }
            }
        }
    }
}

@Composable
private fun StarRow(rating: Double, size: androidx.compose.ui.unit.Dp) {
    Row {
        (1..5).forEach { i ->
            Text(
                if (i <= rating.toInt()) "★" else "☆",
                fontSize = (size.value).sp,
                color    = if (i <= rating) Color(0xFFF59E0B) else MexaWarehouseColors.borderSubtle,
            )
        }
    }
}

@Composable
private fun ReviewRow(review: ReviewItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Avatar
        Box(
            Modifier
                .size(32.dp)
                .background(MexaWarehouseColors.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                review.username.firstOrNull()?.uppercase() ?: "?",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MexaWarehouseColors.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(review.username, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StarRow(rating = review.rating.toDouble(), size = 12.dp)
                    Text(review.createdAt, fontSize = 10.sp, color = MexaWarehouseColors.textMuted)
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    review.comment, fontSize = 12.sp,
                    color = MexaWarehouseColors.textMuted, maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ViewTagBadge(tag: String) {
    val (bg, fg) = when (tag.lowercase()) {
        "yangi"     -> MexaWarehouseColors.blueBadgeBg     to MexaWarehouseColors.blueBadgeFg
        "ommabop"   -> Color(0xFFEDE9FE)                   to Color(0xFF6D28D9)
        "luks"      -> MexaWarehouseColors.amberBadgeBg    to MexaWarehouseColors.amberBadgeFg
        "kam qoldi" -> MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
        "chegirma"  -> MexaWarehouseColors.greenBadgeBg    to MexaWarehouseColors.greenBadgeFg
        else        -> MexaWarehouseColors.tableHeaderBg   to MexaWarehouseColors.textMuted
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

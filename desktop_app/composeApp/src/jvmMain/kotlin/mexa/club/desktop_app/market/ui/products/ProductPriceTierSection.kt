package mexa.club.desktop_app.market.ui.products

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ─── Model ────────────────────────────────────────────────────────────────────

data class PriceTierItem(
    val id: String,
    val minQty: Int,
    val maxQty: Int?,
    val price: BigDecimal,
    val currency: String,
    val priceType: String,
    val rangeLabel: String,
)

private fun fmtPrice(v: BigDecimal): String =
    NumberFormat.getNumberInstance(Locale("uz")).apply { maximumFractionDigits = 0 }.format(v)

private val PrimaryColor   = Color(0xFF5B52F6)
private val PrimaryLight   = Color(0xFFEEEDFD)
private val DangerColor    = Color(0xFFEF4444)
private val TierColors     = listOf(
    Color(0xFF5B52F6), Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF6366F1), Color(0xFF14B8A6),
)

// ─── Main composable ──────────────────────────────────────────────────────────

@Composable
fun ProductPriceTierSection(
    productId: String = "",
    enabled: Boolean = true,
    externalTiers: List<PriceTierItem> = emptyList(),
    onLocalTiersChanged: ((List<PriceTierItem>) -> Unit)? = null,
    basePriceForDisplay: BigDecimal? = null,
) {
    val scope = rememberCoroutineScope()

    var tiers    by remember { mutableStateOf<List<PriceTierItem>>(externalTiers) }
    var loading  by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf<String?>(null) }

    // Form
    var editingId   by remember { mutableStateOf<String?>(null) }
    var showForm    by remember { mutableStateOf(false) }
    var fMinQty     by remember { mutableStateOf("") }
    var fMaxQty     by remember { mutableStateOf("") }
    var fPrice      by remember { mutableStateOf("") }
    var fPriceType  by remember { mutableStateOf("RETAIL") }
    var formError   by remember { mutableStateOf<String?>(null) }
    var saving      by remember { mutableStateOf(false) }

    // Price checker (bottom bar)
    var checkQty     by remember { mutableStateOf("") }
    var checkedPrice by remember { mutableStateOf<BigDecimal?>(null) }
    var checking     by remember { mutableStateOf(false) }

    // ── API helpers ───────────────────────────────────────────────────────────
    fun loadTiers() {
        if (productId.isBlank()) return
        scope.launch {
            loading = true; apiError = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/products/$productId/price-tiers")
                    val arr  = (ApiClient.parseJsonObject(text)?.get("data") as? JsonArray) ?: JsonArray(emptyList())
                    arr.mapNotNull { el ->
                        val t = el as? JsonObject ?: return@mapNotNull null
                        val id = t.stringField("id").ifEmpty { return@mapNotNull null }
                        PriceTierItem(
                            id         = id,
                            minQty     = (t["minQty"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                            maxQty     = (t["maxQty"] as? JsonPrimitive)?.content?.toIntOrNull(),
                            price      = (t["price"]  as? JsonPrimitive)?.content?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            currency   = t.stringField("currency").ifEmpty { "UZS" },
                            priceType  = t.stringField("priceType").ifEmpty { "RETAIL" },
                            rangeLabel = t.stringField("rangeLabel"),
                        )
                    }
                }
            }.onSuccess { tiers = it }.onFailure { apiError = it.message }
            loading = false
        }
    }

    LaunchedEffect(productId) { loadTiers() }

    fun resetForm() {
        editingId = null; fMinQty = ""; fMaxQty = ""; fPrice = ""; fPriceType = "RETAIL"
        formError = null; showForm = false
    }

    fun startEdit(tier: PriceTierItem) {
        editingId  = tier.id
        fMinQty    = tier.minQty.toString()
        fMaxQty    = tier.maxQty?.toString() ?: ""
        fPrice     = tier.price.toPlainString()
        fPriceType = tier.priceType
        formError  = null
        showForm   = true
    }

    fun saveTier() {
        val minQ = fMinQty.trim().toIntOrNull()
        val maxQ = fMaxQty.trim().toIntOrNull()
        val pr   = fPrice.replace(",", ".").trim().toBigDecimalOrNull()
        println("saveTier debug: fMinQty='$fMinQty' -> minQ=$minQ, fMaxQty='$fMaxQty' -> maxQ=$maxQ, fPrice='$fPrice' -> pr=$pr, tiers=${tiers.size}")

        if (minQ == null || minQ < 1) { formError = "Minimal miqdor 1 dan kam bo'lmasligi kerak"; return }
        if (fMaxQty.isNotBlank() && maxQ == null) { formError = "Maksimal miqdor noto'g'ri"; return }
        if (fMaxQty.isNotBlank() && maxQ != null && maxQ < minQ) { formError = "Max miqdor min dan kichik bo'lishi mumkin emas"; return }
        if (pr == null || pr <= BigDecimal.ZERO) { formError = "Narx noto'g'ri"; return }

        val overlapping = tiers.filter { t ->
            t.id != editingId &&
            (fMaxQty.isBlank() || maxQ!! >= t.minQty) &&
            (t.maxQty == null || t.maxQty >= minQ)
        }
        if (overlapping.isNotEmpty()) { formError = "Bu oraliq mavjud bosqich bilan kesishyapti"; return }

        formError = null

        if (productId.isBlank()) {
            val newTier = PriceTierItem(
                id         = editingId ?: java.util.UUID.randomUUID().toString(),
                minQty     = minQ,
                maxQty     = maxQ,
                price      = pr,
                currency   = "UZS",
                priceType  = fPriceType,
                rangeLabel = if (fMaxQty.isBlank()) "$minQ+" else "$minQ – $maxQ",
            )
            tiers = if (editingId != null) tiers.map { if (it.id == editingId) newTier else it }
                    else (tiers + newTier).sortedBy { it.minQty }
            onLocalTiersChanged?.invoke(tiers)
            resetForm()
            return
        }

        scope.launch {
            saving = true
            val body = buildString {
                append("{\"minQty\":$minQ,")
                if (fMaxQty.isNotBlank()) append("\"maxQty\":$maxQ,")
                append("\"price\":$pr,")
                append("\"priceType\":\"$fPriceType\",")
                append("\"currency\":\"UZS\"}")
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    if (editingId != null) ApiClient.put("/api/products/$productId/price-tiers/$editingId", body)
                    else ApiClient.post("/api/products/$productId/price-tiers", body)
                }
            }.onSuccess { loadTiers(); resetForm() }
             .onFailure { formError = it.message ?: "Xatolik" }
            saving = false
        }
    }

    fun deleteTier(id: String) {
        if (productId.isBlank()) {
            tiers = tiers.filter { it.id != id }
            onLocalTiersChanged?.invoke(tiers)
            return
        }
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { ApiClient.delete("/api/products/$productId/price-tiers/$id") } }
                .onSuccess { loadTiers() }
                .onFailure { apiError = it.message }
        }
    }

    fun checkPrice() {
        val qty = checkQty.toIntOrNull() ?: return
        if (productId.isBlank()) {
            val match = tiers.firstOrNull { t -> t.minQty <= qty && (t.maxQty == null || t.maxQty >= qty) }
            checkedPrice = match?.price
            return
        }
        scope.launch {
            checking = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ApiClient.get("/api/products/$productId/price-tiers/resolve?qty=$qty")
                    val data = (ApiClient.parseJsonObject(text)?.get("data") as? JsonObject)
                    val found = (data?.get("found") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
                    if (found) (data?.get("price") as? JsonPrimitive)?.content?.toBigDecimalOrNull() else null
                }
            }.onSuccess { checkedPrice = it }
            checking = false
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // API error
        if (apiError != null) {
            Text(apiError!!, fontSize = 12.sp, color = DangerColor)
        }

        // ── Ikki kolonna: chap — tier ro'yxat, o'ng — forma ───────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {

            // ── CHAP: Tier ro'yxati ────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PrimaryColor)
                    }
                } else if (tiers.isEmpty()) {
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = MexaWarehouseColors.backgroundPage,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 20.dp)) {
                            Text(
                                "Narx bosqichlari yo'q.\nQuyidagi tugma orqali qo'shing.",
                                fontSize = 12.sp, color = MexaWarehouseColors.textMuted,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                } else {
                    tiers.forEachIndexed { idx, tier ->
                        TierCard(
                            tier      = tier,
                            index     = idx + 1,
                            color     = TierColors.getOrElse(idx) { PrimaryColor },
                            isEditing = editingId == tier.id && showForm,
                            enabled   = enabled,
                            onEdit    = { startEdit(tier) },
                            onDelete  = { deleteTier(tier.id) },
                        )
                    }
                }

                // Add tier tugmasi — faqat forma yopiq bo'lganda
                if (enabled && !showForm) {
                    OutlinedButton(
                        onClick  = { resetForm(); showForm = true },
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor),
                        border   = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryColor),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bosqich qo'shish", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── O'NG: Forma ────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1.1f)) {
                if (showForm) {
                    TierFormPanel(
                        editingId         = editingId,
                        fMinQty           = fMinQty,
                        fMaxQty           = fMaxQty,
                        fPrice            = fPrice,
                        fPriceType        = fPriceType,
                        formError         = formError,
                        saving            = saving,
                        onMinQtyChange    = { fMinQty = it.filter(Char::isDigit) },
                        onMaxQtyChange    = { fMaxQty = it.filter(Char::isDigit) },
                        onPriceChange     = { fPrice  = it.filter { c -> c.isDigit() || c == '.' } },
                        onPriceTypeChange = { fPriceType = it },
                        onSave            = { saveTier() },
                        onCancel          = { resetForm() },
                    )
                } else {
                    GlobalStrategyCard(tierCount = tiers.size)
                }
            }
        }

        // ── Narx tekshirish paneli ─────────────────────────────────────────
        if (tiers.isNotEmpty() && !showForm) {
            PriceCheckerBar(
                checkQty     = checkQty,
                checkedPrice = checkedPrice,
                checking     = checking,
                onQtyChange  = { checkQty = it.filter(Char::isDigit); checkedPrice = null },
                onCheck      = { checkPrice() },
            )
        }
    }
}

// ─── Empty placeholder ─────────────────────────────────────────────────────────

@Composable
private fun EmptyTiersPlaceholder() {
    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = MexaWarehouseColors.backgroundPage,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text(
                "Narx bosqichlari yo'q.\nQuyidagi tugma orqali qo'shing.",
                fontSize = 13.sp,
                color = MexaWarehouseColors.textMuted,
                lineHeight = 20.sp,
            )
        }
    }
}

// ─── Tier card ─────────────────────────────────────────────────────────────────

@Composable
private fun TierCard(
    tier: PriceTierItem,
    index: Int,
    color: Color,
    isEditing: Boolean,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = if (isEditing) color.copy(alpha = 0.06f) else MexaWarehouseColors.surfaceLowest,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isEditing) 2.dp else 1.dp,
            color = if (isEditing) color else MexaWarehouseColors.borderSubtle,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Badge
                Box(
                    modifier = Modifier
                        .background(color, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("Tier $index", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Range
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (tier.maxQty != null) "${tier.minQty} – ${tier.maxQty}" else "${tier.minQty}+",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MexaWarehouseColors.textPrimary,
                        )
                        if (tier.maxQty == null) {
                            Icon(Icons.Filled.AllInclusive, contentDescription = null,
                                tint = color, modifier = Modifier.size(13.dp))
                        }
                        Text("dona", fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                    }
                    Text(
                        "${fmtPrice(tier.price)} ${tier.currency} / dona",
                        fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Tahrirlash",
                            tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "O'chirish",
                            tint = DangerColor, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ─── Form panel ─────────────────────────────────────────────────────────────────

@Composable
private fun TierFormPanel(
    editingId: String?,
    fMinQty: String, fMaxQty: String, fPrice: String, fPriceType: String,
    formError: String?,
    saving: Boolean,
    onMinQtyChange: (String) -> Unit,
    onMaxQtyChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPriceTypeChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = MexaWarehouseColors.backgroundPage,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Title + info icon
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (editingId != null) "Bosqichni tahrirlash" else "Yangi bosqich qo'shish",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MexaWarehouseColors.textPrimary,
                )
                Icon(Icons.Filled.Info, contentDescription = null,
                    tint = MexaWarehouseColors.textMuted, modifier = Modifier.size(18.dp))
            }

            // Min + Max qty
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Minimal miqdor", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = MexaWarehouseColors.textPrimary)
                    OutlinedTextField(
                        value = fMinQty, onValueChange = onMinQtyChange,
                        placeholder = { Text("1", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                        singleLine = true, enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = formFieldColors(),
                        textStyle = TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Maksimal miqdor", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = MexaWarehouseColors.textPrimary)
                    OutlinedTextField(
                        value = fMaxQty, onValueChange = onMaxQtyChange,
                        placeholder = { Text("bo'sh = cheksiz", fontSize = 12.sp, color = MexaWarehouseColors.textMuted) },
                        singleLine = true, enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = formFieldColors(),
                        textStyle = TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }

            // Price
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Narx (1 dona, UZS)", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = MexaWarehouseColors.textPrimary)
                OutlinedTextField(
                    value = fPrice, onValueChange = onPriceChange,
                    placeholder = { Text("45000", fontSize = 13.sp, color = MexaWarehouseColors.textMuted) },
                    singleLine = true, enabled = !saving,
                    trailingIcon = {
                        Text("UZS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = MexaWarehouseColors.textMuted,
                            modifier = Modifier.padding(end = 12.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = formFieldColors(),
                    textStyle = TextStyle(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).background(PrimaryColor, CircleShape))
                    Text("Xaridor shu oraliqda buyurtma berganda ushbu narx qo'llaniladi",
                        fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                }
            }

            // Preview calculation
            val minQ = fMinQty.toIntOrNull()
            val pr   = fPrice.toBigDecimalOrNull()
            if (minQ != null && pr != null && pr > BigDecimal.ZERO) {
                val exQty = if (minQ > 0) minQ else 1
                val total = pr.multiply(BigDecimal.valueOf(exQty.toLong()))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryLight,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.TrendingDown, contentDescription = null,
                            tint = PrimaryColor, modifier = Modifier.size(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("HISOB-KITOB",
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = PrimaryColor, letterSpacing = 0.8.sp)
                            Text("$exQty dona × ${fmtPrice(pr)} = ${fmtPrice(total)} UZS",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = PrimaryColor)
                        }
                    }
                }
            }

            // Error
            if (formError != null) {
                Text(formError, fontSize = 12.sp, color = DangerColor)
            }

            // Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = onSave,
                    enabled  = !saving,
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(42.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (editingId != null) "Saqlash" else "Saqlash",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick  = onCancel,
                    enabled  = !saving,
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MexaWarehouseColors.textMuted),
                    modifier = Modifier.height(42.dp),
                ) {
                    Text("Bekor", fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── Global strategy card ──────────────────────────────────────────────────────

@Composable
private fun GlobalStrategyCard(tierCount: Int) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = PrimaryColor.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(36.dp).background(PrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.TrendingDown, contentDescription = null,
                        tint = PrimaryColor, modifier = Modifier.size(20.dp))
                }
                Text("Narx bosqichlari",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MexaWarehouseColors.textPrimary)
            }
            Text(
                "Miqdorga qarab turli narx belgilash imkoniyati. Chap tomondagi tugma orqali bosqich qo'shing.",
                fontSize = 12.sp, color = MexaWarehouseColors.textMuted, lineHeight = 18.sp,
            )
            if (tierCount > 0) {
                HorizontalDivider(color = PrimaryColor.copy(alpha = 0.15f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(PrimaryColor, CircleShape))
                    Text("$tierCount ta narx bosqichi belgilangan",
                        fontSize = 12.sp, color = PrimaryColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─── Price checker bottom bar ──────────────────────────────────────────────────

@Composable
private fun PriceCheckerBar(
    checkQty: String,
    checkedPrice: BigDecimal?,
    checking: Boolean,
    onQtyChange: (String) -> Unit,
    onCheck: () -> Unit,
) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = MexaWarehouseColors.surfaceLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Miqdor uchun narxni tekshirish:",
                fontSize = 12.sp, color = MexaWarehouseColors.textMuted)

            OutlinedTextField(
                value = checkQty, onValueChange = onQtyChange,
                placeholder = { Text("25", fontSize = 12.sp, color = MexaWarehouseColors.textMuted) },
                singleLine = true,
                modifier = Modifier.width(90.dp),
                shape = RoundedCornerShape(6.dp),
                colors = formFieldColors(),
                textStyle = TextStyle(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Text("dona", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)

            if (checkedPrice != null) {
                Icon(Icons.Filled.TrendingDown, contentDescription = null,
                    tint = PrimaryColor, modifier = Modifier.size(16.dp))
                Text("${fmtPrice(checkedPrice)} UZS / dona",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryColor)
            } else if (checkQty.isNotBlank() && !checking) {
                Text("Narx topilmadi", fontSize = 12.sp, color = DangerColor)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick  = onCheck,
                enabled  = checkQty.isNotBlank() && !checking,
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
            ) {
                if (checking) CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                else {
                    Icon(Icons.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tekshirish", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor    = MexaWarehouseColors.outlineVariant,
    focusedBorderColor      = PrimaryColor,
    unfocusedContainerColor = Color.White,
    focusedContainerColor   = Color.White,
)

package mexa.club.desktop_app.market.ui.delivery
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.GatewayRealtimeHub
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.math.BigDecimal
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val PRICING = "/api/admin/pricing"

enum class DeliveryPricingTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Umumiy", Icons.Default.Info),
    RULES("Narx qoidalari", Icons.Default.Percent),
    FREE("Bepul yetkazish", Icons.Default.Savings),
    INCENTIVES("Buyurtma rag'batlari", Icons.Default.LocalShipping),
    ZONES("Zonalar", Icons.Default.Map),
    SURCHARGES("Qo'shimcha haqlar", Icons.Default.Add),
    TYPES("Yetkazish turlari", Icons.Default.LocalShipping),
    SIMULATOR("Simulyator", Icons.Default.Calculate),
    ANALYTICS("Analitika", Icons.Default.BarChart),
    VERSIONS("Versiyalar", Icons.Default.History),
    AUDIT("Audit log", Icons.Default.FactCheck),
}

private data class DeleteTarget(val path: String, val name: String)

private suspend fun fetchList(path: String): List<JsonObject> =
    dataArray(ApiClient.get(path))

private suspend fun fetchObj(path: String): JsonObject? =
    dataObject(ApiClient.get(path))

@Composable
fun DeliveryPricingScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(DeliveryPricingTab.OVERVIEW) }

    var rules by remember { mutableStateOf<List<PricingRuleRow>>(emptyList()) }
    var freeRules by remember { mutableStateOf<List<FreeRuleRow>>(emptyList()) }
    var incentives by remember { mutableStateOf<List<IncentiveRow>>(emptyList()) }
    var surcharges by remember { mutableStateOf<List<SurchargeRow>>(emptyList()) }
    var types by remember { mutableStateOf<List<TypeRow>>(emptyList()) }
    var zones by remember { mutableStateOf<List<ZoneRow>>(emptyList()) }
    var analytics by remember { mutableStateOf<AnalyticsData?>(null) }
    var settings by remember { mutableStateOf<JsonObject?>(null) }
    var versions by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var auditLog by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var onlyActive by remember { mutableStateOf(false) }

    var editRule by remember { mutableStateOf<PricingRuleRow?>(null) }
    var newRule by remember { mutableStateOf(false) }
    var editFree by remember { mutableStateOf<FreeRuleRow?>(null) }
    var newFree by remember { mutableStateOf(false) }
    var editIncentive by remember { mutableStateOf<IncentiveRow?>(null) }
    var newIncentive by remember { mutableStateOf(false) }
    var editSurcharge by remember { mutableStateOf<SurchargeRow?>(null) }
    var newSurcharge by remember { mutableStateOf(false) }
    var editType by remember { mutableStateOf<TypeRow?>(null) }
    var newType by remember { mutableStateOf(false) }
    var editZone by remember { mutableStateOf<ZoneRow?>(null) }
    var newZone by remember { mutableStateOf(false) }
    var newVersion by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }
    var preview by remember { mutableStateOf<JsonObject?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val r = fetchList("$PRICING/rules")
                    val f = fetchList("$PRICING/free-delivery")
                    val i = fetchList("$PRICING/incentives")
                    val s = fetchList("$PRICING/surcharges")
                    val t = fetchList("$PRICING/types")
                    val z = fetchList("/api/admin/delivery-zones")
                    val a = fetchObj("$PRICING/analytics?days=30")
                    val st = fetchObj("$PRICING/settings")
                    val v = fetchList("$PRICING/versions")
                    val au = fetchList("$PRICING/audit")
                    PricingBundle(r.map { it.toPricingRule() }, f.map { it.toFreeRule() },
                        i.map { it.toIncentive() }, s.map { it.toSurcharge() },
                        t.map { it.toTypeRow() }, z.map { it.toZoneRow() }, a, st, v, au)
                }
            }
            result.onSuccess { q ->
                rules = q.rules; freeRules = q.free; incentives = q.inc
                surcharges = q.sur; types = q.types; zones = q.zones
                analytics = q.analytics?.toAnalytics(); settings = q.settings
                versions = q.versions; auditLog = q.audit
            }.onFailure { e -> error = e.message ?: "Xatolik" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        reload()
        // Realtime mutatsiyalar (pricing/types/surcharges/zones) → avtomatik yangilash (T-H7)
        GatewayRealtimeHub.dashboardRefresh.drop(1).collect { reload() }
    }

    // ─── Dialoglar ─────────────────────────────────────────────────────────

    val zoneOptions = remember(zones) { zones.map { z -> "${z.region} — ${z.district}" to z.id } }
    val typeOptions = remember(types) { types.map { it.name } }

    if (newRule || editRule != null) {
        PricingEditorDialog(
            title = if (newRule) "Narx qoidasi yaratish" else "Narx qoidasini tahrirlash",
            subtitle = "Final Fee = Base + (Distance × km) + (Weight × kg), keyin min/max, surcharges va bepul yetkazish",
            initialFields = ruleFields(editRule, zoneOptions, typeOptions),
            saveRequest = { body ->
                if (newRule) ApiClient.post("$PRICING/rules", body)
                else ApiClient.put("$PRICING/rules/${editRule!!.id}", body)
            },
            onDismiss = { newRule = false; editRule = null },
            onSaved = { newRule = false; editRule = null; reload() },
        )
    }
    if (newFree || editFree != null) {
        PricingEditorDialog(
            title = if (newFree) "Bepul yetkazish qoidasi" else "Qoidani tahrirlash",
            subtitle = "Buyurtma summasi chegaragacha yetganda delivery bepul yoki chegirmali bo'ladi",
            initialFields = freeFields(editFree, zoneOptions, typeOptions),
            saveRequest = { body ->
                if (newFree) ApiClient.post("$PRICING/free-delivery", body)
                else ApiClient.put("$PRICING/free-delivery/${editFree!!.id}", body)
            },
            onDismiss = { newFree = false; editFree = null },
            onSaved = { newFree = false; editFree = null; reload() },
        )
    }
    if (newIncentive || editIncentive != null) {
        PricingEditorDialog(
            title = if (newIncentive) "Buyurtma rag'bati qoidasi" else "Rag'batni tahrirlash",
            subtitle = "Savat chegarasi: foydalanuvchiga qo'shish uchun rag'bat xabari",
            initialFields = incentiveFields(editIncentive),
            saveRequest = { body ->
                if (newIncentive) ApiClient.post("$PRICING/incentives", body)
                else ApiClient.put("$PRICING/incentives/${editIncentive!!.id}", body)
            },
            onDismiss = { newIncentive = false; editIncentive = null },
            onSaved = { newIncentive = false; editIncentive = null; reload() },
        )
    }
    if (newSurcharge || editSurcharge != null) {
        PricingEditorDialog(
            title = if (newSurcharge) "Qo'shimcha haq qoidasi" else "Qo'shimcha haqni tahrirlash",
            subtitle = "Peak hour, kecha, og'ir buyurtma va boshqa holatlar uchun qo'shimcha to'lov",
            initialFields = surchargeFields(editSurcharge, typeOptions),
            saveRequest = { body ->
                if (newSurcharge) ApiClient.post("$PRICING/surcharges", body)
                else ApiClient.put("$PRICING/surcharges/${editSurcharge!!.id}", body)
            },
            onDismiss = { newSurcharge = false; editSurcharge = null },
            onSaved = { newSurcharge = false; editSurcharge = null; reload() },
        )
    }
    if (newType || editType != null) {
        PricingEditorDialog(
            title = if (newType) "Yetkazish turi yaratish" else "Yetkazish turini tahrirlash",
            subtitle = "Express, Standart, Kutilgan vaqt — qo'shimcha haq bilan",
            initialFields = typeFields(editType),
            saveRequest = { body ->
                if (newType) ApiClient.post("$PRICING/types", body)
                else ApiClient.put("$PRICING/types/${editType!!.id}", body)
            },
            onDismiss = { newType = false; editType = null },
            onSaved = { newType = false; editType = null; reload() },
        )
    }
    if (editZone != null) {
        val zone = editZone!!
        PricingEditorDialog(
            title = "Zonani tahrirlash",
            subtitle = "${zone.region} — ${zone.district}",
            initialFields = zoneFields(zone),
            saveRequest = { body -> ApiClient.put("/api/admin/delivery-zones/${zone.id}", body) },
            onDismiss = { editZone = null },
            onSaved = { editZone = null; reload() },
        )
    }
    if (newZone) {
        PricingEditorDialog(
            title = "Zona yaratish",
            subtitle = "Hudud darajasidagi bazaviy tarif va min/max chegaralar",
            initialFields = zoneFields(
                ZoneRow(
                    id = "", region = "", district = "",
                    fee = BigDecimal.ZERO, perKmFee = BigDecimal.ZERO, perKgFee = BigDecimal.ZERO,
                    freeAbove = null, minFee = null, maxFee = null, priority = 100, estimatedDays = 1,
                ),
            ),
            saveRequest = { body -> ApiClient.post("/api/admin/delivery-zones", body) },
            onDismiss = { newZone = false },
            onSaved = { newZone = false; reload() },
        )
    }
    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "\"${target.name}\" o'chirilsinmi?",
            message = "O'chirishni qaytarib bo'lmaydi. Tarixdagi yozuvlar saqlanib qoladi.",
            requireReason = true,
            onConfirm = { reason ->
                withContext(Dispatchers.IO) {
                    ApiClient.delete("${target.path}?reason=${URLEncoder.encode(reason, "UTF-8")}")
                }
                deleteTarget = null
                reload()
            },
            onDismiss = { deleteTarget = null },
        )
    }
    if (newVersion) {
        PricingEditorDialog(
            title = "Yangi pricing versiyasi",
            subtitle = "Joriy narx to'plamini yangi versiya sifatida belgilash (avvalgisi ARCHIVED bo'ladi)",
            initialFields = listOf(
                PricingField.Text("O'zgarish sababi", "changeReason", "Narx to'plami yangilandi", required = true),
            ),
            saveRequest = { body -> ApiClient.post("$PRICING/versions", body) },
            onDismiss = { newVersion = false },
            onSaved = { newVersion = false; reload() },
        )
    }

    // ─── UI ────────────────────────────────────────────────────────────────

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Delivery Pricing", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                    Text("Yetkazib berish narxi, bepul chegiralar va savat rag'batlari boshqaruvi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                }
            }
            OutlinedButton(onClick = { reload() }, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text(AppStrings.devicesRefresh(appLanguage()))
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MexaWarehouseColors.surfaceLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DeliveryPricingTab.entries.forEach { t ->
                    val selected = t == tab
                    Row(
                        Modifier
                            .background(
                                if (selected) MexaWarehouseColors.infoBadgeBg else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { tab = t }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            t.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selected) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.textMuted,
                        )
                        Text(
                            t.title,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MexaWarehouseColors.indigoAccent else MexaWarehouseColors.textMuted,
                        )
                    }
                }
            }
        }

        if (tab == DeliveryPricingTab.RULES || tab == DeliveryPricingTab.FREE || tab == DeliveryPricingTab.INCENTIVES ||
            tab == DeliveryPricingTab.SURCHARGES || tab == DeliveryPricingTab.TYPES
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(checked = onlyActive, onCheckedChange = { onlyActive = it })
                Text("Faqat faol qoidalar", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
        }
        error?.let { msg ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MexaWarehouseColors.statusBlockedBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(msg, Modifier.padding(16.dp), fontSize = 13.sp, color = MexaWarehouseColors.statusBlockedFg)
            }
        }

        if (!loading) {
            when (tab) {
                DeliveryPricingTab.OVERVIEW -> OverviewTab(
                    analytics = analytics,
                    settings = settings,
                    onSavedSettings = { reload() },
                )
                DeliveryPricingTab.RULES -> RulesTab(
                    rules = rules.filter { !onlyActive || it.active },
                    onCreate = { newRule = true },
                    onEdit = { editRule = it },
                    onDelete = { r -> deleteTarget = DeleteTarget("$PRICING/rules/${r.id}", r.name) },
                    onPreview = { id ->
                        scope.launch {
                            withContext(Dispatchers.IO) { runCatching { fetchObj("$PRICING/preview?ruleId=$id") } }
                                .onSuccess { preview = it }
                        }
                    },
                    preview = preview,
                )
                DeliveryPricingTab.FREE -> FreeTab(
                    rows = freeRules.filter { !onlyActive || it.active },
                    onCreate = { newFree = true },
                    onEdit = { editFree = it },
                    onDelete = { r -> deleteTarget = DeleteTarget("$PRICING/free-delivery/${r.id}", r.name) },
                )
                DeliveryPricingTab.INCENTIVES -> IncentiveTab(
                    rows = incentives.filter { !onlyActive || it.active },
                    onCreate = { newIncentive = true },
                    onEdit = { editIncentive = it },
                    onDelete = { r -> deleteTarget = DeleteTarget("$PRICING/incentives/${r.id}", r.name) },
                )
                DeliveryPricingTab.ZONES -> ZoneTab(
                    rows = zones,
                    onCreate = { newZone = true },
                    onEdit = { editZone = it },
                    onDelete = { z -> deleteTarget = DeleteTarget("/api/admin/delivery-zones/${z.id}", "${z.region} — ${z.district}") },
                )
                DeliveryPricingTab.SURCHARGES -> SurchargeTab(
                    rows = surcharges.filter { !onlyActive || it.active },
                    onCreate = { newSurcharge = true },
                    onEdit = { editSurcharge = it },
                    onDelete = { s -> deleteTarget = DeleteTarget("$PRICING/surcharges/${s.id}", s.name) },
                )
                DeliveryPricingTab.TYPES -> TypeTab(
                    rows = types.filter { !onlyActive || it.active },
                    onCreate = { newType = true },
                    onEdit = { editType = it },
                    onDelete = { t -> deleteTarget = DeleteTarget("$PRICING/types/${t.id}", t.name) },
                )
                DeliveryPricingTab.SIMULATOR -> SimulatorTab(zones = zones, types = types)
                DeliveryPricingTab.ANALYTICS -> AnalyticsTab(analytics = analytics)
                DeliveryPricingTab.VERSIONS -> VersionsTab(
                    rows = versions,
                    onCreate = { newVersion = true },
                )
                DeliveryPricingTab.AUDIT -> AuditTab(rows = auditLog)
            }
        }
    }
}

private data class PricingBundle(
    val rules: List<PricingRuleRow>,
    val free: List<FreeRuleRow>,
    val inc: List<IncentiveRow>,
    val sur: List<SurchargeRow>,
    val types: List<TypeRow>,
    val zones: List<ZoneRow>,
    val analytics: JsonObject?,
    val settings: JsonObject?,
    val versions: List<JsonObject>,
    val audit: List<JsonObject>,
)

// ─── Umumiy tab ────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(analytics: AnalyticsData?, settings: JsonObject?, onSavedSettings: () -> Unit) {
    var baseCost by remember(settings) { mutableStateOf(settings?.num("baseCost")?.toPlainString() ?: "") }
    var perKm by remember(settings) { mutableStateOf(settings?.num("perKmCost")?.toPlainString() ?: "") }
    var perKg by remember(settings) { mutableStateOf(settings?.num("perKgCost")?.toPlainString() ?: "") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SectionCard(
        title = "Haqiqiy yetkazish xarajati (real cost)",
        subtitle = "Simulyator margin hisobida va analytics'da shu qiymatlar ishlatiladi",
        actions = {
            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        saveError = null
                        val body = """{"baseCost":${baseCost.ifBlank { "0" }},"perKmCost":${perKm.ifBlank { "0" }},"perKgCost":${perKg.ifBlank { "0" }},"currency":"UZS"}"""
                        runCatching { withContext(Dispatchers.IO) { ApiClient.put("$PRICING/settings", body) } }
                            .onSuccess { onSavedSettings() }
                            .onFailure { saveError = it.message ?: "Xatolik" }
                        saving = false
                    }
                },
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                else Text(AppStrings.settingsSave(appLanguage()), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CostField("Base cost (so'm)", baseCost, { baseCost = it }, Modifier.weight(1f))
            CostField("1 km uchun", perKm, { perKm = it }, Modifier.weight(1f))
            CostField("1 kg uchun", perKg, { perKg = it }, Modifier.weight(1f))
        }
        saveError?.let { Text(it, fontSize = 12.sp, color = MexaWarehouseColors.danger, modifier = Modifier.padding(top = 8.dp)) }
    }

    if (analytics != null) {
        SectionCard(title = "So'nggi ${analytics.days} kun — yetkazish KPI'lari", subtitle = "delivery_pricing_events jadvali asosida") {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("O'rtacha delivery fee", money(analytics.avgDeliveryFee), Modifier.weight(1f))
                KpiCard("O'rtacha xarajat", money(analytics.avgDeliveryCost), Modifier.weight(1f))
                KpiCard("O'rtacha subsidy", money(analytics.avgDeliverySubsidy), Modifier.weight(1f), accent = MexaWarehouseColors.danger)
                KpiCard("O'rtacha margin", money(analytics.avgDeliveryMargin), Modifier.weight(1f), accent = MexaWarehouseColors.statusActiveFg)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Buyurtmalar", "${analytics.orders}", Modifier.weight(1f))
                KpiCard("Bepul yetkazish", "${analytics.freeDeliveryOrders} (${analytics.freeDeliveryRate}%)", Modifier.weight(1f))
                KpiCard("Rag'bat olganlar", "${analytics.incentiveOrders} (${analytics.incentiveRate}%)", Modifier.weight(1f))
                KpiCard("AOV", money(analytics.averageOrderValue), Modifier.weight(1f))
            }
            ThresholdTable(analytics.thresholds, Modifier.padding(top = 14.dp))
        }
    }
}

@Composable
private fun CostField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

@Composable
private fun ThresholdTable(rows: List<ThresholdRow>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) {
        Text(
            "Threshold bo'yicha ma'lumot yo'q — birinchi buyurtmalardan keyin shu yerda conversion chiqadi.",
            modifier = modifier, fontSize = 12.sp, color = MexaWarehouseColors.textCaption,
        )
        return
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            TH("Threshold", 1.4f); TH("Exposure", 1f); TH("Yetkazildi", 1f); TH("Conversion", 1f); TH("O'rtacha savat", 1.2f)
        }
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TD(money(r.threshold), 1.4f, bold = true)
                TD("${r.exposed}", 1f)
                TD("${r.reached}", 1f)
                TD("${r.conversion}%", 1f, color = MexaWarehouseColors.indigoAccent, bold = true)
                TD(money(r.avgCart), 1.2f)
            }
        }
    }
}

@Composable
private fun RowScope.TH(text: String, weight: Float) {
    Text(text, Modifier.weight(weight), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textMuted)
}

@Composable
private fun RowScope.TD(text: String, weight: Float, color: Color = MexaWarehouseColors.textPrimary, bold: Boolean = false) {
    Text(
        text,
        Modifier.weight(weight),
        fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
    )
}

// ─── Narx qoidalari ────────────────────────────────────────────────────────

@Composable
private fun RulesTab(
    rules: List<PricingRuleRow>,
    onCreate: () -> Unit,
    onEdit: (PricingRuleRow) -> Unit,
    onDelete: (PricingRuleRow) -> Unit,
    onPreview: (String) -> Unit,
    preview: JsonObject?,
) {
    SectionCard(
        title = "Narx qoidalari",
        subtitle = "Qoida = Base + Distance + Weight, keyin min/max clamp; ustuvorlik bo'yicha birinchi mos qoida tanlanadi",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Yangi qoida", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        if (rules.isEmpty()) {
            Text("Hali qoida yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        }
        rules.forEach { r ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            ActiveChip(r.active)
                            MiniChip("prio ${r.priority}", MexaWarehouseColors.blueBadgeBg, MexaWarehouseColors.blueBadgeFg)
                            if (r.deliveryType.isNotBlank()) MiniChip(r.deliveryType, MexaWarehouseColors.amberBadgeBg, MexaWarehouseColors.amberBadgeFg)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onPreview(r.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview", modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.textMuted)
                            }
                            IconButton(onClick = { onEdit(r) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                            }
                            IconButton(onClick = { onDelete(r) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "O'chirish", modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.danger)
                            }
                        }
                    }
                    Text(
                        "Base ${money(r.baseFee)} + (km × ${money(r.pricePerKm)}) + (kg × ${money(r.pricePerKg)})",
                        fontSize = 13.sp, color = MexaWarehouseColors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                    )
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("min: ${money(r.minFee)}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        Text("max: ${r.maxFee?.let { money(it) } ?: "—"}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        if (r.startAt.isNotBlank() || r.endAt.isNotBlank()) {
                            Text("okno: ${r.startAt.ifBlank { "—" }} → ${r.endAt.ifBlank { "—" }}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                        }
                    }
                }
            }
        }

        preview?.let { p ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MexaWarehouseColors.infoBadgeBg,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Qoida preview", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.roleAdminFg)
                    Text("IF: ${p.str("if")}", fontSize = 12.sp, color = MexaWarehouseColors.textPrimary)
                    Text("FORMULA: ${p.str("formula")}", fontSize = 12.sp, color = MexaWarehouseColors.textPrimary)
                    Text("CLAMP: ${p.str("clamp")}", fontSize = 12.sp, color = MexaWarehouseColors.textPrimary)
                    Text("THEN: ${p.str("then")}", fontSize = 12.sp, color = MexaWarehouseColors.textPrimary)
                    Text("Window: ${p.str("window")}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                }
            }
        }
    }
}

// ─── Bepul yetkazish ───────────────────────────────────────────────────────

@Composable
private fun FreeTab(
    rows: List<FreeRuleRow>,
    onCreate: () -> Unit,
    onEdit: (FreeRuleRow) -> Unit,
    onDelete: (FreeRuleRow) -> Unit,
) {
    SectionCard(
        title = "Bepul yetkazish shartlari",
        subtitle = "Savat summasi chegaragacha yetganda delivery bepul bo'ladi (yetkazish narxini pasaytiradi)",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Yangi shart", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        if (rows.isEmpty()) {
            Text("Hali shart yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        }
        rows.forEach { r ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MexaWarehouseColors.surfaceLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (r.active) MexaWarehouseColors.roleAdminBorder else MexaWarehouseColors.borderSubtle),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                "Buyurtma ${money(r.minOrder)} dan yuqori bo'lsa delivery bepul",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary,
                            )
                            Text(
                                if (r.deliveryFee > BigDecimal.ZERO) "Qolgan haq: ${money(r.deliveryFee)}" else "Yetkazish to'liq bepul",
                                fontSize = 12.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ActiveChip(r.active)
                            IconButton(onClick = { onEdit(r) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                            }
                            IconButton(onClick = { onDelete(r) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.danger)
                            }
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniChip(r.name, MexaWarehouseColors.infoBadgeBg, MexaWarehouseColors.roleAdminFg)
                        MiniChip("prio ${r.priority}", MexaWarehouseColors.blueBadgeBg, MexaWarehouseColors.blueBadgeFg)
                        if (r.deliveryType.isNotBlank()) MiniChip(r.deliveryType, MexaWarehouseColors.amberBadgeBg, MexaWarehouseColors.amberBadgeFg)
                    }
                }
            }
        }
    }
}

// ─── Rag'batlar ────────────────────────────────────────────────────────────

@Composable
private fun IncentiveTab(
    rows: List<IncentiveRow>,
    onCreate: () -> Unit,
    onEdit: (IncentiveRow) -> Unit,
    onDelete: (IncentiveRow) -> Unit,
) {
    SectionCard(
        title = "Savat rag'batlari",
        subtitle = "Savat chegarasiga yetmaganda foydalanuvchiga rag'bat xabari ko'rsatiladi ({remaining}, {saving} o'rinlashuvchi)",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Yangi rag'bat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        if (rows.isEmpty()) {
            Text("Hali rag'bat yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        }
        rows.forEach { r ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MexaWarehouseColors.surfaceLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            ActiveChip(r.active)
                            MiniChip(
                                if (r.rewardType == "FREE") "BEPUL YETKAZISH" else if (r.rewardType == "PERCENT") "CHEGIRMA ${r.rewardValue}%" else "CHEGIRMA ${money(r.rewardValue)}",
                                MexaWarehouseColors.greenBadgeBg, MexaWarehouseColors.greenBadgeFg,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEdit(r) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                            }
                            IconButton(onClick = { onDelete(r) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.danger)
                            }
                        }
                    }
                    Text("Chegara: ${money(r.thresholdAmount)}", fontSize = 13.sp, color = MexaWarehouseColors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    Text("Xabar: ${r.messageTemplate}", fontSize = 12.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

// ─── Zonalar ───────────────────────────────────────────────────────────────

@Composable
private fun ZoneTab(
    rows: List<ZoneRow>,
    onCreate: () -> Unit,
    onEdit: (ZoneRow) -> Unit,
    onDelete: (ZoneRow) -> Unit,
) {
    SectionCard(
        title = "Yetkazish zonalari",
        subtitle = "Narx ustuvorligi: qoida → zona → default tarif; min/max clamp zona darajasida ham mavjud",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text(AppStrings.t(appLanguage(), "Yangi zona"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        if (rows.isEmpty()) {
            Text("Zona yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    TH("Hudud", 2f); TH("Bazaviy", 1.2f); TH("1 km", 1f); TH("1 kg", 1f); TH("min/max", 1.4f); TH("prio", 0.7f); TH("", 0.8f)
                }
                rows.forEach { z ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TD("${z.region} — ${z.district}", 2f, bold = true)
                        TD(money(z.fee), 1.2f)
                        TD(money(z.perKmFee), 1f)
                        TD(money(z.perKgFee), 1f)
                        TD("${z.minFee?.let { money(it) } ?: "—"}/${z.maxFee?.let { money(it) } ?: "—"}", 1.4f)
                        TD("${z.priority}", 0.7f)
                        Row(Modifier.weight(0.8f)) {
                            IconButton(onClick = { onEdit(z) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Tahrirlash", modifier = Modifier.size(15.dp), tint = MexaWarehouseColors.indigoAccent)
                            }
                            IconButton(onClick = { onDelete(z) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "O'chirish", modifier = Modifier.size(15.dp), tint = MexaWarehouseColors.danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Qo'shimcha haqlar ─────────────────────────────────────────────────────

@Composable
private fun SurchargeTab(
    rows: List<SurchargeRow>,
    onCreate: () -> Unit,
    onEdit: (SurchargeRow) -> Unit,
    onDelete: (SurchargeRow) -> Unit,
) {
    SectionCard(
        title = "Qo'shimcha haqlar",
        subtitle = "Peak hour, kecha, og'ir buyurtma — pricing qoidasidan keyin qo'llanadi",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Yangi haq", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        if (rows.isEmpty()) {
            Text("Qoida yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        }
        rows.forEach { s ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            ActiveChip(s.active)
                            MiniChip(s.conditionType, MexaWarehouseColors.amberBadgeBg, MexaWarehouseColors.amberBadgeFg)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEdit(s) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.indigoAccent)
                            }
                            IconButton(onClick = { onDelete(s) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MexaWarehouseColors.danger)
                            }
                        }
                    }
                    val cond = buildString {
                        append(s.conditionType)
                        if (s.conditionValue != null) append(" ${s.operator} ${s.conditionValue}")
                        if (s.timeFrom.isNotBlank()) append(" • vaqt ${s.timeFrom}–${s.timeTo}")
                        if (s.weightFrom != null || s.weightTo != null) {
                            append(" • oralig'i ${s.weightFrom ?: "—"}–${s.weightTo ?: "—"}")
                            append(if (s.conditionType == "DISTANCE") " km" else " kg")
                        }
                        if (s.startAt.isNotBlank() || s.endAt.isNotBlank()) {
                            append(" • okno ${s.startAt.ifBlank { "—" }} → ${s.endAt.ifBlank { "—" }}")
                        }
                    }
                    Text(cond, fontSize = 12.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 5.dp))
                    Text(
                        if (s.feeType == "PERCENT") "+${s.fee}%" else "+${money(s.fee)}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.danger, modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

// ─── Yetkazish turlari ─────────────────────────────────────────────────────

@Composable
private fun TypeTab(
    rows: List<TypeRow>,
    onCreate: () -> Unit,
    onEdit: (TypeRow) -> Unit,
    onDelete: (TypeRow) -> Unit,
) {
    SectionCard(
        title = "Yetkazib berish turlari",
        subtitle = "Har tur uchun qo'shimcha haq va taxminiy vaqt",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Yangi turlar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { t ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(t.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                            ActiveChip(t.active)
                        }
                        Text("Qo'shimcha haq: ${money(t.additionalFee)}", fontSize = 13.sp, color = MexaWarehouseColors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        Text("Taxminiy: ${t.estimatedMinutes} daqiqa", fontSize = 12.sp, color = MexaWarehouseColors.textMuted, modifier = Modifier.padding(top = 3.dp))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEdit(t) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = MexaWarehouseColors.indigoAccent)
                            }
                            IconButton(onClick = { onDelete(t) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = MexaWarehouseColors.danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Analitika ─────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsTab(analytics: AnalyticsData?) {
    if (analytics == null) return
    SectionCard(title = "Pricing analitikasi", subtitle = "Oxirgi ${analytics.days} kun, delivery hodisalari asosida") {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard("Delivery tushumi", money(analytics.deliveryRevenue), Modifier.weight(1f), accent = MexaWarehouseColors.statusActiveFg)
            KpiCard("Delivery xarajati", money(analytics.deliveryCostTotal), Modifier.weight(1f))
            KpiCard("Subsidy (zarar)", money(analytics.deliverySubsidyTotal), Modifier.weight(1f), accent = MexaWarehouseColors.danger)
            KpiCard("Yetkazishlar", "${analytics.deliveriesTotal}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard("O'rtacha masofa", "${analytics.avgDeliveryDistance} km", Modifier.weight(1f))
            KpiCard("Bepul ulushi", "${analytics.freeDeliveryRate}%", Modifier.weight(1f))
            KpiCard("Rag'bat ulushi", "${analytics.incentiveRate}%", Modifier.weight(1f))
            KpiCard("Buyurtmalar (event)", "${analytics.orders}", Modifier.weight(1f))
        }
        ThresholdTable(analytics.thresholds, Modifier.padding(top = 16.dp))
    }
}

private fun shortTime(iso: String): String =
    iso.ifBlank { "—" }.take(19).replace('T', ' ')

@Composable
private fun VersionsTab(rows: List<JsonObject>, onCreate: () -> Unit) {
    SectionCard(
        title = "Pricing versiyalari",
        subtitle = "Narx to'plamining versiyalar tarixi; faol versiya bitta, eski versiyalar ARCHIVED bo'ladi",
        actions = {
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Yangi versiya", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
    ) {
        if (rows.isEmpty()) {
            Text("Versiya yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        }
        rows.forEach { v ->
            val active = v.str("status") == "ACTIVE"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) MexaWarehouseColors.infoBadgeBg else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (active) MexaWarehouseColors.roleAdminBorder else MexaWarehouseColors.borderSubtle,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "v${v.int("version")}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MexaWarehouseColors.textPrimary,
                            )
                            MiniChip(
                                v.str("status"),
                                if (active) MexaWarehouseColors.greenBadgeBg else MexaWarehouseColors.amberBadgeBg,
                                if (active) MexaWarehouseColors.greenBadgeFg else MexaWarehouseColors.amberBadgeFg,
                            )
                        }
                        Text(v.str("createdBy").ifBlank { "—" }, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                    Text(
                        "Sabab: ${v.str("changeReason").ifBlank { "—" }}",
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    Text(
                        "Amal qiladi: ${shortTime(v.str("effectiveFrom"))} → ${shortTime(v.str("effectiveUntil"))}",
                        fontSize = 12.sp,
                        color = MexaWarehouseColors.textMuted,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditTab(rows: List<JsonObject>) {
    SectionCard(title = "O'zgarishlar audit logi", subtitle = "Oxirgi 200 yozuv; o'chirish sabablari majburiy saqlanadi") {
        if (rows.isEmpty()) {
            Text("Yozuv yo'q", Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MexaWarehouseColors.textCaption)
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    TH("Vaqt", 1.4f); TH("Admin", 1.2f); TH("Amal", 1f); TH("Ob'ekt", 1.6f); TH("Sabab", 2.4f)
                }
                rows.forEach { r ->
                    val action = r.str("action")
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        TD(shortTime(r.str("createdAt")), 1.4f, color = MexaWarehouseColors.textMuted)
                        TD(r.str("adminName").ifBlank { "—" }, 1.2f, bold = true)
                        TD(
                            action,
                            1f,
                            color = when {
                                action == "DELETE" -> MexaWarehouseColors.danger
                                action == "UPDATE" -> MexaWarehouseColors.amberBadgeFg
                                else -> MexaWarehouseColors.statusActiveFg
                            },
                        )
                        TD(
                            r.str("entityType").ifBlank { "—" } + r.str("entityId").take(8).let { if (it.isBlank()) "" else " #$it" },
                            1.6f,
                            color = MexaWarehouseColors.textMuted,
                        )
                        TD(r.str("reason").ifBlank { "—" }, 2.4f, color = MexaWarehouseColors.textMuted)
                    }
                }
            }
        }
    }
}

// ─── Simulyator ────────────────────────────────────────────────────────────

@Composable
private fun SimulatorTab(zones: List<ZoneRow>, types: List<TypeRow>) {
    val scope = rememberCoroutineScope()
    var region by remember { mutableStateOf("Toshkent") }
    var district by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("5") }
    var weight by remember { mutableStateOf("1") }
    var orderValue by remember { mutableStateOf("150000") }
    var deliveryType by remember { mutableStateOf("") }
    var zoneId by remember { mutableStateOf("") }
    var timeText by remember {
        mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")))
    }
    var result by remember { mutableStateOf<QuoteResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var simError by remember { mutableStateOf<String?>(null) }

    SectionCard(
        title = "Simulyator",
        subtitle = "Narx qoidasi, zona, surcharge va bepul-yetkazishlar — natija real buyurtmaga o'xshaydi",
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CostField("Hudud", region, { region = it }, Modifier.weight(1f))
            CostField("Tuman", district, { district = it }, Modifier.weight(1f))
            CostField("Masofa (km)", distance, { distance = it }, Modifier.weight(1f))
            CostField("Vazn (kg)", weight, { weight = it }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CostField("Savat summasi", orderValue, { orderValue = it }, Modifier.weight(1f))
            DropdownLike(
                label = "Yetkazish turi",
                value = deliveryType,
                options = types.map { it.name },
                onChange = { deliveryType = it },
                modifier = Modifier.weight(1f),
            )
            DropdownLike(
                label = "Zona",
                value = zones.firstOrNull { it.id == zoneId }?.let { "${it.region} — ${it.district}" } ?: "",
                options = zones.map { "${it.region} — ${it.district}" },
                onChange = { v ->
                    zoneId = if (v.isEmpty()) "" else (zones.find { "${it.region} — ${it.district}" == v }?.id ?: "")
                },
                modifier = Modifier.weight(1f),
            )
            CostField("Vaqt", timeText, { timeText = it }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    busy = true
                    simError = null
                    scope.launch {
                        val body = buildJson(
                            listOf(
                                PricingField.Text("Hudud", "region", region),
                                PricingField.Text("Tuman", "district", district),
                                PricingField.Num("Masofa", "distanceKm", distance),
                                PricingField.Num("Vazn", "weightKg", weight),
                                PricingField.Num("Savat", "orderValue", orderValue),
                                PricingField.Text("Tur", "deliveryType", deliveryType),
                                PricingField.Text("Vaqt", "time", timeText),
                            ) + if (zoneId.isBlank()) emptyList() else listOf(PricingField.Text("Zone", "zoneId", zoneId)),
                        )
                        val r = runCatching {
                            withContext(Dispatchers.IO) { ApiClient.post("$PRICING/simulator", body) }
                        }
                        result = r.getOrNull()?.let { dataObject(it)?.toQuoteResult() }
                        r.exceptionOrNull()?.let { simError = it.message ?: "Xatolik" }
                        busy = false
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                Text("Hisoblash", fontWeight = FontWeight.SemiBold)
            }
            simError?.let { Text(it, fontSize = 12.sp, color = MexaWarehouseColors.danger) }
        }

        result?.let { q ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Natija", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                            if (q.freeDelivery) MiniChip("BEPUL", MexaWarehouseColors.greenBadgeBg, MexaWarehouseColors.greenBadgeFg)
                            MiniChip(q.deliveryType, MexaWarehouseColors.infoBadgeBg, MexaWarehouseColors.roleAdminFg)
                        }
                        Text(money(q.finalFee), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.indigoAccent)
                    }
                    Column(Modifier.padding(top = 10.dp)) {
                        InfoRow("Bazaviy haq", money(q.baseFee))
                        InfoRow("Masofa uchun", money(q.distanceFee))
                        InfoRow("Vazn uchun", money(q.weightFee))
                        q.surcharges.forEach { (name, amount) -> InfoRow("+ $name", money(amount)) }
                        if (q.discount > BigDecimal.ZERO) InfoRow("Bepul yetkazish chegirmasi", "−" + money(q.discount))
                        InfoRow("Zone", q.zoneName.ifBlank { "—" })
                        InfoRow("Qo'llangan qoida", q.appliedRule.ifBlank { "— (default)" })
                        InfoRow("Taxminiy xarajat", money(q.estimatedCost))
                        InfoRow("Margin", money(q.margin))
                        InfoRow("Taxminiy vaqt", "${q.estimatedMinutes} daqiqa • ${q.estimatedDays} kun")
                    }
                    val msg = q.incentiveMessage ?: q.message
                    if (msg != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MexaWarehouseColors.greenBadgeBg,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(msg, fontSize = 13.sp, color = MexaWarehouseColors.greenBadgeFg, fontWeight = FontWeight.Medium)
                                val thr = q.nextThreshold
                                if (thr != null) {
                                    Text(
                                        "Chegara: ${money(thr)} • qolgan: ${money(q.remainingAmount ?: BigDecimal.ZERO)} • tejash: ${money(q.potentialSaving ?: BigDecimal.ZERO)}",
                                        fontSize = 12.sp, color = MexaWarehouseColors.greenBadgeFg, modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DropdownLike(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        androidx.compose.material3.ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = value.ifEmpty { "— tanlanmagan —" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                androidx.compose.material3.DropdownMenuItem(text = { Text(AppStrings.t(appLanguage(), "— tanlanmagan —")) }, onClick = { onChange(""); expanded = false })
                options.forEach { opt ->
                    androidx.compose.material3.DropdownMenuItem(text = { Text(opt) }, onClick = { onChange(opt); expanded = false })
                }
            }
        }
    }
}

// ─── Edit field to'plamlari ────────────────────────────────────────────────

private fun ruleFields(
    rule: PricingRuleRow?,
    zoneOptions: List<Pair<String, String>>,
    typeOptions: List<String>,
): List<PricingField> = listOf(
    PricingField.Text("Nomi", "name", rule?.name ?: "", required = true),
    PricingField.Num("Bazaviy haq", "baseFee", rule?.baseFee?.toPlainString() ?: "10000"),
    PricingField.Num("1 km narxi", "pricePerKm", rule?.pricePerKm?.toPlainString() ?: "1000"),
    PricingField.Num("1 kg narxi", "pricePerKg", rule?.pricePerKg?.toPlainString() ?: "0"),
    PricingField.Num("Minimal haq", "minFee", rule?.minFee?.toPlainString() ?: "0"),
    PricingField.Num("Maksimal haq (bo'sh = cheksiz)", "maxFee", rule?.maxFee?.toPlainString() ?: ""),
    PricingField.Num("Ustuvorlik", "priority", "${rule?.priority ?: 100}"),
    PricingField.Sel(
        "Yetkazish turi", "deliveryType",
        typeOptions.map { it to it },
        rule?.deliveryType ?: "",
        hint = "Bo'sh = barcha turlar uchun",
    ),
    PricingField.Sel("Zona", "zoneId", zoneOptions, rule?.zoneId ?: "", hint = "Tanlanmasa hudud bo'yicha aniqlanadi"),
    PricingField.Bool("Faol", "active", rule?.active ?: true),
    PricingField.Text("Boshlanish (yyyy-MM-ddTHH:mm)", "startAt", rule?.startAt ?: "", skipWhenBlank = true),
    PricingField.Text("Tugash (yyyy-MM-ddTHH:mm)", "endAt", rule?.endAt ?: "", skipWhenBlank = true),
    PricingField.Text("O'zgarish sababi", "changeReason", rule?.changeReason ?: "Yangi qoida"),
)

private fun freeFields(
    rule: FreeRuleRow?,
    zoneOptions: List<Pair<String, String>>,
    typeOptions: List<String>,
): List<PricingField> = listOf(
    PricingField.Text("Nomi", "name", rule?.name ?: "200k dan yuqori bepul", required = true),
    PricingField.Num("Chegara (so'm)", "minOrder", rule?.minOrder?.toPlainString() ?: "200000", required = true),
    PricingField.Num("Qolgan haq (0 = bepul)", "deliveryFee", rule?.deliveryFee?.toPlainString() ?: "0"),
    PricingField.Sel("Yetkazish turi", "deliveryType", typeOptions.map { it to it }, rule?.deliveryType ?: ""),
    PricingField.Sel("Zona", "zoneId", zoneOptions, rule?.zoneId ?: ""),
    PricingField.Num("Ustuvorlik", "priority", "${rule?.priority ?: 100}"),
    PricingField.Bool("Faol", "active", rule?.active ?: true),
    PricingField.Text("Boshlanish", "startAt", rule?.startAt ?: "", skipWhenBlank = true),
    PricingField.Text("Tugash", "endAt", rule?.endAt ?: "", skipWhenBlank = true),
)

private fun incentiveFields(rule: IncentiveRow?): List<PricingField> = listOf(
    PricingField.Text("Nomi", "name", rule?.name ?: "Savat rag'bati", required = true),
    PricingField.Num("Chegara (so'm)", "thresholdAmount", rule?.thresholdAmount?.toPlainString() ?: "150000", required = true),
    PricingField.Sel(
        "Reward turi", "rewardType",
        listOf("FREE", "AMOUNT", "PERCENT").map { it to it },
        rule?.rewardType ?: "AMOUNT",
    ),
    PricingField.Num("Reward qiymati", "rewardValue", rule?.rewardValue?.toPlainString() ?: "5000"),
    PricingField.Text(
        "Xabar shabloni",
        "messageTemplate",
        rule?.messageTemplate
            ?: "Yana {remaining} so'mlik mahsulot qo'shing va bepul yetkazib berishdan foydalaning.",
        hint = "{remaining} va {saving} o'rinlashadi",
    ),
    PricingField.Num("Ustuvorlik", "priority", "${rule?.priority ?: 100}"),
    PricingField.Bool("Faol", "active", rule?.active ?: true),
    PricingField.Text("Boshlanish (yyyy-MM-ddTHH:mm)", "startAt", rule?.startAt ?: "", skipWhenBlank = true),
    PricingField.Text("Tugash (yyyy-MM-ddTHH:mm)", "endAt", rule?.endAt ?: "", skipWhenBlank = true),
)

private fun surchargeFields(rule: SurchargeRow?, typeOptions: List<String>): List<PricingField> {
    fun opts(vararg v: String) = v.map { it to it }
    return listOf(
        PricingField.Text("Nomi", "name", rule?.name ?: "", required = true),
        PricingField.Sel(
            "Shart turi", "conditionType",
            opts("PEAK_HOUR", "NIGHT", "HOLIDAY", "HEAVY_ORDER", "HIGH_DEMAND", "EXPRESS", "DISTANCE", "WEIGHT"),
            rule?.conditionType ?: "PEAK_HOUR",
        ),
        PricingField.Sel("Operator", "operator", opts("GTE", "LTE", "EQ", "BETWEEN"), rule?.operator ?: "GTE"),
        PricingField.Num("Shart qiymati", "conditionValue", rule?.conditionValue?.toPlainString() ?: ""),
        PricingField.Num("Haq", "fee", rule?.fee?.toPlainString() ?: "5000", required = true),
        PricingField.Sel("Haq turi", "feeType", opts("FIXED", "PERCENT"), rule?.feeType ?: "FIXED"),
        PricingField.Text("Boshlanish vaqti (HH:mm)", "timeFrom", rule?.timeFrom ?: "", hint = "Masalan 18:00", skipWhenBlank = true),
        PricingField.Text("Tugash vaqti (HH:mm)", "timeTo", rule?.timeTo ?: "", hint = "Masalan 21:00", skipWhenBlank = true),
        PricingField.Num(
            "Min qiymat (BETWEEN)", "weightFrom", rule?.weightFrom?.toPlainString() ?: "",
            hint = "Operator BETWEEN: vazn kg (WEIGHT) yoki masofa km (DISTANCE) boshlang'ich qiymati",
        ),
        PricingField.Num(
            "Max qiymat (BETWEEN)", "weightTo", rule?.weightTo?.toPlainString() ?: "",
            hint = "Operator BETWEEN: vazn kg (WEIGHT) yoki masofa km (DISTANCE) yakuniy qiymati",
        ),
        PricingField.Sel("Yetkazish turi", "deliveryType", typeOptions.map { it to it }, rule?.deliveryType ?: ""),
        PricingField.Num("Ustuvorlik", "priority", "${rule?.priority ?: 100}"),
        PricingField.Bool("Faol", "active", rule?.active ?: true),
        PricingField.Text("Boshlanish sanasi (yyyy-MM-ddTHH:mm)", "startAt", rule?.startAt ?: "", hint = "Qoida shu sanadan boshlab faol", skipWhenBlank = true),
        PricingField.Text("Tugash sanasi (yyyy-MM-ddTHH:mm)", "endAt", rule?.endAt ?: "", hint = "Qoida shu sanada o'chadi", skipWhenBlank = true),
    )
}

private fun typeFields(rule: TypeRow?): List<PricingField> = listOf(
    PricingField.Text("Nomi", "name", rule?.name ?: "", required = true, hint = "Masalan: Express"),
    PricingField.Num("Qo'shimcha haq", "additionalFee", rule?.additionalFee?.toPlainString() ?: "0"),
    PricingField.Num("Bazaviy haq", "baseFee", rule?.baseFee?.toPlainString() ?: "0"),
    PricingField.Num("Taxminiy daqiqa", "estimatedMinutes", "${rule?.estimatedMinutes ?: 60}"),
    PricingField.Bool("Faol", "active", rule?.active ?: true),
)

private fun zoneFields(zone: ZoneRow): List<PricingField> = listOf(
    PricingField.Text("Hudud", "region", zone.region, required = true),
    PricingField.Text("Tuman", "district", zone.district, required = true),
    PricingField.Num("Bazaviy haq", "fee", zone.fee.toPlainString()),
    PricingField.Num("1 km narxi", "perKmFee", zone.perKmFee.toPlainString()),
    PricingField.Num("1 kg narxi", "perKgFee", zone.perKgFee.toPlainString()),
    PricingField.Num("Bepul chegarasi", "freeAbove", zone.freeAbove?.toPlainString() ?: ""),
    PricingField.Num("Minimal haq", "minFee", zone.minFee?.toPlainString() ?: ""),
    PricingField.Num("Maksimal haq", "maxFee", zone.maxFee?.toPlainString() ?: ""),
    PricingField.Num("Ustuvorlik", "priority", "${zone.priority}"),
    PricingField.Num("Yetkazish kunlari", "estimatedDays", "${zone.estimatedDays}"),
)

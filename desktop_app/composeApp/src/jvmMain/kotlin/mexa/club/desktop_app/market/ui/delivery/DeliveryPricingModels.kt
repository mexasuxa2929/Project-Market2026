package mexa.club.desktop_app.market.ui.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors
import java.math.BigDecimal

/** Delivery Pricing sahifasi uchun modellar + kichik UI yordamchilari. */

// ─── JSON yordamchilari ────────────────────────────────────────────────────

fun JsonObject.num(key: String): BigDecimal =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toBigDecimalOrNull() ?: BigDecimal.ZERO

fun JsonObject.numOrNull(key: String): BigDecimal? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toBigDecimalOrNull()

fun JsonObject.int(key: String): Int =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0

fun JsonObject.long(key: String): Long =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L

fun JsonObject.bool(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull
        ?: (this[key] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull() ?: false

fun JsonObject.str(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: ""

/** API envelope'dan data massivini olish. */
fun dataArray(text: String): List<JsonObject> {
    val root = ApiClient.parseJsonObject(text) ?: return emptyList()
    val data = root["data"] as? JsonArray ?: return emptyList()
    return data.mapNotNull { it as? JsonObject }
}

/** API envelope'dan data obyektini olish. */
fun dataObject(text: String): JsonObject? {
    val root = ApiClient.parseJsonObject(text) ?: return null
    return root["data"] as? JsonObject
}

/** Pulni formatlash: 26700 → "26 700 so'm". */
fun money(v: BigDecimal?): String {
    if (v == null) return "0 so'm"
    val n = v.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
    val sb = StringBuilder()
    var count = 0
    for (i in n.indices.reversed()) {
        sb.append(n[i])
        count++
        if (count % 3 == 0 && i > 0 && n[i - 1] != '-') sb.append(' ')
    }
    return sb.reverse().toString() + " so'm"
}

fun moneyStr(v: String): String = money(v.toBigDecimalOrNull() ?: BigDecimal.ZERO)

// ─── Modellar ──────────────────────────────────────────────────────────────

data class PricingRuleRow(
    val id: String,
    val name: String,
    val zoneId: String,
    val deliveryType: String,
    val baseFee: BigDecimal,
    val pricePerKm: BigDecimal,
    val pricePerKg: BigDecimal,
    val minFee: BigDecimal,
    val maxFee: BigDecimal?,
    val priority: Int,
    val active: Boolean,
    val startAt: String,
    val endAt: String,
    val changeReason: String,
)

fun JsonObject.toPricingRule() = PricingRuleRow(
    id = str("id"),
    name = str("name"),
    zoneId = str("zoneId"),
    deliveryType = str("deliveryType"),
    baseFee = num("baseFee"),
    pricePerKm = num("pricePerKm"),
    pricePerKg = num("pricePerKg"),
    minFee = num("minFee"),
    maxFee = numOrNull("maxFee"),
    priority = int("priority"),
    active = bool("active"),
    startAt = str("startAt"),
    endAt = str("endAt"),
    changeReason = str("changeReason"),
)

data class FreeRuleRow(
    val id: String,
    val name: String,
    val minOrder: BigDecimal,
    val deliveryFee: BigDecimal,
    val zoneId: String,
    val deliveryType: String,
    val priority: Int,
    val active: Boolean,
    val startAt: String,
    val endAt: String,
)

fun JsonObject.toFreeRule() = FreeRuleRow(
    id = str("id"),
    name = str("name"),
    minOrder = num("minOrder"),
    deliveryFee = num("deliveryFee"),
    zoneId = str("zoneId"),
    deliveryType = str("deliveryType"),
    priority = int("priority"),
    active = bool("active"),
    startAt = str("startAt"),
    endAt = str("endAt"),
)

data class IncentiveRow(
    val id: String,
    val name: String,
    val thresholdAmount: BigDecimal,
    val rewardType: String,
    val rewardValue: BigDecimal,
    val messageTemplate: String,
    val priority: Int,
    val active: Boolean,
    val startAt: String,
    val endAt: String,
)

fun JsonObject.toIncentive() = IncentiveRow(
    id = str("id"),
    name = str("name"),
    thresholdAmount = num("thresholdAmount"),
    rewardType = str("rewardType").ifBlank { "FREE" },
    rewardValue = num("rewardValue"),
    messageTemplate = str("messageTemplate"),
    priority = int("priority"),
    active = bool("active"),
    startAt = str("startAt"),
    endAt = str("endAt"),
)

data class SurchargeRow(
    val id: String,
    val name: String,
    val conditionType: String,
    val operator: String,
    val conditionValue: BigDecimal?,
    val fee: BigDecimal,
    val feeType: String,
    val timeFrom: String,
    val timeTo: String,
    val weightFrom: BigDecimal?,
    val weightTo: BigDecimal?,
    val deliveryType: String,
    val priority: Int,
    val active: Boolean,
    val startAt: String,
    val endAt: String,
)

fun JsonObject.toSurcharge() = SurchargeRow(
    id = str("id"),
    name = str("name"),
    conditionType = str("conditionType"),
    operator = str("operator").ifBlank { "GTE" },
    conditionValue = numOrNull("conditionValue"),
    fee = num("fee"),
    feeType = str("feeType").ifBlank { "FIXED" },
    timeFrom = str("timeFrom"),
    timeTo = str("timeTo"),
    weightFrom = numOrNull("weightFrom"),
    weightTo = numOrNull("weightTo"),
    deliveryType = str("deliveryType"),
    priority = int("priority"),
    active = bool("active"),
    startAt = str("startAt"),
    endAt = str("endAt"),
)

data class TypeRow(
    val id: String,
    val name: String,
    val baseFee: BigDecimal,
    val additionalFee: BigDecimal,
    val estimatedMinutes: Int,
    val active: Boolean,
)

fun JsonObject.toTypeRow() = TypeRow(
    id = str("id"),
    name = str("name"),
    baseFee = num("baseFee"),
    additionalFee = num("additionalFee"),
    estimatedMinutes = int("estimatedMinutes"),
    active = bool("active"),
)

data class ZoneRow(
    val id: String,
    val region: String,
    val district: String,
    val fee: BigDecimal,
    val perKmFee: BigDecimal,
    val perKgFee: BigDecimal,
    val freeAbove: BigDecimal?,
    val minFee: BigDecimal?,
    val maxFee: BigDecimal?,
    val priority: Int,
    val estimatedDays: Int,
)

fun JsonObject.toZoneRow() = ZoneRow(
    id = str("id"),
    region = str("region"),
    district = str("district"),
    fee = num("fee"),
    perKmFee = num("perKmFee"),
    perKgFee = num("perKgFee"),
    freeAbove = numOrNull("freeAbove"),
    minFee = numOrNull("minFee"),
    maxFee = numOrNull("maxFee"),
    priority = int("priority").takeIf { it != 0 } ?: 100,
    estimatedDays = int("estimatedDays").takeIf { it != 0 } ?: 1,
)

data class QuoteResult(
    val baseFee: BigDecimal,
    val distanceFee: BigDecimal,
    val weightFee: BigDecimal,
    val surcharges: List<Pair<String, BigDecimal>>,
    val surchargeTotal: BigDecimal,
    val discount: BigDecimal,
    val finalFee: BigDecimal,
    val estimatedCost: BigDecimal,
    val margin: BigDecimal,
    val freeDelivery: Boolean,
    val appliedRule: String,
    val zoneName: String,
    val deliveryType: String,
    val estimatedMinutes: Int,
    val estimatedDays: Int,
    val nextThreshold: BigDecimal?,
    val remainingAmount: BigDecimal?,
    val potentialSaving: BigDecimal?,
    val incentiveMessage: String?,
    val message: String?,
)

fun JsonObject.toQuoteResult(): QuoteResult {
    val surcharges = (this["surcharges"] as? JsonArray).orEmpty().mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        o.str("name") to o.num("amount")
    }
    val incentive = this["incentive"] as? JsonObject
    return QuoteResult(
        baseFee = num("baseFee"),
        distanceFee = num("distanceFee"),
        weightFee = num("weightFee"),
        surcharges = surcharges,
        surchargeTotal = num("surchargeTotal"),
        discount = num("discount"),
        finalFee = num("finalFee"),
        estimatedCost = num("estimatedCost"),
        margin = num("margin"),
        freeDelivery = bool("freeDelivery"),
        appliedRule = str("appliedRule"),
        zoneName = str("zoneName"),
        deliveryType = str("deliveryType"),
        estimatedMinutes = int("estimatedMinutes"),
        estimatedDays = int("estimatedDays"),
        nextThreshold = incentive?.numOrNull("nextThreshold"),
        remainingAmount = incentive?.numOrNull("remainingAmount"),
        potentialSaving = incentive?.numOrNull("potentialSaving"),
        incentiveMessage = incentive?.str("message")?.ifBlank { null },
        message = str("message").ifBlank { null },
    )
}

data class ThresholdRow(
    val threshold: BigDecimal,
    val exposed: Long,
    val reached: Long,
    val conversion: BigDecimal,
    val avgCart: BigDecimal,
)

data class AnalyticsData(
    val days: Int,
    val orders: Long,
    val avgDeliveryFee: BigDecimal,
    val avgDeliveryCost: BigDecimal,
    val avgDeliverySubsidy: BigDecimal,
    val avgDeliveryMargin: BigDecimal,
    val averageOrderValue: BigDecimal,
    val freeDeliveryOrders: Long,
    val freeDeliveryRate: BigDecimal,
    val incentiveOrders: Long,
    val incentiveRate: BigDecimal,
    val avgDeliveryDistance: BigDecimal,
    val deliveryRevenue: BigDecimal,
    val deliveryCostTotal: BigDecimal,
    val deliverySubsidyTotal: BigDecimal,
    val deliveriesTotal: Long,
    val thresholds: List<ThresholdRow>,
)

fun JsonObject.toAnalytics(): AnalyticsData {
    val thresholds = (this["thresholds"] as? JsonArray).orEmpty().mapNotNull { el ->
        val o = el as? JsonObject ?: return@mapNotNull null
        ThresholdRow(
            threshold = o.num("threshold"),
            exposed = o.long("exposed"),
            reached = o.long("reached"),
            conversion = o.num("conversion"),
            avgCart = o.num("avgCart"),
        )
    }
    return AnalyticsData(
        days = int("days"),
        orders = long("orders"),
        avgDeliveryFee = num("avgDeliveryFee"),
        avgDeliveryCost = num("avgDeliveryCost"),
        avgDeliverySubsidy = num("avgDeliverySubsidy"),
        avgDeliveryMargin = num("avgDeliveryMargin"),
        averageOrderValue = num("averageOrderValue"),
        freeDeliveryOrders = long("freeDeliveryOrders"),
        freeDeliveryRate = num("freeDeliveryRate"),
        incentiveOrders = long("incentiveOrders"),
        incentiveRate = num("incentiveRate"),
        avgDeliveryDistance = num("avgDeliveryDistance"),
        deliveryRevenue = num("deliveryRevenue"),
        deliveryCostTotal = num("deliveryCostTotal"),
        deliverySubsidyTotal = num("deliverySubsidyTotal"),
        deliveriesTotal = long("deliveriesTotal"),
        thresholds = thresholds,
    )
}

// ─── Umumiy UI ─────────────────────────────────────────────────────────────

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MexaWarehouseColors.surfaceLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                }
                actions?.invoke()
            }
            content()
        }
    }
}

@Composable
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier, accent: Color = MexaWarehouseColors.indigoAccent) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MexaWarehouseColors.borderSubtle),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 11.sp, color = MexaWarehouseColors.textMuted, letterSpacing = 0.3.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun MiniChip(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box2(modifier, bg) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg, letterSpacing = 0.2.sp)
    }
}

@Composable
private fun Box2(modifier: Modifier, bg: Color, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier
            .background(bg, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun ActiveChip(active: Boolean) {
    if (active) MiniChip("FAOL", MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg)
    else MiniChip("O'CHIRILGAN", MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg)
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
        Text(value, fontSize = 12.sp, color = MexaWarehouseColors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

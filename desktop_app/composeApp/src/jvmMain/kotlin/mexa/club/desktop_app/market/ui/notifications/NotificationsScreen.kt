package mexa.club.desktop_app.market.ui.notifications

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.itemsOrContentArray
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

data class NotificationRow(
    val id: String,
    val channel: String,
    val type: String,
    val recipientEmail: String,
    val recipientPhone: String,
    val subject: String,
    val status: String,
    val retryCount: Int,
    val errorMessage: String,
    val createdAt: String,
)

private val ALL_FILTER = "ALL"
val NOTIFICATION_STATUS_OPTIONS = listOf("PENDING", "SENT", "FAILED", "DEAD_LETTER")

fun notificationStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Kutilmoqda"
    "SENT" -> "Yuborildi"
    "FAILED" -> "Xato"
    "DEAD_LETTER" -> "Bekor qilindi"
    else -> status
}

fun notificationStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "SENT" -> MexaWarehouseColors.statusActiveBg to MexaWarehouseColors.statusActiveFg
    "PENDING" -> MexaWarehouseColors.amberBadgeBg to Color(0xFFB45309)
    "FAILED", "DEAD_LETTER" -> MexaWarehouseColors.statusBlockedBg to MexaWarehouseColors.statusBlockedFg
    else -> MexaWarehouseColors.borderSubtle to MexaWarehouseColors.textMuted
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notifications by remember { mutableStateOf<List<NotificationRow>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(ALL_FILTER) }
    var statusExpanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<NotificationRow?>(null) }

    suspend fun fetchNotifications(): List<NotificationRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/notifications?page=0&size=200")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
        val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        items.mapNotNull { el ->
            val n = el as? JsonObject ?: return@mapNotNull null
            NotificationRow(
                id = n.stringField("id").ifEmpty { return@mapNotNull null },
                channel = n.stringField("channel"),
                type = n.stringField("type"),
                recipientEmail = n.stringField("recipientEmail"),
                recipientPhone = n.stringField("recipientPhone"),
                subject = n.stringField("subject"),
                status = n.stringField("status").ifEmpty { "PENDING" },
                retryCount = n.stringField("retryCount").toIntOrNull() ?: 0,
                errorMessage = n.stringField("errorMessage"),
                createdAt = n.stringField("createdAt"),
            )
        }.sortedByDescending { it.createdAt }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchNotifications() }
                .onSuccess { notifications = it }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    selected?.let { target ->
        NotificationDetailDialog(notification = target, onDismiss = { selected = null }, onResent = { selected = null; reload() })
    }

    val filtered = notifications.filter { n ->
        (statusFilter == ALL_FILTER || n.status.equals(statusFilter, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
                n.recipientEmail.contains(searchQuery, ignoreCase = true) ||
                n.recipientPhone.contains(searchQuery, ignoreCase = true) ||
                n.subject.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).background(MexaWarehouseColors.indigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Bildirishnomalar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Yuborilgan email/SMS/push xabarlar tarixi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Email, telefon yoki mavzu bo'yicha...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }, modifier = Modifier.width(200.dp)) {
                OutlinedTextField(
                    value = if (statusFilter == ALL_FILTER) "Barcha holatlar" else notificationStatusLabel(statusFilter),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    DropdownMenuItem(text = { Text("Barcha holatlar") }, onClick = { statusFilter = ALL_FILTER; statusExpanded = false })
                    NOTIFICATION_STATUS_OPTIONS.forEach { opt ->
                        DropdownMenuItem(text = { Text(notificationStatusLabel(opt)) }, onClick = { statusFilter = opt; statusExpanded = false })
                    }
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MexaWarehouseColors.indigoAccent)
            }
            error != null -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "Xatolik", color = MexaWarehouseColors.danger)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload() }) { Text("Qayta urinish") }
                }
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Bildirishnomalar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> NotificationsTable(filtered, onRowClick = { selected = it })
        }
    }
}

@Composable
private fun NotificationsTable(notifications: List<NotificationRow>, onRowClick: (NotificationRow) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Kanal" to 0.8f, "Turi" to 1.4f, "Qabul qiluvchi" to 1.6f, "Mavzu" to 1.8f, "Holat" to 1f).forEach { (h, w) ->
                Text(h.uppercase(), modifier = Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        notifications.forEachIndexed { idx, n ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .clickable { onRowClick(n) }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(n.channel, modifier = Modifier.weight(0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Text(n.type, modifier = Modifier.weight(1.4f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(n.recipientEmail.ifBlank { n.recipientPhone }.ifBlank { "—" }, modifier = Modifier.weight(1.6f), fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                Text(n.subject.ifBlank { "—" }, modifier = Modifier.weight(1.8f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Box(Modifier.weight(1f)) {
                    val (bg, fg) = notificationStatusColors(n.status)
                    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(notificationStatusLabel(n.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

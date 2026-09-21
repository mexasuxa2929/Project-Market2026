package mexa.club.desktop_app.market.ui.auditlog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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

data class AuditLogRow(
    val id: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val actorUsername: String,
    val details: String,
    val createdAt: String,
)

@Composable
fun AuditLogScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var logs by remember { mutableStateOf<List<AuditLogRow>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    suspend fun fetchLogs(): List<AuditLogRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/audit-logs?page=0&size=200")
        val data = ApiClient.parseJsonObject(text)?.let { ApiClient.dataObject(it) }
        val items = data?.itemsOrContentArray() ?: JsonArray(emptyList())
        items.mapNotNull { el ->
            val l = el as? JsonObject ?: return@mapNotNull null
            AuditLogRow(
                id = l.stringField("id").ifEmpty { return@mapNotNull null },
                action = l.stringField("action"),
                targetType = l.stringField("targetType"),
                targetId = l.stringField("targetId"),
                actorUsername = l.stringField("actorUsername"),
                details = l.stringField("details"),
                createdAt = l.stringField("createdAt"),
            )
        }.sortedByDescending { it.createdAt }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchLogs() }
                .onSuccess { logs = it }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val filtered = logs.filter { l ->
        searchQuery.isBlank() ||
            l.action.contains(searchQuery, ignoreCase = true) ||
            l.targetType.contains(searchQuery, ignoreCase = true) ||
            l.actorUsername.contains(searchQuery, ignoreCase = true) ||
            l.details.contains(searchQuery, ignoreCase = true)
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
                Icon(Icons.Default.History, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Audit jurnali", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Tizimda amalga oshirilgan o'zgarishlar tarixi", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Amal, obyekt turi yoki foydalanuvchi bo'yicha...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )

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
                Text("Yozuvlar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> AuditLogTable(filtered)
        }
    }
}

@Composable
private fun AuditLogTable(logs: List<AuditLogRow>) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Amal" to 1.2f, "Obyekt" to 1.4f, "Foydalanuvchi" to 1.2f, "Tafsilot" to 2f, "Sana" to 1f).forEach { (h, w) ->
                Text(h.uppercase(), modifier = Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        logs.forEachIndexed { idx, l ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(l.action.ifBlank { "—" }, modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Column(Modifier.weight(1.4f)) {
                    Text(l.targetType.ifBlank { "—" }, fontSize = 13.sp, color = MexaWarehouseColors.textPrimary)
                    if (l.targetId.isNotBlank()) Text(l.targetId.take(8), fontSize = 11.sp, color = MexaWarehouseColors.textMuted)
                }
                Text(l.actorUsername.ifBlank { "—" }, modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(l.details.ifBlank { "—" }, modifier = Modifier.weight(2f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                Text(l.createdAt.take(19).replace("T", " "), modifier = Modifier.weight(1f), fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

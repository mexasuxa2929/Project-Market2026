package mexa.club.desktop_app.market.ui.templates

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import mexa.club.desktop_app.market.api.stringField
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

data class TemplateRow(
    val id: String,
    val type: String,
    val channel: String,
    val subject: String,
    val bodyTemplate: String,
    val active: Boolean,
)

@Composable
fun TemplatesScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var templates by remember { mutableStateOf<List<TemplateRow>>(emptyList()) }
    var editTarget by remember { mutableStateOf<TemplateRow?>(null) }

    suspend fun fetchTemplates(): List<TemplateRow> = withContext(Dispatchers.IO) {
        val text = ApiClient.get("/api/admin/templates")
        val root = ApiClient.parseJsonObject(text)
        val arr = (root?.get("data") as? JsonArray) ?: JsonArray(emptyList())
        arr.mapNotNull { el ->
            val t = el as? JsonObject ?: return@mapNotNull null
            TemplateRow(
                id = t.stringField("id").ifEmpty { return@mapNotNull null },
                type = t.stringField("type"),
                channel = t.stringField("channel"),
                subject = t.stringField("subject"),
                bodyTemplate = t.stringField("bodyTemplate"),
                active = t.stringField("active").toBooleanStrictOrNull() ?: true,
            )
        }.sortedBy { it.type }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { fetchTemplates() }
                .onSuccess { templates = it }
                .onFailure { error = it.message ?: "Xatolik" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    editTarget?.let { target ->
        TemplateEditDialog(template = target, onDismiss = { editTarget = null }, onSaved = { editTarget = null; reload() })
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
                Icon(Icons.Default.Description, contentDescription = null, tint = MexaWarehouseColors.indigoAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Xabar shablonlari", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text("Email/SMS/push uchun matn shablonlari", fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
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
            templates.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Shablonlar topilmadi", color = MexaWarehouseColors.textMuted)
            }
            else -> TemplatesTable(templates, onRowClick = { editTarget = it })
        }
    }
}

@Composable
private fun TemplatesTable(templates: List<TemplateRow>, onRowClick: (TemplateRow) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MexaWarehouseColors.borderSubtle, RoundedCornerShape(12.dp))
            .background(MexaWarehouseColors.surfaceLowest, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().background(MexaWarehouseColors.tableHeaderBg).padding(vertical = 12.dp, horizontal = 20.dp),
        ) {
            listOf("Turi" to 1.6f, "Kanal" to 0.8f, "Mavzu" to 1.8f, "Holat" to 0.8f).forEach { (h, w) ->
                Text(h.uppercase(), modifier = Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = MexaWarehouseColors.outline)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle))
        templates.forEachIndexed { idx, t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) MexaWarehouseColors.surfaceLowest else MexaWarehouseColors.surfaceContainerLow.copy(alpha = 0.35f))
                    .clickable { onRowClick(t) }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(t.type, modifier = Modifier.weight(1.6f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MexaWarehouseColors.textPrimary)
                Text(t.channel, modifier = Modifier.weight(0.8f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Text(t.subject.ifBlank { "—" }, modifier = Modifier.weight(1.8f), fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                Box(Modifier.weight(0.8f)) {
                    val (bg, fg, label) = if (t.active)
                        Triple(MexaWarehouseColors.statusActiveBg, MexaWarehouseColors.statusActiveFg, "FAOL")
                    else
                        Triple(MexaWarehouseColors.statusBlockedBg, MexaWarehouseColors.statusBlockedFg, "NOFAOL")
                    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MexaWarehouseColors.borderSubtle.copy(alpha = 0.6f)))
        }
    }
}

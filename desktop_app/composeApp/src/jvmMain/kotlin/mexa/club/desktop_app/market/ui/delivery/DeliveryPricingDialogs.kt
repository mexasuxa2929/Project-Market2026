package mexa.club.desktop_app.market.ui.delivery
import mexa.club.desktop_app.localization.AppStrings
import mexa.club.desktop_app.localization.appLanguage


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

/** Diamchi: bir xil editor dialogi — barcha pricing shakllari uchun. */
sealed class PricingField {
    abstract val label: String
    abstract val key: String
    open val hint: String? get() = null

    data class Text(
        override val label: String,
        override val key: String,
        val value: String,
        val required: Boolean = false,
        override val hint: String? = null,
        val skipWhenBlank: Boolean = false,
    ) : PricingField()

    data class Num(
        override val label: String,
        override val key: String,
        val value: String,
        val required: Boolean = false,
        override val hint: String? = null,
    ) : PricingField()

    data class Bool(
        override val label: String,
        override val key: String,
        val value: Boolean,
    ) : PricingField()

    /** options — (ko'rinadigan matn, yuboriladigan qiymat) juftliklari. */
    data class Sel(
        override val label: String,
        override val key: String,
        val options: List<Pair<String, String>>,
        val value: String,
        val allowEmpty: Boolean = true,
        override val hint: String? = null,
    ) : PricingField()
}

private fun esc(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\n", "\\n")

/** Fieldlar → JSON body (bo'sh ixtiyoriy maydonlar null sifatida yuboriladi — PUT merge'da eski qiymat tozalanadi). */
fun buildJson(fields: List<PricingField>): String =
    fields.mapNotNull { f ->
        val key = "\"${f.key}\""
        when (f) {
            is PricingField.Text ->
                if (f.skipWhenBlank && f.value.isBlank()) "$key:null"
                else "$key:\"${esc(f.value.trim())}\""
            is PricingField.Num -> {
                val raw = f.value.trim()
                if (raw.isEmpty()) "$key:null"
                else {
                    val n = raw.toBigDecimalOrNull()
                    if (n != null) "$key:$n" else "$key:\"${esc(raw)}\""
                }
            }
            is PricingField.Bool -> "$key:${f.value}"
            is PricingField.Sel ->
                if (f.value.isBlank()) "$key:null" else "$key:\"${esc(f.value)}\""
        }
    }.joinToString(",", "{", "}")

@Composable
fun PricingEditorDialog(
    title: String,
    subtitle: String,
    initialFields: List<PricingField>,
    saveRequest: suspend (String) -> String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var fields by remember(initialFields) { mutableStateOf(initialFields) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun update(index: Int, field: PricingField) {
        fields = fields.toMutableList().also { it[index] = field }
    }

    fun save() {
        val missing = fields.filter {
            when (it) {
                is PricingField.Text -> it.required && it.value.isBlank()
                is PricingField.Num -> it.required && it.value.trim().isEmpty()
                else -> false
            }
        }
        if (missing.isNotEmpty()) {
            error = "Majburiy maydonlar to'ldirilmagan: " + missing.joinToString { it.label }
            return
        }
        scope.launch {
            saving = true
            error = null
            val body = buildJson(fields)
            val result = runCatching { saveRequest(body) }
            result.onSuccess { onSaved() }
                .onFailure { e -> error = e.message ?: "Xatolik yuz berdi" }
            saving = false
        }
    }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 460.dp, max = 620.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                        Text(subtitle, fontSize = 12.sp, color = MexaWarehouseColors.textMuted)
                    }
                    IconButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Yopish", tint = MexaWarehouseColors.textMuted)
                    }
                }

                fields.forEachIndexed { index, field ->
                    when (field) {
                        is PricingField.Bool -> {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(field.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                Switch(checked = field.value, onCheckedChange = { update(index, field.copy(value = it)) })
                            }
                        }
                        is PricingField.Sel -> PricingSelectField(
                            field = field,
                            onChange = { update(index, field.copy(value = it)) },
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text(field.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                                val isRequired = when (field) {
                                    is PricingField.Text -> field.required
                                    is PricingField.Num -> field.required
                                    else -> false
                                }
                                if (isRequired) Text(" *", fontSize = 13.sp, color = MexaWarehouseColors.danger)
                            }
                            field.hint?.let { Text(it, fontSize = 11.sp, color = MexaWarehouseColors.textCaption) }
                            OutlinedTextField(
                                value = when (field) {
                                    is PricingField.Text -> field.value
                                    is PricingField.Num -> field.value
                                    else -> ""
                                },
                                onValueChange = { v ->
                                    update(index, when (field) {
                                        is PricingField.Text -> field.copy(value = v)
                                        is PricingField.Num -> field.copy(value = v)
                                        else -> field
                                    })
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = { if (!saving) onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text(AppStrings.cancel(appLanguage())) }
                    Button(
                        onClick = { save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.indigoAccent),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(AppStrings.settingsSave(appLanguage()), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PricingSelectField(field: PricingField.Sel, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(field.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
        field.hint?.let { Text(it, fontSize = 11.sp, color = MexaWarehouseColors.textCaption) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = field.options.firstOrNull { it.second == field.value }?.first
                    ?: field.value.ifEmpty { "— tanlanmagan —" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (field.allowEmpty) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.t(appLanguage(), "— tanlanmagan —")) },
                        onClick = { onChange(""); expanded = false },
                    )
                }
                field.options.forEach { (display, value) ->
                    DropdownMenuItem(text = { Text(display) }, onClick = { onChange(value); expanded = false })
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    requireReason: Boolean = true,
    onConfirm: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MexaWarehouseColors.surfaceLowest,
            modifier = Modifier.widthIn(min = 400.dp, max = 520.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MexaWarehouseColors.textPrimary)
                Text(message, fontSize = 13.sp, color = MexaWarehouseColors.textMuted)
                if (requireReason) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sabab *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MexaWarehouseColors.textPrimary)
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it; error = null },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("Nima uchun o'chirilmoqda?", fontSize = 13.sp) },
                        )
                    }
                }
                error?.let { Text(it, color = MexaWarehouseColors.danger, fontSize = 13.sp) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = { if (!busy) onDismiss() }, shape = RoundedCornerShape(8.dp)) { Text(AppStrings.cancel(appLanguage())) }
                    Button(
                        onClick = {
                            if (requireReason && reason.isBlank()) {
                                error = "Sabab majburiy"
                                return@Button
                            }
                            scope.launch {
                                busy = true
                                error = null
                                val result = runCatching { onConfirm(reason) }
                                result.onFailure { e -> error = e.message ?: "Xatolik" }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MexaWarehouseColors.danger),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(AppStrings.deleteUserSubmit(appLanguage()), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Kichik JSON parse yordamchisi (editor uchun). */
object ApiClientJson {
    fun parse(text: String): kotlinx.serialization.json.JsonObject? =
        runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
        }.getOrNull()

    fun str(obj: kotlinx.serialization.json.JsonObject?, key: String): String {
        val el = obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive ?: return ""
        return el.contentOrNull ?: ""
    }

    fun bool(obj: kotlinx.serialization.json.JsonObject?, key: String, def: Boolean): Boolean {
        val el = obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive ?: return def
        return el.booleanOrNull ?: el.contentOrNull?.toBooleanStrictOrNull() ?: def
    }
}

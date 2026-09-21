package mexa.club.desktop_app.market.ui.products

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mexa.club.desktop_app.market.ui.theme.MexaWarehouseColors

private data class PresetColor(val hex: String, val name: String)

private val PRESET_COLORS = listOf(
    PresetColor("#FFFFFF", "Oq"),
    PresetColor("#F5F5F5", "Kumush oq"),
    PresetColor("#D3D3D3", "Kulrang och"),
    PresetColor("#808080", "Kulrang"),
    PresetColor("#404040", "To'q kulrang"),
    PresetColor("#000000", "Qora"),
    PresetColor("#FFD700", "Oltin"),
    PresetColor("#FFA500", "To'q sariq"),
    PresetColor("#FF6347", "Tarvuz"),
    PresetColor("#FF0000", "Qizil"),
    PresetColor("#DC143C", "Krем qizil"),
    PresetColor("#8B0000", "To'q qizil"),
    PresetColor("#FFC0CB", "Pushti"),
    PresetColor("#FF69B4", "Qo'ng'ir pushti"),
    PresetColor("#9400D3", "Binafsha"),
    PresetColor("#4B0082", "To'q binafsha"),
    PresetColor("#0000FF", "Ko'k"),
    PresetColor("#1E90FF", "Moviy"),
    PresetColor("#00BFFF", "Och moviy"),
    PresetColor("#00CED1", "Feruza"),
    PresetColor("#008080", "Zangori"),
    PresetColor("#00FF7F", "Yashil och"),
    PresetColor("#008000", "Yashil"),
    PresetColor("#006400", "To'q yashil"),
    PresetColor("#ADFF2F", "Limon yashil"),
    PresetColor("#FFFF00", "Sariq"),
    PresetColor("#F5DEB3", "Bug'doy"),
    PresetColor("#D2691E", "Shokolad"),
    PresetColor("#8B4513", "Jigarrang"),
    PresetColor("#A0522D", "Sienna"),
)

private fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val value = clean.toLong(16)
        if (clean.length == 6) {
            Color(
                red = ((value shr 16) and 0xFF).toInt() / 255f,
                green = ((value shr 8) and 0xFF).toInt() / 255f,
                blue = (value and 0xFF).toInt() / 255f,
            )
        } else {
            Color(
                red = ((value shr 16) and 0xFF).toInt() / 255f,
                green = ((value shr 8) and 0xFF).toInt() / 255f,
                blue = (value and 0xFF).toInt() / 255f,
                alpha = ((value shr 24) and 0xFF).toInt() / 255f,
            )
        }
    } catch (_: NumberFormatException) { null }
}

private fun hexToDisplayName(hex: String): String {
    val normalized = if (hex.startsWith("#")) hex.uppercase() else "#${hex.uppercase()}"
    return PRESET_COLORS.find { it.hex.uppercase() == normalized }?.name ?: normalized
}

private fun contrastColor(bg: Color): Color {
    val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (luminance > 0.5f) Color(0xFF1A1A1A) else Color.White
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerSection(
    colors: List<String>,
    onColorsChange: (List<String>) -> Unit,
) {
    var hexInput by remember { mutableStateOf("") }
    var hexError by remember { mutableStateOf(false) }

    fun addCustomColor() {
        val raw = hexInput.trim()
        if (raw.isEmpty()) return
        val hex = if (raw.startsWith("#")) raw else "#$raw"
        if (parseHexColor(hex) == null) { hexError = true; return }
        val normalized = hex.uppercase()
        if (normalized !in colors.map { it.uppercase() }) {
            onColorsChange(colors + normalized)
        }
        hexInput = ""
        hexError = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── Preset palette ────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MexaWarehouseColors.outlineVariant, RoundedCornerShape(8.dp))
                .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PRESET_COLORS.forEach { preset ->
                    val selected = colors.any { it.uppercase() == preset.hex.uppercase() }
                    val bgColor = parseHexColor(preset.hex) ?: Color.Gray
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MexaWarehouseColors.primary
                                        else Color(0xFFCCCCCC),
                                shape = CircleShape,
                            )
                            .clickable {
                                val n = preset.hex.uppercase()
                                if (selected) onColorsChange(colors.filter { it.uppercase() != n })
                                else onColorsChange(colors + n)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = contrastColor(bgColor),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Custom hex input ──────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Color preview swatch
            val previewColor = parseHexColor(
                if (hexInput.trim().startsWith("#")) hexInput.trim()
                else "#${hexInput.trim()}"
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(previewColor ?: Color(0xFFEEEEEE))
                    .border(
                        1.dp,
                        if (hexError) Color(0xFFE53935) else MexaWarehouseColors.outlineVariant,
                        RoundedCornerShape(6.dp),
                    ),
            )

            // Hex text input
            Box(
                Modifier
                    .weight(1f)
                    .border(
                        1.dp,
                        if (hexError) Color(0xFFE53935) else MexaWarehouseColors.outlineVariant,
                        RoundedCornerShape(8.dp),
                    )
                    .background(MexaWarehouseColors.backgroundPage, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                BasicTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it; hexError = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                addCustomColor(); true
                            } else false
                        },
                    textStyle = TextStyle(fontSize = 13.sp, color = MexaWarehouseColors.textPrimary),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (hexInput.isEmpty()) {
                            Text(
                                "HEX kod kiriting... masalan: #FF5733",
                                fontSize = 13.sp,
                                color = MexaWarehouseColors.textMuted,
                            )
                        }
                        inner()
                    },
                )
            }

            // Add button
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MexaWarehouseColors.primary)
                    .clickable { addCustomColor() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Qo'shish",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (hexError) {
            Text(
                "Noto'g'ri HEX format. Masalan: #FF5733 yoki FF5733",
                fontSize = 11.sp,
                color = Color(0xFFE53935),
            )
        }

        // ── Selected colors chips ─────────────────────────────────────────
        if (colors.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                colors.forEach { colorHex ->
                    val bgColor = parseHexColor(colorHex) ?: Color.Gray
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bgColor)
                            .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp))
                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            hexToDisplayName(colorHex),
                            fontSize = 12.sp,
                            color = contrastColor(bgColor),
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(14.dp)
                                .clickable { onColorsChange(colors - colorHex) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "O'chirish",
                                tint = contrastColor(bgColor),
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
